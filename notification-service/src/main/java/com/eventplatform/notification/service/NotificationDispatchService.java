package com.eventplatform.notification.service;

import com.eventplatform.common.exception.ApiException;
import com.eventplatform.common.messaging.DomainEvent;
import com.eventplatform.common.security.AuthenticatedUser;
import com.eventplatform.notification.domain.Notification;
import com.eventplatform.notification.domain.NotificationStatus;
import com.eventplatform.notification.domain.NotificationTemplate;
import com.eventplatform.notification.dto.DispatchRequest;
import com.eventplatform.notification.dto.NotificationResponse;
import com.eventplatform.notification.repository.NotificationRepository;
import com.eventplatform.notification.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;

    public NotificationDispatchService(
            NotificationTemplateRepository templateRepository,
            NotificationRepository notificationRepository
    ) {
        this.templateRepository = templateRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public List<NotificationResponse> dispatch(DispatchRequest request) {
        List<NotificationTemplate> templates = new ArrayList<>();
        templateRepository.findByCode(request.eventType()).ifPresent(templates::add);
        templateRepository.findByCode(request.eventType() + ".inapp").ifPresent(templates::add);
        if (templates.isEmpty()) {
            throw ApiException.notFound("No template for event type " + request.eventType());
        }
        List<NotificationResponse> sent = new ArrayList<>();
        for (NotificationTemplate template : templates) {
            Notification notification = new Notification();
            notification.setUserId(request.userId());
            notification.setChannel(template.getChannel());
            notification.setTemplateCode(template.getCode());
            notification.setSubject(render(template.getSubject(), request));
            notification.setBody(render(template.getBody(), request));
            notification.setAggregateId(request.aggregateId());
            notification.setEventType(request.eventType());
            try {
                log.info("Sending {} notification template={} user={}", template.getChannel(), template.getCode(), request.userId());
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
            } catch (RuntimeException ex) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(ex.getMessage());
            }
            notificationRepository.save(notification);
            sent.add(NotificationResponse.from(notification));
        }
        return sent;
    }

    @Transactional
    public List<NotificationResponse> consume(DomainEvent event) {
        if (event.actorId() == null || !StringUtils.hasText(event.type())) {
            return List.of();
        }
        try {
            return dispatch(new DispatchRequest(event.actorId(), event.type(), event.aggregateId(), event.payloadJson()));
        } catch (ApiException ex) {
            log.warn("Skipping notification for type={}: {}", event.type(), ex.getMessage());
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> mine(AuthenticatedUser user, Pageable pageable) {
        UUID userId = user.isAdmin() ? user.getId() : user.getId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(NotificationResponse::from);
    }

    private String render(String template, DispatchRequest request) {
        String value = template;
        if (request.aggregateId() != null) {
            value = value.replace("{{aggregateId}}", request.aggregateId());
        }
        if (request.eventType() != null) {
            value = value.replace("{{eventType}}", request.eventType());
        }
        if (request.userId() != null) {
            value = value.replace("{{userId}}", request.userId().toString());
        }
        return value;
    }
}
