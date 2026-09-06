package com.eventplatform.venue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVenueRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 300) String address,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state
) {
}
