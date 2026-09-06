package com.eventplatform.common.client;

import java.util.UUID;

public record EventSnapshot(
        UUID id,
        UUID organizerId,
        String title,
        String description,
        String city,
        String status,
        String categoryName
) {
    public boolean isPublished() {
        return "PUBLISHED".equalsIgnoreCase(status);
    }
}
