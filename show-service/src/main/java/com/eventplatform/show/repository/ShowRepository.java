package com.eventplatform.show.repository;

import com.eventplatform.show.domain.Show;
import com.eventplatform.show.domain.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowRepository extends JpaRepository<Show, UUID>, JpaSpecificationExecutor<Show> {

    Optional<Show> findByIdAndOrganizerId(UUID id, UUID organizerId);

    List<Show> findByEventIdAndStatusNot(UUID eventId, ShowStatus status);

    @Query("""
            select count(s) from Show s
            where s.hallId = :hallId
              and s.status <> com.eventplatform.show.domain.ShowStatus.CANCELLED
              and s.id <> coalesce(:excludeId, '00000000-0000-0000-0000-000000000000')
              and s.startAt < :endAt
              and s.endAt > :startAt
            """)
    long countOverlapping(
            @Param("hallId") UUID hallId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludeId") UUID excludeId
    );
}
