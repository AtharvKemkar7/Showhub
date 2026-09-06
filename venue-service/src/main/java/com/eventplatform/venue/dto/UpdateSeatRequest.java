package com.eventplatform.venue.dto;

import com.eventplatform.venue.domain.SeatType;

public record UpdateSeatRequest(
        SeatType seatType,
        Boolean enabled
) {
}
