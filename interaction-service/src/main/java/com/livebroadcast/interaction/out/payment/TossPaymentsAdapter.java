package com.livebroadcast.interaction.out.payment;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.error.ErrorType;
import com.livebroadcast.interaction.app.PaymentProperties;
import com.livebroadcast.interaction.app.port.out.PaymentGatewayPort;
import com.livebroadcast.interaction.domain.donation.PaymentApproval;
import com.livebroadcast.interaction.domain.donation.PaymentOutcomeUnknownException;
import com.livebroadcast.interaction.domain.donation.PaymentRejectedException;
import com.livebroadcast.interaction.domain.donation.PaymentTransaction;

/**
 * 토스페이먼츠 결제 승인 API 어댑터.
 * 문서: https://docs.tosspayments.com/reference#결제-승인
 */
@Component
public class TossPaymentsAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentsAdapter.class);
    private static final String ALREADY_PROCESSED = "ALREADY_PROCESSED_PAYMENT";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RestClient restClient;

    public TossPaymentsAdapter(PaymentProperties properties) {
        String secret = properties.secretKey() == null ? "" : properties.secretKey();
        String basic = Base64.getEncoder().encodeToString((secret + ":").getBytes(StandardCharsets.UTF_8));
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        // 결제 승인은 결제사 권장대로 넉넉히 기다린다. 이 시간을 넘기면 UNKNOWN 으로 두고 조회로 확정한다.
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder()
                .baseUrl(properties.apiUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .build();
    }

    @Override
    public PaymentApproval confirm(String paymentKey, String orderId, long amount) {
        PaymentApproval approval;
        try {
            approval = restClient.post()
                    .uri("/v1/payments/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    // 같은 주문으로 승인 요청이 재전송되어도 토스 쪽에서 한 번만 처리되도록 멱등키를 보낸다.
                    .header("Idempotency-Key", orderId)
                    .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return toApproval(response.bodyTo(TossPayment.class));
                        }
                        TossError error = response.bodyTo(TossError.class);
                        String code = error == null ? null : error.code();
                        if (ALREADY_PROCESSED.equals(code)) {
                            // 이미 승인된 결제면 결제 정보를 다시 조회해서 검증한다.
                            return null;
                        }
                        if (response.getStatusCode().is4xxClientError()) {
                            throw new PaymentRejectedException(code, error == null ? null : error.message());
                        }
                        // 5xx: 결제사 내부에서 승인이 됐는지 알 수 없다.
                        throw new PaymentOutcomeUnknownException("결제사 응답 오류 " + response.getStatusCode().value());
                    });
        } catch (RestClientException e) {
            // 연결 실패, 응답 시간 초과. 요청이 결제사에 도착해 승인됐을 수도 있으므로 실패로 단정하지 않는다.
            log.warn("토스페이먼츠 승인 결과 확인 불가 orderId={} reason={}", orderId, e.getMessage());
            throw new PaymentOutcomeUnknownException(null);
        }
        // ALREADY_PROCESSED_PAYMENT 인 경우: 결제 정보를 다시 조회해서 검증한다(승인 결과는 도메인에서 확인).
        return approval != null ? approval : fetch(paymentKey);
    }

    @Override
    public PaymentApproval fetch(String paymentKey) {
        try {
            TossPayment payment = restClient.get()
                    .uri("/v1/payments/{paymentKey}", paymentKey)
                    .retrieve()
                    .body(TossPayment.class);
            return toApproval(payment);
        } catch (RestClientException e) {
            log.warn("토스페이먼츠 결제 조회 실패 reason={}", e.getMessage());
            throw gatewayError();
        }
    }

    /**
     * 거래 내역 조회(대사용). 문서: https://docs.tosspayments.com/reference#거래-조회
     * startingAfter 커서로 페이지를 넘기며 최대 1만 건까지 읽는다.
     */
    @Override
    public List<PaymentTransaction> listTransactions(Instant from, Instant to) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(KST);
        List<PaymentTransaction> result = new ArrayList<>();
        String startingAfter = null;
        try {
            for (int page = 0; page < 100; page++) {
                String after = startingAfter;
                List<TossTransaction> chunk = restClient.get()
                        .uri(b -> {
                            b.path("/v1/transactions")
                                    .queryParam("startDate", format.format(from))
                                    .queryParam("endDate", format.format(to.minusSeconds(1)))
                                    .queryParam("limit", 100);
                            if (after != null) {
                                b.queryParam("startingAfter", after);
                            }
                            return b.build();
                        })
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<TossTransaction>>() {
                        });
                if (chunk == null || chunk.isEmpty()) {
                    break;
                }
                chunk.forEach(t -> result.add(new PaymentTransaction(t.paymentKey(), t.orderId(), t.status(),
                        t.amount())));
                if (chunk.size() < 100) {
                    break;
                }
                startingAfter = chunk.get(chunk.size() - 1).transactionKey();
            }
        } catch (RestClientException e) {
            log.warn("토스페이먼츠 거래 내역 조회 실패 reason={}", e.getMessage());
            throw gatewayError();
        }
        return result;
    }

    private PaymentApproval toApproval(TossPayment p) {
        if (p == null) {
            throw gatewayError();
        }
        Instant approvedAt = p.approvedAt() == null ? null : OffsetDateTime.parse(p.approvedAt()).toInstant();
        return new PaymentApproval(p.paymentKey(), p.orderId(), p.status(), p.totalAmount(), approvedAt);
    }

    private static DomainException gatewayError() {
        return new DomainException(ErrorType.BAD_GATEWAY, "PAYMENT_GATEWAY_ERROR",
                "결제사와 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TossPayment(String paymentKey, String orderId, String status, long totalAmount, String approvedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TossTransaction(String transactionKey, String paymentKey, String orderId, String status, long amount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TossError(String code, String message) {
    }
}
