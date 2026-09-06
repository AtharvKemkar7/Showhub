package com.eventplatform.venue.dto;

import com.eventplatform.venue.domain.Hall;

import java.util.List;
import java.util.UUID;

public record HallResponse(
        UUID id,
        UUID venueId,
        String name,
        int capacity,
        int seatCount,
        List<SeatResponse> seats
) {
    public static HallResponse from(Hall hall, List<SeatResponse> seats) {
        return new HallResponse(
                hall.getId(),
                hall.getVenue().getId(),
                hall.getName(),
                hall.getCapacity(),
                seats.size(),
                seats
        );
    }
}
