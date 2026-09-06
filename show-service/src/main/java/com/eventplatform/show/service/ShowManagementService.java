package com.eventplatform.show.service;

import com.eventplatform.common.exception.ApiException;
import com.eventplatform.common.security.AuthenticatedUser;
import com.eventplatform.common.security.Role;
import com.eventplatform.show.client.CatalogClient;
import com.eventplatform.show.domain.Show;
import com.eventplatform.show.domain.ShowPrice;
import com.eventplatform.show.domain.ShowStatus;
import com.eventplatform.show.dto.CreateShowRequest;
import com.eventplatform.show.dto.SeatPriceRequest;
import com.eventplatform.show.dto.ShowResponse;
import com.eventplatform.show.dto.UpdateShowRequest;
import com.eventplatform.show.repository.ShowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ShowManagementService {

    private final ShowRepository showRepository;
    private final CatalogClient catalogClient;

    public ShowManagementService(ShowRepository showRepository, CatalogClient catalogClient) {
        this.showRepository = showRepository;
        this.catalogClient = catalogClient;
    }

    @Transactional
    public ShowResponse create(AuthenticatedUser actor, CreateShowRequest request) {
        validateDates(request.startAt(), request.endAt());
        CatalogClient.EventInfo event = catalogClient.requirePublishedOrOwnedEvent(
                request.eventId(), actor.getId(), actor.isAdmin());
        if (!actor.isAdmin() && !event.organizerId().equals(actor.getId())) {
            throw ApiException.forbidden("Organizer can manage shows only for their own events");
        }
        if (!event.published() && !actor.isAdmin()) {
            throw ApiException.unprocessable("Shows can only be created for published events");
        }
        CatalogClient.VenueInfo venue = catalogClient.requireVenueHall(request.venueId(), request.hallId());
        if (!venue.active()) {
            throw ApiException.unprocessable("Venue is not available");
        }
        assertNoOverlap(request.hallId(), request.startAt(), request.endAt(), null);
        Show show = new Show();
        show.setEventId(event.id());
        show.setOrganizerId(event.organizerId());
        show.setVenueId(request.venueId());
        show.setHallId(request.hallId());
        show.setLanguage(StringUtils.hasText(request.language()) ? request.language().trim() : "en");
        show.setStartAt(request.startAt());
        show.setEndAt(request.endAt());
        show.setStatus(ShowStatus.SCHEDULED);
        applyPrices(show, request.prices());
        showRepository.save(show);
        return ShowResponse.from(show);
    }

    @Transactional
    public ShowResponse update(AuthenticatedUser actor, UUID showId, UpdateShowRequest request) {
        Show show = requireAccessible(actor, showId);
        if (show.getStatus() == ShowStatus.CANCELLED || show.getStatus() == ShowStatus.COMPLETED) {
            throw ApiException.conflict("Cancelled or completed shows cannot be modified");
        }
        Instant start = request.startAt() != null ? request.startAt() : show.getStartAt();
        Instant end = request.endAt() != null ? request.endAt() : show.getEndAt();
        validateDates(start, end);
        assertNoOverlap(show.getHallId(), start, end, show.getId());
        show.setStartAt(start);
        show.setEndAt(end);
        if (StringUtils.hasText(request.language())) {
            show.setLanguage(request.language().trim());
        }
        if (request.status() != null) {
            show.setStatus(request.status());
        }
        if (request.prices() != null && !request.prices().isEmpty()) {
            show.getPrices().clear();
            applyPrices(show, request.prices());
        }
        return ShowResponse.from(show);
    }

    @Transactional
    public void delete(AuthenticatedUser actor, UUID showId) {
        Show show = requireAccessible(actor, showId);
        if (show.getStatus() == ShowStatus.ACTIVE) {
            throw ApiException.conflict("Active shows cannot be deleted; cancel them instead");
        }
        show.setStatus(ShowStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public ShowResponse get(AuthenticatedUser actor, UUID showId) {
        Show show = showRepository.findById(showId).orElseThrow(() -> ApiException.notFound("Show not found"));
        if (actor == null || actor.getRole() == Role.CUSTOMER) {
            if (show.getStatus() == ShowStatus.CANCELLED) {
                throw ApiException.notFound("Show not found");
            }
            CatalogClient.EventInfo event = catalogClient.requirePublishedOrOwnedEvent(show.getEventId(), null, false);
            if (!event.published()) {
                throw ApiException.notFound("Show not found");
            }
            return ShowResponse.from(show);
        }
        return ShowResponse.from(requireAccessible(actor, showId));
    }

    @Transactional(readOnly = true)
    public Page<ShowResponse> search(UUID eventId, UUID venueId, ShowStatus status, UUID organizerId, Pageable pageable) {
        Specification<Show> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (eventId != null) {
                predicates.add(cb.equal(root.get("eventId"), eventId));
            }
            if (venueId != null) {
                predicates.add(cb.equal(root.get("venueId"), venueId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (organizerId != null) {
                predicates.add(cb.equal(root.get("organizerId"), organizerId));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return showRepository.findAll(spec, pageable).map(ShowResponse::from);
    }

    public Show requireAccessible(AuthenticatedUser actor, UUID showId) {
        Show show = showRepository.findById(showId).orElseThrow(() -> ApiException.notFound("Show not found"));
        if (actor.isAdmin()) {
            return show;
        }
        if (actor.isOrganizer() && show.getOrganizerId().equals(actor.getId())) {
            return show;
        }
        throw ApiException.forbidden("Access denied");
    }

    private void applyPrices(Show show, List<SeatPriceRequest> prices) {
        Set<String> types = new HashSet<>();
        for (SeatPriceRequest price : prices) {
            if (!types.add(price.seatType().name())) {
                throw ApiException.badRequest("Duplicate seat type pricing");
            }
            ShowPrice entity = new ShowPrice();
            entity.setShow(show);
            entity.setSeatType(price.seatType());
            entity.setAmountCents(price.amountCents());
            entity.setCurrency("INR");
            show.getPrices().add(entity);
        }
    }

    private void validateDates(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("End time must be after start time");
        }
    }

    private void assertNoOverlap(UUID hallId, Instant start, Instant end, UUID excludeId) {
        if (showRepository.countOverlapping(hallId, start, end, excludeId) > 0) {
            throw ApiException.conflict("Overlapping show already exists in this hall");
        }
    }
}
