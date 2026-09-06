package com.eventplatform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "show_seats")
public class ShowSeat {

    @Id
    private UUID id;

    @Column(name = "show_id", nullable = false)
    private UUID showId;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "seat_type", nullable = false, length = 20)
    private String seatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatState state;

    @Column(name = "lock_id")
    private UUID lockId;

    @Column(name = "lock_owner_id")
    private UUID lockOwnerId;

    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (state == null) {
            state = SeatState.AVAILABLE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isLockExpired(Instant now) {
        return state == SeatState.LOCKED && (lockExpiresAt == null || !lockExpiresAt.isAfter(now));
    }

    public UUID getId() {
        return id;
    }

    public UUID getShowId() {
        return showId;
    }

    public void setShowId(UUID showId) {
        this.showId = showId;
    }

    public UUID getSeatId() {
        return seatId;
    }

    public void setSeatId(UUID seatId) {
        this.seatId = seatId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public SeatState getState() {
        return state;
    }

    public void setState(SeatState state) {
        this.state = state;
    }

    public UUID getLockId() {
        return lockId;
    }

    public void setLockId(UUID lockId) {
        this.lockId = lockId;
    }

    public UUID getLockOwnerId() {
        return lockOwnerId;
    }

    public void setLockOwnerId(UUID lockOwnerId) {
        this.lockOwnerId = lockOwnerId;
    }

    public Instant getLockExpiresAt() {
        return lockExpiresAt;
    }

    public void setLockExpiresAt(Instant lockExpiresAt) {
        this.lockExpiresAt = lockExpiresAt;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }
}
