package com.eventplatform.identity.dto;

import com.eventplatform.identity.domain.Role;
import com.eventplatform.identity.domain.UserStatus;

public record AdminUpdateUserRequest(
        Role role,
        UserStatus status
) {
}
