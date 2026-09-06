package com.eventplatform.common.security;

import com.eventplatform.common.exception.ApiException;
import org.springframework.util.StringUtils;

public class InternalTokenValidator {

    private final String expected;

    public InternalTokenValidator(String expected) {
        this.expected = expected;
    }

    public void require(String provided) {
        if (!StringUtils.hasText(expected) || !expected.equals(provided)) {
            throw ApiException.unauthorized("Invalid internal token");
        }
    }
}
