package com.eventplatform.venue.service;

import com.eventplatform.common.exception.ApiException;
import com.eventplatform.venue.domain.Hall;
import com.eventplatform.venue.domain.Seat;
import com.eventplatform.venue.domain.SeatType;
import com.eventplatform.venue.domain.Venue;
import com.eventplatform.venue.dto.BulkSeatLayoutRequest;
import com.eventplatform.venue.dto.CreateHallRequest;
import com.eventplatform.venue.dto.CreateSeatRequest;
import com.eventplatform.venue.dto.CreateVenueRequest;
import com.eventplatform.venue.dto.HallResponse;
import com.eventplatform.venue.dto.SeatResponse;
import com.eventplatform.venue.dto.UpdateSeatRequest;
import com.eventplatform.venue.dto.UpdateVenueRequest;
import com.eventplatform.venue.dto.VenueResponse;
import com.eventplatform.venue.repository.HallRepository;
import com.eventplatform.venue.repository.SeatRepository;
import com.eventplatform.venue.repository.VenueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;

    public VenueService(VenueRepository venueRepository, HallRepository hallRepository, SeatRepository seatRepository) {
        this.venueRepository = venueRepository;
        this.hallRepository = hallRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public VenueResponse createVenue(CreateVenueRequest request) {
        if (venueRepository.existsByNameIgnoreCaseAndCityIgnoreCase(request.name().trim(), request.city().trim())) {
            throw ApiException.conflict("Venue with the same name already exists in this city");
        }
        Venue venue = new Venue();
        venue.setName(request.name().trim());
        venue.setAddress(request.address().trim());
        venue.setCity(request.city().trim());
        venue.setState(request.state().trim());
        venue.setActive(true);
        venueRepository.save(venue);
        return VenueResponse.summary(venue);
    }

    @Transactional
    public VenueResponse updateVenue(UUID venueId, UpdateVenueRequest request) {
        Venue venue = requireVenue(venueId);
        if (StringUtils.hasText(request.name())) {
            venue.setName(request.name().trim());
        }
        if (StringUtils.hasText(request.address())) {
            venue.setAddress(request.address().trim());
        }
        if (StringUtils.hasText(request.city())) {
            venue.setCity(request.city().trim());
        }
        if (StringUtils.hasText(request.state())) {
            venue.setState(request.state().trim());
        }
        if (request.active() != null) {
            venue.setActive(request.active());
        }
        return toDetail(venue);
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenue(UUID venueId, boolean activeOnly) {
        Venue venue = requireVenue(venueId);
        if (activeOnly && !venue.isActive()) {
            throw ApiException.notFound("Venue not found");
        }
        return toDetail(venue);
    }

    @Transactional(readOnly = true)
    public Page<VenueResponse> listAdmin(Pageable pageable) {
        return venueRepository.findAll(pageable).map(VenueResponse::summary);
    }

    @Transactional(readOnly = true)
    public Page<VenueResponse> listAvailable(String city, Pageable pageable) {
        Page<Venue> page = StringUtils.hasText(city)
                ? venueRepository.findByActiveTrueAndCityIgnoreCase(city.trim(), pageable)
                : venueRepository.findByActiveTrue(pageable);
        return page.map(VenueResponse::summary);
    }

    @Transactional
    public HallResponse createHall(UUID venueId, CreateHallRequest request) {
        Venue venue = requireVenue(venueId);
        if (hallRepository.existsByVenueIdAndNameIgnoreCase(venueId, request.name().trim())) {
            throw ApiException.conflict("Hall name already exists in this venue");
        }
        Hall hall = new Hall();
        hall.setVenue(venue);
        hall.setName(request.name().trim());
        hall.setCapacity(request.capacity());
        hallRepository.save(hall);
        return HallResponse.from(hall, List.of());
    }

    @Transactional
    public SeatResponse addSeat(UUID venueId, UUID hallId, CreateSeatRequest request) {
        Hall hall = requireHall(venueId, hallId);
        String row = request.rowLabel().trim().toUpperCase();
        if (seatRepository.existsByHallIdAndRowLabelIgnoreCaseAndSeatNumber(hall.getId(), row, request.seatNumber())) {
            throw ApiException.conflict("Seat already exists in this hall");
        }
        long current = seatRepository.countByHallId(hall.getId());
        if (current + 1 > hall.getCapacity()) {
            throw ApiException.unprocessable("Seat count would exceed hall capacity");
        }
        Seat seat = new Seat();
        seat.setHall(hall);
        seat.setRowLabel(row);
        seat.setSeatNumber(request.seatNumber());
        seat.setSeatType(request.seatType());
        seat.setEnabled(true);
        seatRepository.save(seat);
        return SeatResponse.from(seat);
    }

    @Transactional
    public List<SeatResponse> configureLayout(UUID venueId, UUID hallId, BulkSeatLayoutRequest request) {
        Hall hall = requireHall(venueId, hallId);
        Set<String> uniqueRows = new HashSet<>();
        for (String row : request.rows()) {
            if (!StringUtils.hasText(row) || !uniqueRows.add(row.trim().toUpperCase())) {
                throw ApiException.badRequest("Duplicate or blank row labels are not allowed");
            }
        }
        int total = uniqueRows.size() * request.seatsPerRow();
        if (total > hall.getCapacity()) {
            throw ApiException.unprocessable("Layout exceeds hall capacity of " + hall.getCapacity());
        }
        if (seatRepository.countByHallId(hall.getId()) > 0) {
            throw ApiException.conflict("Hall already has seats; add or update seats individually");
        }
        List<Seat> created = new ArrayList<>();
        for (String row : uniqueRows) {
            for (int number = 1; number <= request.seatsPerRow(); number++) {
                Seat seat = new Seat();
                seat.setHall(hall);
                seat.setRowLabel(row);
                seat.setSeatNumber(number);
                seat.setSeatType(request.defaultType() == null ? SeatType.REGULAR : request.defaultType());
                seat.setEnabled(true);
                created.add(seat);
            }
        }
        return seatRepository.saveAll(created).stream().map(SeatResponse::from).toList();
    }

    @Transactional
    public SeatResponse updateSeat(UUID venueId, UUID hallId, UUID seatId, UpdateSeatRequest request) {
        requireHall(venueId, hallId);
        Seat seat = seatRepository.findByIdAndHallId(seatId, hallId)
                .orElseThrow(() -> ApiException.notFound("Seat not found"));
        if (request.seatType() != null) {
            seat.setSeatType(request.seatType());
        }
        if (request.enabled() != null) {
            seat.setEnabled(request.enabled());
        }
        return SeatResponse.from(seat);
    }

    @Transactional(readOnly = true)
    public HallResponse getHall(UUID venueId, UUID hallId) {
        Hall hall = requireHall(venueId, hallId);
        List<SeatResponse> seats = seatRepository.findByHallIdOrderByRowLabelAscSeatNumberAsc(hallId)
                .stream().map(SeatResponse::from).toList();
        return HallResponse.from(hall, seats);
    }

    private VenueResponse toDetail(Venue venue) {
        List<HallResponse> halls = hallRepository.findByVenueId(venue.getId()).stream()
                .map(hall -> HallResponse.from(hall, seatRepository.findByHallIdOrderByRowLabelAscSeatNumberAsc(hall.getId())
                        .stream().map(SeatResponse::from).toList()))
                .toList();
        return VenueResponse.from(venue, halls);
    }

    private Venue requireVenue(UUID venueId) {
        return venueRepository.findById(venueId).orElseThrow(() -> ApiException.notFound("Venue not found"));
    }

    private Hall requireHall(UUID venueId, UUID hallId) {
        return hallRepository.findByIdAndVenueId(hallId, venueId)
                .orElseThrow(() -> ApiException.notFound("Hall not found"));
    }
}
