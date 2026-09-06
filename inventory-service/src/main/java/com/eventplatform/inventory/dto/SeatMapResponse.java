package com.eventplatform.inventory.dto;

import com.eventplatform.inventory.domain.SeatState;
import com.eventplatform.inventory.domain.ShowSeat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SeatMapResponse(
        UUID showId,
        List<SeatView> seats
) {
    public record SeatView(
            UUID seatId,
            String rowLabel,
            int seatNumber,
            String seatType,
            SeatState state,
            Instant lockExpiresAt
    ) {
        public static SeatView from(ShowSeat seat) {
            return new SeatView(
                    seat.getSeatId(),
                    seat.getRowLabel(),
                    seat.getSeatNumber(),
                    seat.getSeatType(),
                    seat.getState(),
                    seat.getLockExpiresAt()
            );
        }
    }
}
