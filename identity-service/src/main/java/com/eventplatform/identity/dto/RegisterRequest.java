package com.eventplatform.identity.dto;

import com.eventplatform.identity.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 30) @Pattern(regexp = "^$|^[+0-9][0-9\\-\\s]{6,28}$", message = "Invalid phone number") String phone,
        @NotNull Role role
) {
}
