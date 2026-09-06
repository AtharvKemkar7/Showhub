package com.eventplatform.venue.dto;

import jakarta.validation.constraints.Size;

public record UpdateVenueRequest(
        @Size(max = 200) String name,
        @Size(max = 300) String address,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        Boolean active
) {
}
