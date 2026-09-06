package com.eventplatform.identity.web;

import com.eventplatform.identity.domain.Role;
import com.eventplatform.identity.domain.UserStatus;
import com.eventplatform.identity.dto.AdminUpdateUserRequest;
import com.eventplatform.identity.dto.UserResponse;
import com.eventplatform.identity.security.SecurityUtils;
import com.eventplatform.identity.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Page<UserResponse> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return adminUserService.listUsers(role, status, pageable);
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable UUID userId) {
        return adminUserService.getUser(userId);
    }

    @PatchMapping("/{userId}")
    public UserResponse update(@PathVariable UUID userId, @Valid @RequestBody AdminUpdateUserRequest request) {
        return adminUserService.updateUser(SecurityUtils.currentUserId(), userId, request);
    }
}
