package com.eventplatform.booking.repository;

import com.eventplatform.booking.domain.Booking;
import com.eventplatform.booking.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @EntityGraph(attributePaths = "items")
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findWithItemsById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "items")
    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Page<Booking> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Page<Booking> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Booking> findByStatusAndLockExpiresAtBefore(BookingStatus status, Instant cutoff);
}
