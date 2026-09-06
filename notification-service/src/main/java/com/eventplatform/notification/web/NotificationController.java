package com.eventplatform.notification.web;

import com.eventplatform.common.security.SecurityUtils;
import com.eventplatform.notification.dto.NotificationResponse;
import com.eventplatform.notification.service.NotificationDispatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationDispatchService notificationDispatchService;

    public NotificationController(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<NotificationResponse> mine(@PageableDefault(size = 20) Pageable pageable) {
        return notificationDispatchService.mine(SecurityUtils.currentUser(), pageable);
    }
}
