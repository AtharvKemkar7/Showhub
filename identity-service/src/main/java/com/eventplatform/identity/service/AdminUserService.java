package com.eventplatform.identity.service;

import com.eventplatform.identity.domain.Role;
import com.eventplatform.identity.domain.User;
import com.eventplatform.identity.domain.UserStatus;
import com.eventplatform.identity.dto.AdminUpdateUserRequest;
import com.eventplatform.identity.dto.UserResponse;
import com.eventplatform.identity.exception.ApiException;
import com.eventplatform.identity.repository.RefreshTokenRepository;
import com.eventplatform.identity.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AdminUserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Role role, UserStatus status, Pageable pageable) {
        Page<User> page;
        if (role != null && status != null) {
            page = userRepository.findByRoleAndStatus(role, status, pageable);
        } else if (role != null) {
            page = userRepository.findByRole(role, pageable);
        } else if (status != null) {
            page = userRepository.findByStatus(status, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return page.map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        return UserResponse.from(requireUser(userId));
    }

    @Transactional
    public UserResponse updateUser(UUID adminId, UUID userId, AdminUpdateUserRequest request) {
        if (request.role() == null && request.status() == null) {
            throw ApiException.badRequest("At least one of role or status must be provided");
        }
        User user = requireUser(userId);
        if (request.role() != null) {
            if (user.getId().equals(adminId) && request.role() != Role.ADMIN) {
                throw ApiException.forbidden("Administrators cannot remove their own admin role");
            }
            user.setRole(request.role());
        }
        if (request.status() != null) {
            if (user.getId().equals(adminId) && request.status() != UserStatus.ACTIVE) {
                throw ApiException.forbidden("Administrators cannot deactivate their own account");
            }
            user.setStatus(request.status());
            if (request.status() != UserStatus.ACTIVE) {
                refreshTokenRepository.revokeAllActiveForUser(user);
            }
        }
        return UserResponse.from(user);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
