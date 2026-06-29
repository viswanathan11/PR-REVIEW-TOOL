package com.example.BackendApplication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.BackendApplication.dto.WebhookPayloadDTO;
import com.example.BackendApplication.service.WebhookValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookValidator validator;
    private final ObjectMapper objectMapper;

    private static final List<String> TRIGGER_ACTIONS =
        List.of("opened", "synchronize", "reopened");

    // Explicit constructor injection (No Lombok)
    public WebhookController(WebhookValidator validator, ObjectMapper objectMapper) {
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/github")
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String sig,
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String event) {

        // Validate the signature using our HMAC SHA256 WebhookValidator
        if (!validator.isValid(rawBody, sig)) {
            log.warn("Invalid webhook signature rejected");
            return ResponseEntity.status(401).build();
        }

        // Only process pull request events
        if (!"pull_request".equals(event)) {
            log.info("Ignored non-PR event: {}", event);
            return ResponseEntity.ok().build();
        }

        try {
            WebhookPayloadDTO payload = objectMapper.readValue(rawBody, WebhookPayloadDTO.class);
            log.info("Webhook received: action={} PR={}",
                payload.getAction(),
                payload.getPullRequest().getNumber());

            if (TRIGGER_ACTIONS.contains(payload.getAction())) {
                log.info("Trigger action matched! Ready to trigger review for PR #{} ({})", 
                    payload.getPullRequest().getNumber(), 
                    payload.getPullRequest().getHtmlUrl());
                
                // TODO: Enqueue review in Phase 6: reviewJobService.enqueueReview(payload);
            }
        } catch (Exception e) {
            log.error("Failed to parse webhook payload: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
