package com.eventplatform.search.repository;

import com.eventplatform.search.domain.SearchEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface SearchEventRepository extends JpaRepository<SearchEvent, UUID> {

    @Query(
            value = """
                    SELECT * FROM search_events e
                    WHERE e.status = 'PUBLISHED'
                      AND (:q IS NULL OR lower(e.title) LIKE concat('%', lower(cast(:q as text)), '%')
                           OR (e.description IS NOT NULL AND lower(e.description) LIKE concat('%', lower(cast(:q as text)), '%')))
                      AND (:city IS NULL OR lower(e.city) = lower(cast(:city as text)))
                      AND (:category IS NULL OR lower(e.category_name) = lower(cast(:category as text)))
                      AND (cast(:fromTs as timestamptz) IS NULL OR e.start_at >= cast(:fromTs as timestamptz))
                      AND (cast(:toTs as timestamptz) IS NULL OR e.start_at <= cast(:toTs as timestamptz))
                    """,
            countQuery = """
                    SELECT count(*) FROM search_events e
                    WHERE e.status = 'PUBLISHED'
                      AND (:q IS NULL OR lower(e.title) LIKE concat('%', lower(cast(:q as text)), '%')
                           OR (e.description IS NOT NULL AND lower(e.description) LIKE concat('%', lower(cast(:q as text)), '%')))
                      AND (:city IS NULL OR lower(e.city) = lower(cast(:city as text)))
                      AND (:category IS NULL OR lower(e.category_name) = lower(cast(:category as text)))
                      AND (cast(:fromTs as timestamptz) IS NULL OR e.start_at >= cast(:fromTs as timestamptz))
                      AND (cast(:toTs as timestamptz) IS NULL OR e.start_at <= cast(:toTs as timestamptz))
                    """,
            nativeQuery = true
    )
    Page<SearchEvent> search(
            @Param("q") String q,
            @Param("city") String city,
            @Param("category") String category,
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to,
            Pageable pageable
    );
}
