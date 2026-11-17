package com.livebroadcast.interaction.domain.donation;

import java.time.Instant;
import java.util.Objects;

/**
 * 후원 애그리거트.
 *
 * 흐름: PENDING(주문 생성) -> 결제창 -> 서버 승인 요청 -> 결제사 응답 검증 -> DONE(적립)
 * 같은 승인 통지(브라우저 리다이렉트 재시도, 웹훅 재전송)가 여러 번 와도 approve() 는 최초 1회만 true 를 돌려준다.
 */
public class Donation {

    public static final int MAX_MESSAGE = 200;

    private final Long id;
    private final String orderId;
    private final Long broadcastId;
    private final Long streamerId;
    private final String streamerNickname;
    private final Long donorId;
    private final String donorNickname;
    private final long amount;
    private final String message;
    private DonationStatus status;
    private String paymentKey;
    private Instant approvedAt;
    private String failureReason;
    private final Instant createdAt;
    private Instant updatedAt;
    private final DonationTargetType targetType;
    /** 응원 대상(구단/선수). 응원금이면 필수, 방송 후원이면 편파방일 때만 있다. */
    private final Long targetId;
    private final String targetName;

    public Donation(Long id, String orderId, Long broadcastId, Long streamerId, String streamerNickname,
                    Long donorId, String donorNickname, long amount, String message, DonationStatus status,
                    String paymentKey, Instant approvedAt, String failureReason, Instant createdAt, Instant updatedAt) {
        this(id, orderId, broadcastId, streamerId, streamerNickname, donorId, donorNickname, amount, message, status,
                paymentKey, approvedAt, failureReason, createdAt, updatedAt, DonationTargetType.BROADCAST, null, null);
    }

