package com.salaryprocessor.controller;

import com.salaryprocessor.service.ContentfulService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling webhooks from Contentful
 */
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private ContentfulService contentfulService;

    @Value("${contentful.webhook.secret:default_webhook_secret}")
    private String webhookSecret;

    /**
     * Handle webhooks from Contentful when content changes
     * The X-Contentful-Webhook-Name header contains the name of the webhook
     * The X-Contentful-Topic header contains the event type (e.g., ContentManagement.Entry.publish)
     * The X-Contentful-Webhook-Secret header contains the webhook secret for validation
     *
     * @param webhookName The name of the webhook
     * @param topic The type of event
     * @param secretHeader The webhook secret for validation
     * @param payload The webhook payload
     * @return A response indicating success or failure
     */
    @PostMapping("/contentful")
    public ResponseEntity<Map<String, Object>> handleContentfulWebhook(
            @RequestHeader(value = "X-Contentful-Webhook-Name", required = false) String webhookName,
            @RequestHeader(value = "X-Contentful-Topic", required = false) String topic,
            @RequestHeader(value = "X-Contentful-Webhook-Secret", required = false) String secretHeader,
            @RequestBody Map<String, Object> payload) {

        log.info("Received webhook from Contentful: {}, topic: {}", webhookName, topic);

        // Validate the webhook secret
        if (secretHeader == null || !secretHeader.equals(webhookSecret)) {
            log.warn("Invalid webhook secret received");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> response = new HashMap<>();

        // Check if this is an employee entry update
        if (topic != null && 
           (topic.contains("Entry.publish") || 
            topic.contains("Entry.create") || 
            topic.contains("Entry.update") || 
            topic.contains("Entry.delete"))) {
            
            // Refresh employee data from Contentful
            log.info("Refreshing employee data due to Contentful content change");
            contentfulService.refreshEmployeeData();
            
            response.put("status", "success");
            response.put("message", "Employee data refreshed successfully");
        } else {
            response.put("status", "ignored");
            response.put("message", "Event not relevant for employee data refresh");
        }

        return ResponseEntity.ok(response);
    }
}
