package com.salaryprocessor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for Contentful integration
 * This sets up the Contentful Content Delivery API (CDA) client
 */
@Configuration
public class ContentfulConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ContentfulConfig.class);
    
    @Value("${contentful.space-id}")
    private String spaceId;
    
    @Value("${contentful.access-token}")
    private String accessToken;
    
    @Value("${contentful.environment:master}")
    private String environment;
    
    /**
     * Base URL for Contentful Content Delivery API
     */
    @Bean
    public String contentfulBaseUrl() {
        return String.format("https://cdn.contentful.com/spaces/%s/environments/%s", spaceId, environment);
    }
    
    /**
     * Access token for Contentful Content Delivery API
     */
    @Bean
    public String contentfulAccessToken() {
        return accessToken;
    }
    
    /**
     * RestTemplate for making HTTP requests to Contentful API
     */
    @Bean
    public RestTemplate restTemplate() {
        log.info("Initializing RestTemplate for Contentful API access, space ID: {}", spaceId);
        return new RestTemplate();
    }
}
