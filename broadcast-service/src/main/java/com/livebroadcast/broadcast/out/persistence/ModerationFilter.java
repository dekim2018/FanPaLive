package com.livebroadcast.broadcast.out.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/** 운영자가 중단한 방송(moderated_at)의 녹화와 클립을 공개 목록에서 빼는 조건 */
final class ModerationFilter {

    private ModerationFilter() {
    }

    static Predicate notModerated(Root<?> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Long> moderated = query.subquery(Long.class);
        Root<BroadcastJpaEntity> b = moderated.from(BroadcastJpaEntity.class);
        moderated.select(b.get("id")).where(cb.isNotNull(b.get("moderatedAt")));
        return cb.not(root.get("broadcastId").in(moderated));
    }
}
