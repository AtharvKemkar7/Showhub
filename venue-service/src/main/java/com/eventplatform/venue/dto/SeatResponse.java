package com.eventplatform.venue.dto;

import com.eventplatform.venue.domain.Seat;
import com.eventplatform.venue.domain.SeatType;

import java.util.UUID;

public record SeatResponse(
        UUID id,
        String rowLabel,
        int seatNumber,
        SeatType seatType,
        boolean enabled
) {
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(), seat.getSeatType(), seat.isEnabled());
    }
}
