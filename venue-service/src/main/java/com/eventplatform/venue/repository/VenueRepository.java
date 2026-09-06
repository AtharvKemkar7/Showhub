package com.eventplatform.venue.repository;

import com.eventplatform.venue.domain.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    Page<Venue> findByActiveTrue(Pageable pageable);

    Page<Venue> findByActiveTrueAndCityIgnoreCase(String city, Pageable pageable);
}
