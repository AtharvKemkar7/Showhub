package com.eventplatform.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityBeansConfig {

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(JwtProperties properties) {
        return new JwtService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        return new JwtAuthenticationFilter(jwtService, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestAccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public InternalTokenValidator internalTokenValidator(
            @Value("${app.internal-token:dev-internal-token}") String internalToken
    ) {
        return new InternalTokenValidator(internalToken);
    }
}