    public Donation(Long id, String orderId, Long broadcastId, Long streamerId, String streamerNickname,
                    Long donorId, String donorNickname, long amount, String message, DonationStatus status,
                    String paymentKey, Instant approvedAt, String failureReason, Instant createdAt, Instant updatedAt,
                    DonationTargetType targetType, Long targetId, String targetName) {
        this.targetType = targetType == null ? DonationTargetType.BROADCAST : targetType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.id = id;
        this.orderId = orderId;
        this.broadcastId = broadcastId;
        this.streamerId = streamerId;
        this.streamerNickname = streamerNickname;
        this.donorId = donorId;
        this.donorNickname = donorNickname;
        this.amount = amount;
        this.message = message;
        this.status = status;
        this.paymentKey = paymentKey;
        this.approvedAt = approvedAt;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Donation order(String orderId, Long broadcastId, Long streamerId, String streamerNickname,
                                 Long donorId, String donorNickname, long amount, String message,
                                 long minAmount, long maxAmount, Instant now) {
        return order(orderId, broadcastId, streamerId, streamerNickname, null, null, donorId, donorNickname, amount,
                message, minAmount, maxAmount, now);
    }

    /**
     * 방송자 후원. 방송이 편파방이면(targetId 있음) 그 응원팀의 랭킹 점수에도 들어간다.
     */
    public static Donation order(String orderId, Long broadcastId, Long streamerId, String streamerNickname,
                                 Long targetId, String targetName, Long donorId, String donorNickname, long amount,
                                 String message, long minAmount, long maxAmount, Instant now) {
        if (Objects.equals(streamerId, donorId)) {
            throw DonationErrors.selfDonation();
        }
        String safeMessage = validate(amount, message, minAmount, maxAmount);
        return new Donation(null, orderId, broadcastId, streamerId, streamerNickname, donorId, donorNickname,
                amount, safeMessage, DonationStatus.PENDING, null, null, null, now, now,
                DonationTargetType.BROADCAST, targetId, targetName);
    }

    /** 구단/선수 응원금. 방송자가 없고, 실제로 구단에 전달되지 않는 가상 기금이다. */
    public static Donation orderForTarget(String orderId, Long targetId, String targetName, Long donorId,
                                          String donorNickname, long amount, String message, long minAmount,
                                          long maxAmount, Instant now) {
        if (targetId == null) {
            throw DonationErrors.targetRequired();
        }
        String safeMessage = validate(amount, message, minAmount, maxAmount);
        return new Donation(null, orderId, null, null, null, donorId, donorNickname, amount, safeMessage,
                DonationStatus.PENDING, null, null, null, now, now, DonationTargetType.FAN_TARGET, targetId,
                targetName);
    }

    private static String validate(long amount, String message, long minAmount, long maxAmount) {
        if (amount < minAmount || amount > maxAmount) {
            throw DonationErrors.invalidAmount(minAmount, maxAmount);
        }
        String safeMessage = message == null ? "" : message.strip();
        if (safeMessage.length() > MAX_MESSAGE) {
            throw DonationErrors.invalidMessage();
        }
        return safeMessage;
    }

    public void ensureDonor(Long userId) {
        if (userId == null || !donorId.equals(userId)) {
            throw DonationErrors.notDonor();
        }
    }

    /** 결제사에 승인을 요청하기 전에, 브라우저가 보낸 금액이 주문 금액과 같은지 먼저 확인한다. */
    public void ensureRequestedAmount(long requestedAmount) {
        if (requestedAmount != amount) {
            throw DonationErrors.amountMismatch();
        }
    }

    /**
     * 결제사에 승인을 요청해도 되는 주문인지 확인한다.
     * 이미 실패/취소된 주문을 결제사에 승인 요청하면 실제 결제는 되는데 적립은 안 되므로, 호출 전에 막는다.
     */
    public void ensureConfirmable() {
        if (status.isCompleted()) {
            throw DonationErrors.differentPayment();
        }
        if (!status.acceptsConfirmRequest()) {
            throw DonationErrors.alreadyClosed();
        }
    }

    /** 이미 같은 결제로 확정된 주문이면 true (중복 승인 통지) */
    public boolean isAlreadyApprovedWith(String key) {
        return status.isCompleted() && Objects.equals(paymentKey, key);
    }

    /**
     * 결제사 승인 결과를 검증하고 후원을 확정한다.
     *
     * @return 이번 호출로 새로 확정되었으면 true, 이미 같은 결제로 확정된 상태면 false
     */
    public boolean approve(PaymentApproval approval, Instant now) {
        return approve(approval, now, false);
    }

    /**
     * 결제사가 결제 완료라고 알려온 경우(웹훅)의 확정. 결제사 기준으로 돈이 실제로 빠져나갔으므로
     * 우리 쪽에서 먼저 실패/취소 처리한 주문이어도 적립한다.
     */
    public boolean reconcile(PaymentApproval approval, Instant now) {
        return approve(approval, now, true);
    }

    private boolean approve(PaymentApproval approval, Instant now, boolean fromGateway) {
        if (status.isCompleted()) {
            if (Objects.equals(paymentKey, approval.paymentKey())) {
                return false;
            }
            throw DonationErrors.differentPayment();
        }
        boolean accepted = fromGateway ? status.acceptsGatewayApproval() : status.acceptsConfirmRequest();
        if (!accepted) {
            throw DonationErrors.alreadyClosed();
        }
        if (!orderId.equals(approval.orderId())) {
            throw DonationErrors.orderMismatch();
        }
        if (approval.totalAmount() != amount) {
            throw DonationErrors.amountMismatch();
        }
        if (!approval.isDone()) {
            throw DonationErrors.notApproved();
        }
        this.status = DonationStatus.DONE;
        this.paymentKey = approval.paymentKey();
        this.approvedAt = approval.approvedAt() == null ? now : approval.approvedAt();
        this.failureReason = null;
        this.updatedAt = now;
        return true;
    }

    public void fail(String reason, Instant now) {
        if (!status.canFail()) {
            return;
        }
        close(reason, now);
    }

    /**
     * 승인 요청을 보냈지만 결과를 모른다(타임아웃, 결제사 5xx). 결제 키를 남겨 두고 나중에 결제 조회로 확정한다.
     */
    public void markUnknown(String paymentKey, Instant now) {
        if (status != DonationStatus.PENDING && status != DonationStatus.UNKNOWN) {
            return;
        }
        this.status = DonationStatus.UNKNOWN;
        this.paymentKey = paymentKey;
        this.updatedAt = now;
    }

    /** 결제 조회 결과 결제가 되지 않은 것으로 확인됨(취소, 만료, 중단) */
    public void resolveAsFailed(String reason, Instant now) {
        if (!status.isUnknown()) {
            return;
        }
        close(reason, now);
    }

    private void close(String reason, Instant now) {
        this.status = DonationStatus.FAILED;
        this.failureReason = reason == null ? null : reason.substring(0, Math.min(reason.length(), 200));
        this.updatedAt = now;
    }

    public boolean isForBroadcast() {
        return targetType == DonationTargetType.BROADCAST;
    }

    /** 확정됐고 응원 대상(구단/선수)이 있어 랭킹 점수에 반영할 후원인지 */
    public boolean countsForFanTarget() {
        return targetId != null && status == DonationStatus.DONE;
    }

    public String orderName() {
        return isForBroadcast() ? streamerNickname + "님 후원" : targetName + " 응원금";
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public Long getBroadcastId() { return broadcastId; }
    public Long getStreamerId() { return streamerId; }
    public String getStreamerNickname() { return streamerNickname; }
    public Long getDonorId() { return donorId; }
    public String getDonorNickname() { return donorNickname; }
    public long getAmount() { return amount; }
    public String getMessage() { return message; }
    public DonationStatus getStatus() { return status; }
    public String getPaymentKey() { return paymentKey; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public DonationTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
}
