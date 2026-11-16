package com.livebroadcast.fandom.domain.target;

import com.livebroadcast.common.error.DomainException;

public final class TargetErrors {

    private TargetErrors() {
    }

    public static DomainException notFound() {
        return DomainException.notFound("TARGET_NOT_FOUND", "응원 대상을 찾을 수 없습니다.");
    }

    public static DomainException leagueNotFound() {
        return DomainException.notFound("LEAGUE_NOT_FOUND", "리그를 찾을 수 없습니다.");
    }

    public static DomainException requestNotFound() {
        return DomainException.notFound("TARGET_REQUEST_NOT_FOUND", "등록 신청을 찾을 수 없습니다.");
    }

    public static DomainException leagueClosed() {
        return DomainException.badRequest("LEAGUE_CLOSED_FOR_REQUEST", "이 리그는 운영자가 직접 관리해서 등록 신청을 받지 않습니다.");
    }

    public static DomainException alreadyExists() {
        return DomainException.conflict("TARGET_ALREADY_EXISTS", "이미 등록된 응원 대상입니다.");
    }

    public static DomainException invalidName() {
        return DomainException.badRequest("INVALID_TARGET_NAME", "이름은 2~40자여야 합니다.");
    }

    public static DomainException invalidShortName() {
        return DomainException.badRequest("INVALID_SHORT_NAME", "약칭은 1~6자여야 합니다.");
    }

    public static DomainException invalidColor() {
        return DomainException.badRequest("INVALID_COLOR", "대표 색은 #RRGGBB 형식이어야 합니다.");
    }

    public static DomainException kindMismatch() {
        return DomainException.badRequest("TARGET_KIND_MISMATCH", "팀 종목 리그에는 구단을, 개인 종목 리그에는 선수를 신청해 주세요.");
    }

    public static DomainException requestClosed() {
        return DomainException.conflict("TARGET_REQUEST_CLOSED", "이미 처리된 신청입니다.");
    }

    public static DomainException rejectReasonRequired() {
        return DomainException.badRequest("REJECT_REASON_REQUIRED", "반려 사유를 입력해 주세요.");
    }

    public static DomainException alreadyVoted() {
        return DomainException.conflict("ALREADY_VOTED", "이미 \"나도 원해요\"를 눌렀습니다.");
    }

    public static DomainException notVisible() {
        return DomainException.badRequest("TARGET_NOT_ACTIVE", "운영이 중단된 응원 대상입니다.");
    }
}
