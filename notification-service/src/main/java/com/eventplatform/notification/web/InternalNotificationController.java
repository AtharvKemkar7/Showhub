package com.eventplatform.notification.web;

import com.eventplatform.common.messaging.DomainEvent;
import com.eventplatform.common.security.InternalTokenValidator;
import com.eventplatform.notification.dto.DispatchRequest;
import com.eventplatform.notification.dto.NotificationResponse;
import com.eventplatform.notification.service.NotificationDispatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalNotificationController {

    private final NotificationDispatchService notificationDispatchService;
    private final InternalTokenValidator internalTokenValidator;

    public InternalNotificationController(
            NotificationDispatchService notificationDispatchService,
            InternalTokenValidator internalTokenValidator
    ) {
        this.notificationDispatchService = notificationDispatchService;
        this.internalTokenValidator = internalTokenValidator;
    }

    @PostMapping("/notifications")
    public List<NotificationResponse> dispatch(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody DispatchRequest request
    ) {
        internalTokenValidator.require(internalToken);
        return notificationDispatchService.dispatch(request);
    }

    @PostMapping("/events")
    public List<NotificationResponse> consume(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody DomainEvent event
    ) {
        internalTokenValidator.require(internalToken);
        return notificationDispatchService.consume(event);
    }
}
