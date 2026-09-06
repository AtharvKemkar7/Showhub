package com.eventplatform.venue.repository;

import com.eventplatform.venue.domain.Hall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HallRepository extends JpaRepository<Hall, UUID> {

    List<Hall> findByVenueId(UUID venueId);

    Optional<Hall> findByIdAndVenueId(UUID id, UUID venueId);

    boolean existsByVenueIdAndNameIgnoreCase(UUID venueId, String name);

    long countByVenueId(UUID venueId);
}
