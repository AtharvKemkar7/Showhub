package com.eventplatform.venue.repository;

import com.eventplatform.venue.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByHallIdOrderByRowLabelAscSeatNumberAsc(UUID hallId);

    Optional<Seat> findByIdAndHallId(UUID id, UUID hallId);

    boolean existsByHallIdAndRowLabelIgnoreCaseAndSeatNumber(UUID hallId, String rowLabel, int seatNumber);

    long countByHallId(UUID hallId);

    long countByHallIdAndEnabledTrue(UUID hallId);
}
