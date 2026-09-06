package com.eventplatform.search.web;

import com.eventplatform.common.messaging.DomainEvent;
import com.eventplatform.common.security.InternalTokenValidator;
import com.eventplatform.search.dto.IndexEventRequest;
import com.eventplatform.search.dto.SearchEventResponse;
import com.eventplatform.search.service.EventIndexService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalSearchController {

    private final EventIndexService eventIndexService;
    private final InternalTokenValidator internalTokenValidator;

    public InternalSearchController(EventIndexService eventIndexService, InternalTokenValidator internalTokenValidator) {
        this.eventIndexService = eventIndexService;
        this.internalTokenValidator = internalTokenValidator;
    }

    @PostMapping("/index")
    public SearchEventResponse index(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody IndexEventRequest request
    ) {
        internalTokenValidator.require(internalToken);
        return eventIndexService.index(request);
    }

    @PostMapping("/events")
    public void consume(
            @RequestHeader("X-Internal-Token") String internalToken,
            @Valid @RequestBody DomainEvent event
    ) {
        internalTokenValidator.require(internalToken);
        eventIndexService.consume(event);
    }
}
