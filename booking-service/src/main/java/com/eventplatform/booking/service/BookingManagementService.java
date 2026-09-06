package com.eventplatform.booking.service;

import com.eventplatform.booking.client.InventoryClient;
import com.eventplatform.booking.client.ShowCatalogClient;
import com.eventplatform.booking.domain.Booking;
import com.eventplatform.booking.domain.BookingCommand;
import com.eventplatform.booking.domain.BookingItem;
import com.eventplatform.booking.domain.BookingStatus;
import com.eventplatform.booking.dto.BookingResponse;
import com.eventplatform.booking.dto.CreateBookingRequest;
import com.eventplatform.booking.repository.BookingCommandRepository;
import com.eventplatform.booking.repository.BookingRepository;
import com.eventplatform.common.exception.ApiException;
import com.eventplatform.common.kafka.Topics;
import com.eventplatform.common.messaging.DomainEvent;
import com.eventplatform.common.messaging.EventPublisher;
import com.eventplatform.common.security.AuthenticatedUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class BookingManagementService {

    private static final Logger log = LoggerFactory.getLogger(BookingManagementService.class);

    private final BookingRepository bookingRepository;
    private final BookingCommandRepository commandRepository;
    private final ShowCatalogClient showCatalogClient;
    private final InventoryClient inventoryClient;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${app.booking.pending-ttl:5m}")
    private Duration pendingTtl;

    public BookingManagementService(
            BookingRepository bookingRepository,
            BookingCommandRepository commandRepository,
            ShowCatalogClient showCatalogClient,
            InventoryClient inventoryClient,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper
    ) {
        this.bookingRepository = bookingRepository;
        this.commandRepository = commandRepository;
        this.showCatalogClient = showCatalogClient;
        this.inventoryClient = inventoryClient;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BookingResponse create(AuthenticatedUser user, CreateBookingRequest request, String idempotencyKey) {
        BookingResponse cached = loadCached(idempotencyKey, "CREATE");
        if (cached != null) {
            return cached;
        }
        ShowCatalogClient.ShowInfo show = showCatalogClient.requireBookableShow(request.showId());
        List<UUID> seatIds = request.seatIds().stream().distinct().toList();
        InventoryClient.SeatMap seatMap = inventoryClient.seatMap(request.showId());
        InventoryClient.LockResult lock = inventoryClient.lock(user.getId(), request.showId(), seatIds, idempotencyKey);

        Booking booking = new Booking();
        booking.setCustomerId(user.getId());
        booking.setShowId(show.id());
        booking.setEventId(show.eventId());
        booking.setOrganizerId(show.organizerId());
        booking.setVenueId(show.venueId());
        booking.setHallId(show.hallId());
        booking.setLockId(lock.lockId());
        booking.setLockExpiresAt(lock.expiresAt() != null ? lock.expiresAt() : Instant.now().plus(pendingTtl));
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setCurrency("INR");

        int total = 0;
        LinkedHashSet<UUID> unique = new LinkedHashSet<>(seatIds);
        for (UUID seatId : unique) {
            InventoryClient.SeatView seat = seatMap.seat(seatId);
            if (seat == null) {
                throw ApiException.notFound("Seat not found in inventory");
            }
            ShowCatalogClient.ShowInfo.Price price = show.priceFor(seat.seatType());
            if (price == null) {
                throw ApiException.unprocessable("No price configured for seat type " + seat.seatType());
            }
            BookingItem item = new BookingItem();
            item.setBooking(booking);
            item.setSeatId(seat.seatId());
            item.setRowLabel(seat.rowLabel());
            item.setSeatNumber(seat.seatNumber());
            item.setSeatType(seat.seatType());
            item.setAmountCents(price.amountCents());
            item.setCurrency(price.currency());
            booking.getItems().add(item);
            total += price.amountCents();
        }
        booking.setTotalCents(total);
        bookingRepository.save(booking);
        BookingResponse response = BookingResponse.from(booking);
        cache(idempotencyKey, "CREATE", booking.getId(), response);
        publish("booking.created", booking);
        return response;
    }

    @Transactional(readOnly = true)
    public BookingResponse get(AuthenticatedUser user, UUID bookingId) {
        Booking booking = require(bookingId);
        assertCanView(user, booking);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> mine(UUID customerId, Pageable pageable) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable).map(BookingResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> organizer(UUID organizerId, Pageable pageable) {
        return bookingRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerId, pageable).map(BookingResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> admin(Pageable pageable) {
        return bookingRepository.findAllByOrderByCreatedAtDesc(pageable).map(BookingResponse::from);
    }

    @Transactional
    public BookingResponse confirmPayment(UUID bookingId, UUID paymentId, String idempotencyKey) {
        BookingResponse cached = loadCached(idempotencyKey, "CONFIRM");
        if (cached != null) {
            return cached;
        }
        Booking booking = require(bookingId);
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            BookingResponse response = BookingResponse.from(booking);
            cache(idempotencyKey, "CONFIRM", bookingId, response);
            return response;
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("Booking cannot be confirmed");
        }
        inventoryClient.confirm(booking.getLockId(), booking.getId(), idempotencyKey);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentId);
        booking.setConfirmedAt(Instant.now());
        BookingResponse response = BookingResponse.from(booking);
        cache(idempotencyKey, "CONFIRM", bookingId, response);
        publish("booking.confirmed", booking);
        return response;
    }

    @Transactional
    public BookingResponse cancel(AuthenticatedUser user, UUID bookingId, String reason, String idempotencyKey) {
        BookingResponse cached = loadCached(idempotencyKey, "CANCEL");
        if (cached != null) {
            return cached;
        }
        Booking booking = require(bookingId);
        assertCanCancel(user, booking);
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            BookingResponse response = BookingResponse.from(booking);
            cache(idempotencyKey, "CANCEL", bookingId, response);
            return response;
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            inventoryClient.unbook(booking.getId(), idempotencyKey);
        } else if (booking.getLockId() != null) {
            inventoryClient.release(booking.getLockId(), idempotencyKey);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelReason(StringUtils.hasText(reason) ? reason : "Cancelled");
        booking.setCancelledAt(Instant.now());
        BookingResponse response = BookingResponse.from(booking);
        cache(idempotencyKey, "CANCEL", bookingId, response);
        publish("booking.cancelled", booking);
        return response;
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void expirePending() {
        List<Booking> expired = bookingRepository.findByStatusAndLockExpiresAtBefore(BookingStatus.PENDING_PAYMENT, Instant.now());
        for (Booking booking : expired) {
            try {
                if (booking.getLockId() != null) {
                    inventoryClient.release(booking.getLockId(), "expire-" + booking.getId());
                }
            } catch (RuntimeException ex) {
                log.warn("Failed to release lock for expired booking {}", booking.getId());
            }
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setCancelReason("Payment window expired");
            booking.setCancelledAt(Instant.now());
            publish("booking.expired", booking);
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} pending bookings", expired.size());
        }
    }

    private Booking require(UUID bookingId) {
        return bookingRepository.findWithItemsById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
    }

    private void assertCanView(AuthenticatedUser user, Booking booking) {
        if (user.isAdmin()) {
            return;
        }
        if (user.isOrganizer() && user.getId().equals(booking.getOrganizerId())) {
            return;
        }
        if (user.getId().equals(booking.getCustomerId())) {
            return;
        }
        throw ApiException.forbidden("Not allowed to view this booking");
    }

    private void assertCanCancel(AuthenticatedUser user, Booking booking) {
        if (user.isAdmin()) {
            return;
        }
        if (user.getId().equals(booking.getCustomerId())) {
            return;
        }
        throw ApiException.forbidden("Not allowed to cancel this booking");
    }

    private void publish(String type, Booking booking) {
        eventPublisher.publish(
                Topics.BOOKING_EVENTS,
                booking.getId().toString(),
                DomainEvent.of(type, booking.getId().toString(), booking.getCustomerId(), write(BookingResponse.from(booking)))
        );
    }

    private BookingResponse loadCached(String idempotencyKey, String type) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        return commandRepository.findById(idempotencyKey)
                .filter(cmd -> type.equals(cmd.getCommandType()))
                .map(cmd -> read(cmd.getResponseJson()))
                .orElse(null);
    }

    private void cache(String idempotencyKey, String type, UUID bookingId, BookingResponse response) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return;
        }
        BookingCommand command = new BookingCommand();
        command.setIdempotencyKey(idempotencyKey);
        command.setCommandType(type);
        command.setBookingId(bookingId);
        command.setResponseJson(write(response));
        commandRepository.save(command);
    }

    private String write(BookingResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize booking response");
        }
    }

    private BookingResponse read(String json) {
        try {
            return objectMapper.readValue(json, BookingResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize cached booking response");
        }
    }
}
