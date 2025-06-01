package com.salaryprocessor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.contentful.java.cda.CDAClient;

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
     * Create and configure Contentful CDA client
     * @return Configured CDAClient
     */
    @Bean
    public CDAClient contentfulClient() {
        log.info("Initializing Contentful client for space ID: {}", spaceId);
        return CDAClient.builder()
                .setSpace(spaceId)
                .setToken(accessToken)
                .setEnvironment(environment)
                .build();
    }
}
