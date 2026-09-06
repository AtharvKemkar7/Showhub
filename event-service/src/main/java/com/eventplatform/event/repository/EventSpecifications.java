package com.eventplatform.event.repository;

import com.eventplatform.event.domain.Event;
import com.eventplatform.event.domain.EventStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> filter(
            EventStatus status,
            UUID categoryId,
            String city,
            String title,
            Instant from,
            Instant to,
            UUID organizerId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (StringUtils.hasText(city)) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase()));
            }
            if (StringUtils.hasText(title)) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%"));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), to));
            }
            if (organizerId != null) {
                predicates.add(cb.equal(root.get("organizerId"), organizerId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
