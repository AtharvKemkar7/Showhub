package com.eventplatform.inventory.service;

import com.eventplatform.common.exception.ApiException;
import com.eventplatform.inventory.domain.InventoryCommand;
import com.eventplatform.inventory.domain.SeatState;
import com.eventplatform.inventory.domain.ShowSeat;
import com.eventplatform.inventory.dto.ConfirmLockRequest;
import com.eventplatform.inventory.dto.CreateInventoryRequest;
import com.eventplatform.inventory.dto.LockResponse;
import com.eventplatform.inventory.dto.LockSeatsRequest;
import com.eventplatform.inventory.dto.SeatMapResponse;
import com.eventplatform.inventory.repository.InventoryCommandRepository;
import com.eventplatform.inventory.repository.ShowSeatRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SeatLockService {

    private static final Logger log = LoggerFactory.getLogger(SeatLockService.class);
    private static final String REDIS_LOCK_PREFIX = "seat-lock:";

    private final ShowSeatRepository showSeatRepository;
    private final InventoryCommandRepository commandRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.lock.ttl:5m}")
    private Duration lockTtl;

    public SeatLockService(
            ShowSeatRepository showSeatRepository,
            InventoryCommandRepository commandRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.showSeatRepository = showSeatRepository;
        this.commandRepository = commandRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SeatMapResponse createInventory(CreateInventoryRequest request) {
        if (showSeatRepository.existsByShowId(request.showId())) {
            throw ApiException.conflict("Inventory already exists for this show");
        }
        Set<UUID> unique = new HashSet<>();
        List<ShowSeat> seats = new ArrayList<>();
        for (CreateInventoryRequest.SeatSpec spec : request.seats()) {
            if (!unique.add(spec.seatId())) {
                throw ApiException.badRequest("Duplicate seat in inventory payload");
            }
            ShowSeat seat = new ShowSeat();
            seat.setShowId(request.showId());
            seat.setSeatId(spec.seatId());
            seat.setRowLabel(spec.rowLabel().trim().toUpperCase());
            seat.setSeatNumber(spec.seatNumber());
            seat.setSeatType(spec.seatType().trim().toUpperCase());
            seat.setState(SeatState.AVAILABLE);
            seats.add(seat);
        }
        showSeatRepository.saveAll(seats);
        return seatMap(request.showId());
    }

    @Transactional(readOnly = true)
    public SeatMapResponse seatMap(UUID showId) {
        List<ShowSeat> seats = showSeatRepository.findByShowIdOrderByRowLabelAscSeatNumberAsc(showId);
        if (seats.isEmpty()) {
            throw ApiException.notFound("Inventory not found for show");
        }
        Instant now = Instant.now();
        List<SeatMapResponse.SeatView> views = seats.stream()
                .map(seat -> {
                    if (seat.isLockExpired(now)) {
                        return new SeatMapResponse.SeatView(
                                seat.getSeatId(),
                                seat.getRowLabel(),
                                seat.getSeatNumber(),
                                seat.getSeatType(),
                                SeatState.AVAILABLE,
                                null
                        );
                    }
                    return SeatMapResponse.SeatView.from(seat);
                })
                .toList();
        return new SeatMapResponse(showId, views);
    }

    @Transactional
    public LockResponse lock(UUID ownerId, LockSeatsRequest request, String idempotencyKey) {
        LockResponse cached = loadCached(idempotencyKey, "LOCK");
        if (cached != null) {
            return cached;
        }
        List<UUID> seatIds = request.seatIds().stream().distinct().sorted(Comparator.comparing(UUID::toString)).toList();
        List<ShowSeat> seats = showSeatRepository.lockSeats(request.showId(), seatIds);
        if (seats.size() != seatIds.size()) {
            throw ApiException.notFound("One or more seats do not exist for this show");
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lockTtl);
        UUID lockId = UUID.randomUUID();
        List<String> redisKeys = new ArrayList<>();
        try {
            for (ShowSeat seat : seats) {
                expireIfNeeded(seat, now);
                if (seat.getState() != SeatState.AVAILABLE) {
                    throw ApiException.conflict("Seat " + seat.getRowLabel() + seat.getSeatNumber() + " is not available");
                }
                String redisKey = redisKey(request.showId(), seat.getSeatId());
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(redisKey, ownerId + ":" + lockId, lockTtl.toSeconds(), TimeUnit.SECONDS);
                if (!Boolean.TRUE.equals(acquired)) {
                    throw ApiException.conflict("Seat is already locked");
                }
                redisKeys.add(redisKey);
                seat.setState(SeatState.LOCKED);
                seat.setLockId(lockId);
                seat.setLockOwnerId(ownerId);
                seat.setLockExpiresAt(expiresAt);
            }
        } catch (RuntimeException ex) {
            redisKeys.forEach(redisTemplate::delete);
            throw ex;
        }
        LockResponse response = new LockResponse(lockId, request.showId(), ownerId, expiresAt, seatIds, "LOCKED");
        cache(idempotencyKey, "LOCK", response);
        return response;
    }

    @Transactional
    public LockResponse release(UUID ownerId, UUID lockId, boolean admin, String idempotencyKey) {
        LockResponse cached = loadCached(idempotencyKey, "RELEASE");
        if (cached != null) {
            return cached;
        }
        List<ShowSeat> seats = showSeatRepository.findByLockId(lockId);
        if (seats.isEmpty()) {
            throw ApiException.notFound("Lock not found");
        }
        UUID showId = seats.getFirst().getShowId();
        for (ShowSeat seat : seats) {
            if (!admin && !ownerId.equals(seat.getLockOwnerId())) {
                throw ApiException.forbidden("Lock is owned by another user");
            }
            if (seat.getState() == SeatState.BOOKED) {
                throw ApiException.conflict("Booked seats cannot be released");
            }
            clearLock(seat);
        }
        LockResponse response = new LockResponse(lockId, showId, ownerId, Instant.now(), seatIds(seats), "RELEASED");
        cache(idempotencyKey, "RELEASE", response);
        return response;
    }

    @Transactional
    public LockResponse extend(UUID ownerId, UUID lockId, String idempotencyKey) {
        LockResponse cached = loadCached(idempotencyKey, "EXTEND");
        if (cached != null) {
            return cached;
        }
        List<ShowSeat> seats = showSeatRepository.findByLockId(lockId);
        if (seats.isEmpty()) {
            throw ApiException.notFound("Lock not found");
        }
        Instant expiresAt = Instant.now().plus(lockTtl);
        for (ShowSeat seat : seats) {
            if (!ownerId.equals(seat.getLockOwnerId())) {
                throw ApiException.forbidden("Lock is owned by another user");
            }
            if (seat.getState() != SeatState.LOCKED) {
                throw ApiException.conflict("Only locked seats can be extended");
            }
            seat.setLockExpiresAt(expiresAt);
            redisTemplate.opsForValue().set(
                    redisKey(seat.getShowId(), seat.getSeatId()),
                    ownerId + ":" + lockId,
                    lockTtl.toSeconds(),
                    TimeUnit.SECONDS
            );
        }
        LockResponse response = new LockResponse(lockId, seats.getFirst().getShowId(), ownerId, expiresAt, seatIds(seats), "EXTENDED");
        cache(idempotencyKey, "EXTEND", response);
        return response;
    }

    @Transactional
    public LockResponse confirm(UUID ownerId, ConfirmLockRequest request, boolean serviceCall, String idempotencyKey) {
        LockResponse cached = loadCached(idempotencyKey, "CONFIRM");
        if (cached != null) {
            return cached;
        }
        List<ShowSeat> seats = showSeatRepository.findByLockId(request.lockId());
        if (seats.isEmpty()) {
            throw ApiException.notFound("Lock not found");
        }
        Instant now = Instant.now();
        for (ShowSeat seat : seats) {
            if (!serviceCall && !ownerId.equals(seat.getLockOwnerId())) {
                throw ApiException.forbidden("Lock is owned by another user");
            }
            expireIfNeeded(seat, now);
            if (seat.getState() != SeatState.LOCKED || !request.lockId().equals(seat.getLockId())) {
                throw ApiException.conflict("Seats are not locked by this lock");
            }
            seat.setState(SeatState.BOOKED);
            seat.setBookingId(request.bookingId());
            seat.setLockExpiresAt(null);
            redisTemplate.delete(redisKey(seat.getShowId(), seat.getSeatId()));
        }
        LockResponse response = new LockResponse(
                request.lockId(),
                seats.getFirst().getShowId(),
                seats.getFirst().getLockOwnerId(),
                Instant.now(),
                seatIds(seats),
                "BOOKED"
        );
        cache(idempotencyKey, "CONFIRM", response);
        return response;
    }

    @Transactional
    public LockResponse unbook(UUID bookingId, String idempotencyKey) {
        LockResponse cached = loadCached(idempotencyKey, "UNBOOK");
        if (cached != null) {
            return cached;
        }
        List<ShowSeat> seats = showSeatRepository.findByBookingId(bookingId);
        if (seats.isEmpty()) {
            throw ApiException.notFound("Booked seats not found for booking");
        }
        for (ShowSeat seat : seats) {
            if (seat.getState() != SeatState.BOOKED) {
                throw ApiException.conflict("Seats are not booked");
            }
            clearLock(seat);
        }
        LockResponse response = new LockResponse(
                null,
                seats.getFirst().getShowId(),
                null,
                Instant.now(),
                seatIds(seats),
                "AVAILABLE"
        );
        cache(idempotencyKey, "UNBOOK", response);
        return response;
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void expireLocks() {
        List<ShowSeat> expired = showSeatRepository.findByStateAndLockExpiresAtBefore(SeatState.LOCKED, Instant.now());
        for (ShowSeat seat : expired) {
            clearLock(seat);
        }
        if (!expired.isEmpty()) {
            log.info("Released {} expired seat locks", expired.size());
        }
    }

    private void expireIfNeeded(ShowSeat seat, Instant now) {
        if (seat.isLockExpired(now)) {
            clearLock(seat);
        }
    }

    private void clearLock(ShowSeat seat) {
        redisTemplate.delete(redisKey(seat.getShowId(), seat.getSeatId()));
        seat.setState(SeatState.AVAILABLE);
        seat.setLockId(null);
        seat.setLockOwnerId(null);
        seat.setLockExpiresAt(null);
        seat.setBookingId(null);
    }

    private String redisKey(UUID showId, UUID seatId) {
        return REDIS_LOCK_PREFIX + showId + ":" + seatId;
    }

    private List<UUID> seatIds(List<ShowSeat> seats) {
        return seats.stream().map(ShowSeat::getSeatId).toList();
    }

    private LockResponse loadCached(String idempotencyKey, String type) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        return commandRepository.findById(idempotencyKey)
                .filter(cmd -> type.equals(cmd.getCommandType()))
                .map(cmd -> read(cmd.getResponseJson()))
                .orElse(null);
    }

    private void cache(String idempotencyKey, String type, LockResponse response) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return;
        }
        InventoryCommand command = new InventoryCommand();
        command.setIdempotencyKey(idempotencyKey);
        command.setCommandType(type);
        command.setResponseJson(write(response));
        commandRepository.save(command);
    }

    private String write(LockResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize lock response");
        }
    }

    private LockResponse read(String json) {
        try {
            return objectMapper.readValue(json, LockResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize cached lock response");
        }
    }
}
