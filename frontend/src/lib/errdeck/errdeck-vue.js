// src/envelope.ts
var e = new TextEncoder();
function t(t, n, r = /* @__PURE__ */ new Date(), i = []) {
	let a = [JSON.stringify({
		sdk: "vue/0.1.0",
		sent_at: r.toISOString()
	})], o = (t, n) => {
		a.push(JSON.stringify({
			type: t,
			length: e.encode(n).length
		}), n);
	};
	for (let e of t) o("event", JSON.stringify(e));
	for (let e of i) o("transaction", JSON.stringify(e));
	return n.length && o("client_report", JSON.stringify({
		timestamp: r.toISOString(),
		discarded: n
	})), e.encode(a.join("\n") + "\n");
}
async function n(e) {
	if (typeof CompressionStream > "u") return null;
	let t = new Blob([e]).stream().pipeThrough(new CompressionStream("gzip"));
	return new Uint8Array(await new Response(t).arrayBuffer());
}
// src/fold.ts
var r = class {
	constructor(e, t, n = 200) {
		this.windowMs = e, this.threshold = t, this.maxSignatures = n, this.windows = /* @__PURE__ */ new Map(), this.retired = [];
	}
	admit(e, t) {
		let n = i(e), r = this.windows.get(n);
		if (!r || t - r.start >= this.windowMs) {
			if (!r && this.windows.size >= this.maxSignatures) return !0;
			r && r.folded > 0 && this.retired.push(r), r = {
				start: t,
				passed: 0,
				folded: 0
			}, this.windows.set(n, r);
		}
		return r.passed < this.threshold ? (r.passed++, !0) : (r.folded === 0 && (r.sample = e), r.folded++, !1);
	}
	drain(e) {
		let t = [], n = (e) => {
			e.folded > 0 && e.sample && t.push({
				...e.sample,
				event_id: a(),
				count: e.folded
			});
		};
		for (let [t, r] of this.windows) e - r.start >= this.windowMs && (n(r), this.windows.delete(t));
		return this.retired.forEach(n), this.retired = [], t;
	}
};
function i(e) {
	let t = e.exceptions[0];
	return t ? `${t.type}|${t.frames.slice(0, 10).map((e) => `${e.filename}:${e.lineno}:${e.colno}`).join(",")}` : `m:${e.message ?? ""}`;
}
function a() {
	return typeof crypto < "u" && "randomUUID" in crypto ? crypto.randomUUID() : "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (e) => {
		let t = Math.random() * 16 | 0;
		return (e === "x" ? t : t & 3 | 8).toString(16);
	});
}
// src/stack.ts
var o = /^\s*at (?:(?:async )?(.*?) \()?(.*?):(\d+):(\d+)\)?\s*$/, s = /^\s*(.*?)@(.*?):(\d+):(\d+)\s*$/;
function c(e, t = 50) {
	if (!e) return [];
	let n = [];
	for (let r of e.split("\n")) {
		let e = o.exec(r) ?? s.exec(r);
		if (!e || !e[2] || e[2] === "native") continue;
		let i = {
			filename: e[2],
			lineno: Number(e[3]),
			colno: Number(e[4])
		}, a = e[1]?.trim();
		if (a && (i.function = a), n.push(i), n.length >= t) break;
	}
	return n;
}
// src/tracing.ts
var l = 500, u = 500, d = 15e3;
function f(e) {
	return a().replace(/-/g, "").slice(0, e);
}
var p = class {
	constructor(e, t = () => Date.now()) {
		this.options = e, this.now = t, this.active = null, this.done = [];
	}
	traceId() {
		return this.active?.traceId;
	}
	start(e, t) {
		this.end();
		let n = this.options.tracesSampleRate ?? .1;
		this.active = {
			traceId: f(32),
			spanId: f(16),
			sampled: Math.random() < n,
			name: this.clean(e),
			op: t,
			start: this.now(),
			spans: [],
			pending: 0,
			error: !1
		}, this.schedule();
	}
	rename(e) {
		this.active && e && (this.active.name = this.clean(e));
	}
	markError() {
		this.active && (this.active.error = !0);
	}
	startSpan(e, t, n) {
		let r = this.active;
		if (!r) return { finish: () => {} };
		r.pending++;
		let i = f(16), a = this.now();
		return {
			header: this.propagates(n) ? `00-${r.traceId}-${i}-${r.sampled ? "01" : "00"}` : void 0,
			finish: (n) => {
				r.pending--, (n === void 0 || n >= 500) && (r.error = !0), this.add(r, {
					span_id: i,
					parent_span_id: r.spanId,
					op: e,
					description: this.clean(t),
					start_offset_ms: a - r.start,
					duration_ms: this.now() - a
				}), r === this.active && this.schedule();
			}
		};
	}
	take() {
		let e = this.done;
		return this.done = [], e;
	}
	end() {
		let e = this.active;
		if (!e) return;
		clearTimeout(e.timer), this.active = null;
		let t = this.now() - e.start, n = t >= (this.options.slowThresholdMs ?? 1e3);
		if (!e.sampled && !e.error && !n) return;
		let r = {
			trace_id: e.traceId,
			span_id: e.spanId,
			name: e.name,
			op: e.op,
			start_timestamp: new Date(e.start).toISOString(),
			duration_ms: t,
			status: e.error ? "error" : "ok",
			spans: e.spans
		};
		e.sampled || (r.partial = !0), this.options.environment && (r.environment = this.options.environment), this.options.release && (r.release = this.options.release), this.done.length < 100 && this.done.push(r);
	}
	add(e, t) {
		let n = e.spans[e.spans.length - 1];
		n && n.op === t.op && n.description === t.description ? (n.repeat = (n.repeat ?? 1) + 1, n.duration_ms += t.duration_ms) : e.spans.length < l && e.spans.push(t);
	}
	schedule() {
		let e = this.active;
		if (!e) return;
		clearTimeout(e.timer);
		let t = d - (this.now() - e.start);
		e.timer = setTimeout(() => e.pending > 0 && t > u ? this.schedule() : this.end(), Math.max(0, Math.min(u, t)));
	}
	clean(e) {
		try {
			return this.options.redact ? this.options.redact(e) : e;
		} catch {
			return "[Filtered]";
		}
	}
	propagates(e) {
		let t = this.options.tracePropagationTargets;
		return t ? t.some((t) => typeof t == "string" ? e.startsWith(t) : t.test(e)) : typeof location > "u" || e.startsWith("/") || e.startsWith(location.origin);
	}
}, m = class {
	constructor(e, t, n, i = () => Date.now()) {
		this.url = e, this.options = t, this.transport = n, this.now = i, this.queue = [], this.crumbs = [], this.discarded = /* @__PURE__ */ new Map(), this.pausedUntil = 0, this.pauseReason = "rate_limited", this.disabled = !1, this.sending = !1, this.seen = /* @__PURE__ */ new WeakSet(), this.redact = (e) => {
			if (!this.options.redact) return e;
			try {
				return this.options.redact(e);
			} catch {
				return "[Filtered]";
			}
		}, this.folder = new r(t.foldWindowMs ?? 1e4, t.foldThreshold ?? 5), this.queueSize = t.queueSize ?? 100, this.maxBreadcrumbs = t.maxBreadcrumbs ?? 50, this.tracer = t.tracing === !1 ? null : new p(t, i);
	}
	capture(e, t) {
		try {
			if (e && typeof e == "object") {
				if (this.seen.has(e)) return;
				this.seen.add(e);
			}
			let n = this.event(e, t);
			if (this.tracer?.markError(), !this.folder.admit(n, this.now())) return;
			this.enqueue(n);
		} catch {}
	}
	addBreadcrumb(e) {
		let t = this.redact, n = e.data ? Object.fromEntries(Object.entries(e.data).map(([e, n]) => [e, t(n)])) : void 0;
		this.crumbs.push({
			timestamp: new Date(this.now()).toISOString(),
			...e,
			...e.message ? { message: t(e.message) } : {},
			...n ? { data: n } : {}
		}), this.crumbs.length > this.maxBreadcrumbs && this.crumbs.shift();
	}
	async flush() {
		if (this.sending) return;
		await this.flushTransactions();
		let e = this.take();
		if (e) {
			this.sending = !0;
			try {
				let r = t(e.batch, e.discarded, new Date(this.now())), i = await n(r).catch(() => null), a = await this.transport.send(i ? `${this.url}&enc=gzip` : this.url, i ?? r);
				this.handle(a.status, a.retryAfter, e);
			} catch {
				this.count("network_error", h(e.batch)), this.restore(e.discarded);
			} finally {
				this.sending = !1;
			}
		}
	}
	flushBeacon() {
		try {
			this.tracer?.end();
			let e = this.tracer?.take() ?? [];
			e.length && this.now() >= this.pausedUntil && this.transport.beacon(`${this.url}&kind=tx`, t([], [], new Date(this.now()), e));
			let n = this.take();
			if (!n) return;
			this.transport.beacon(this.url, t(n.batch, n.discarded, new Date(this.now()))) || (this.count("network_error", h(n.batch)), this.restore(n.discarded));
		} catch {}
	}
	async flushTransactions() {
		let e = this.tracer?.take() ?? [];
		if (!(!e.length || this.disabled || this.now() < this.pausedUntil)) try {
			let r = t([], [], new Date(this.now()), e), i = await n(r).catch(() => null);
			await this.transport.send(i ? `${this.url}&kind=tx&enc=gzip` : `${this.url}&kind=tx`, i ?? r);
		} catch {}
	}
	pending() {
		return this.queue.length;
	}
	take() {
		if (this.queue.push(...this.folder.drain(this.now())), this.disabled) return this.count("unauthorized", h(this.queue)), this.queue = [], null;
		if (this.now() < this.pausedUntil) return this.count(this.pauseReason, h(this.queue)), this.queue = [], null;
		let e = [...this.discarded].map(([e, t]) => ({
			reason: e,
			category: "event",
			quantity: t
		}));
		if (this.discarded.clear(), !this.queue.length && !e.length) return null;
		let t = this.queue;
		return this.queue = [], {
			batch: t,
			discarded: e
		};
	}
	handle(e, t, n) {
		if (e >= 200 && e < 300) return;
		this.restore(n.discarded);
		let r = h(n.batch);
		e === 429 || e === 503 ? (this.pauseReason = e === 429 ? "rate_limited" : "queue_full", this.pausedUntil = this.now() + (t ?? 60) * 1e3, this.count(this.pauseReason, r)) : e === 401 || e === 403 ? (this.disabled = !0, this.count("unauthorized", r), console.warn(`ErrDeck: ingest 가 ${e} 로 거절해 전송을 멈춘다. DSN 과 허용 출처를 확인한다`)) : this.count(e === 413 ? "too_large" : "network_error", r);
	}
	enqueue(e) {
		this.queue.length >= this.queueSize ? this.count("queue_overflow", e.count ?? 1) : this.queue.push(e);
	}
	count(e, t) {
		t > 0 && this.discarded.set(e, (this.discarded.get(e) ?? 0) + t);
	}
	restore(e) {
		for (let t of e) this.count(t.reason, t.quantity);
	}
	event(e, t) {
		let n = e instanceof Error ? e : null, r = {
			event_id: a(),
			timestamp: new Date(this.now()).toISOString(),
			level: "error",
			exceptions: [{
				type: n?.name || (typeof e == "object" && e ? "Error" : "UnhandledValue"),
				value: this.redact(g(n ? n.message : _(e))),
				frames: c(n?.stack)
			}],
			breadcrumbs: this.crumbs.slice()
		}, i = this.tracer?.traceId();
		return i && (r.trace_id = i), this.options.environment && (r.environment = this.options.environment), this.options.release && (r.release = this.options.release), t && Object.keys(t).length && (r.tags = t), typeof location < "u" && (r.request = {
			method: "GET",
			url: this.redact(location.origin + location.pathname),
			headers: typeof navigator < "u" ? { "User-Agent": navigator.userAgent } : {}
		}), r;
	}
};
function h(e) {
	return e.reduce((e, t) => e + (t.count ?? 1), 0);
}
function g(e) {
	return e.length > 8192 ? e.slice(0, 8192) : e;
}
function _(e) {
	try {
		return typeof e == "string" ? e : JSON.stringify(e) ?? String(e);
	} catch {
		return String(e);
	}
}
// src/dsn.ts
function v(e) {
	let t = /^(https?):\/\/([A-Za-z0-9_-]+)@([^/?#]+)\/([1-9][0-9]{0,9})$/.exec(e.trim());
	if (!t) throw Error("ErrDeck: DSN 형식이 틀렸다");
	let [, n, r, i, a] = t;
	return `${n}://${i}/ingest/${a}/envelope?key=${r}`;
}
// src/transport.ts
var y = {
	async send(e, t) {
		let n = await fetch(e, {
			method: "POST",
			body: t,
			headers: { "Content-Type": "text/plain" },
			keepalive: t.length < 6e4,
			credentials: "omit",
			mode: "cors"
		}), r = Number(n.headers.get("Retry-After"));
		return {
			status: n.status,
			retryAfter: Number.isFinite(r) && r > 0 ? r : void 0
		};
	},
	beacon(e, t) {
		return typeof navigator < "u" && typeof navigator.sendBeacon == "function" && navigator.sendBeacon(e, new Blob([t], { type: "text/plain" }));
	}
}, b = null;
function x(e, t) {
	b?.capture(e, t);
}
function S(e) {
	return { install(t) {
		if (!e.dsn || typeof window > "u") return;
		let n;
		try {
			n = v(e.dsn);
		} catch (e) {
			console.warn(String(e));
			return;
		}
		let r = new m(n, e, y);
		b = r;
		let i = t.config.errorHandler;
		if (t.config.errorHandler = (e, t, n) => {
			let a = t?.$options?.name ?? (t?.$)?.type?.__name;
			r.capture(e, {
				...a ? { component: a } : {},
				...n ? { lifecycle: n } : {}
			}), i ? i(e, t, n) : console.error(e);
		}, window.addEventListener("error", (e) => r.capture(e.error ?? e.message)), window.addEventListener("unhandledrejection", (e) => r.capture(e.reason)), e.breadcrumbs !== !1 && C(r, e), w(r, n, e), T(r, n, e), r.tracer) {
			let t = r.tracer;
			t.start(location.pathname, "pageload"), e.router?.beforeEach?.((e) => t.start(e.fullPath.split("?")[0] ?? e.fullPath, "navigation")), e.router?.afterEach((e) => t.rename(e.matched?.[e.matched.length - 1]?.path ?? ""));
		}
		let a = setInterval(() => void r.flush(), e.flushIntervalMs ?? 2e3);
		document.addEventListener("visibilitychange", () => {
			document.visibilityState === "hidden" && r.flushBeacon();
		}), window.addEventListener("pagehide", () => r.flushBeacon());
		let o = t.unmount.bind(t);
		t.unmount = () => {
			clearInterval(a), r.flushBeacon(), o();
		};
	} };
}
function C(e, t) {
	let n = (e) => e.split("?")[0] ?? e;
	t.router?.afterEach((t, r) => e.addBreadcrumb({
		category: "navigation",
		data: {
			from: n(r.fullPath),
			to: n(t.fullPath)
		}
	})), document.addEventListener("click", (t) => {
		let n = t.target instanceof Element ? t.target : null;
		if (!n) return;
		let r = (n.textContent ?? "").trim().slice(0, 40), i = n.id ? `#${n.id}` : "", a = typeof n.className == "string" && n.className ? `.${n.className.trim().split(/\s+/).join(".")}` : "";
		e.addBreadcrumb({
			category: "ui.click",
			message: `${n.tagName.toLowerCase()}${i}${a}${r ? ` "${r}"` : ""}`
		});
	}, !0);
}
function w(e, t, n) {
	let r = n.breadcrumbs !== !1, i = window.fetch.bind(window);
	window.fetch = async (n, a) => {
		let o = typeof n == "string" ? n : n instanceof URL ? n.href : n.url;
		if (o.startsWith(t.split("?")[0] ?? t)) return i(n, a);
		let s = a?.method ?? (n instanceof Request ? n.method : "GET"), c = o.split("?")[0], l = e.tracer?.startSpan("http.client", `${s} ${c}`, o);
		if (l?.header) {
			let e = new Headers(a?.headers ?? (n instanceof Request ? n.headers : void 0));
			e.set("traceparent", l.header), a = {
				...a,
				headers: e
			};
		}
		try {
			let t = await i(n, a);
			return l?.finish(t.status), r && e.addBreadcrumb({
				category: "http",
				message: `${s} ${c}`,
				data: { status_code: String(t.status) }
			}), t;
		} catch (t) {
			throw l?.finish(), r && e.addBreadcrumb({
				category: "http",
				message: `${s} ${c}`,
				data: { error: String(t) }
			}), t;
		}
	};
}
function T(e, t, n) {
	if (typeof XMLHttpRequest > "u") return;
	let r = n.breadcrumbs !== !1, i = t.split("?")[0] ?? t, a = XMLHttpRequest.prototype, o = a.open, s = a.send, c = /* @__PURE__ */ new WeakMap();
	a.open = function(e, t, ...n) {
		return c.set(this, {
			method: String(e).toUpperCase(),
			url: String(t)
		}), o.call(this, e, t, ...n);
	}, a.send = function(t) {
		let n = c.get(this);
		if (!n || n.url.startsWith(i)) return s.call(this, t);
		let a = n.url.split("?")[0], o = e.tracer?.startSpan("http.client", `${n.method} ${a}`, n.url);
		if (o?.header) try {
			this.setRequestHeader("traceparent", o.header);
		} catch {}
		return this.addEventListener("loadend", () => {
			let t = this.status;
			o?.finish(t === 0 ? void 0 : t), r && e.addBreadcrumb({
				category: "http",
				message: `${n.method} ${a}`,
				data: t === 0 ? { error: "network" } : { status_code: String(t) }
			});
		}), s.call(this, t);
	};
}
export { m as Client, x as captureException, S as createErrDeck };
