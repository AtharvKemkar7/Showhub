package com.eventplatform.show.dto;

import com.eventplatform.show.domain.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SeatPriceRequest(
        @NotNull SeatType seatType,
        @NotNull @Min(0) Integer amountCents
) {
}
