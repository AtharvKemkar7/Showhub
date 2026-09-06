package com.eventplatform.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectEventRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
