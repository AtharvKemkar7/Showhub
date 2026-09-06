package com.eventplatform.venue.dto;

import com.eventplatform.venue.domain.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSeatRequest(
        @NotBlank @Size(max = 10) String rowLabel,
        @NotNull @Min(1) Integer seatNumber,
        @NotNull SeatType seatType
) {
}
