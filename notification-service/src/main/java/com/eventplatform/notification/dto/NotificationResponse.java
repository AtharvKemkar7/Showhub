package com.eventplatform.notification.dto;

import com.eventplatform.notification.domain.Notification;
import com.eventplatform.notification.domain.NotificationChannel;
import com.eventplatform.notification.domain.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationChannel channel,
        String templateCode,
        String subject,
        String body,
        NotificationStatus status,
        String aggregateId,
        String eventType,
        Instant createdAt,
        Instant sentAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getChannel(),
                notification.getTemplateCode(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus(),
                notification.getAggregateId(),
                notification.getEventType(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }
}
