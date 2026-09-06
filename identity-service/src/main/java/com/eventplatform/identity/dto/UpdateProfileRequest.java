package com.eventplatform.identity.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30) @Pattern(regexp = "^$|^[+0-9][0-9\\-\\s]{6,28}$", message = "Invalid phone number") String phone
) {
}
