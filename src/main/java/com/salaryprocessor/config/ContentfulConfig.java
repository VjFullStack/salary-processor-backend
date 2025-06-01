package com.salaryprocessor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for Contentful - currently disabled and using hardcoded values instead
 */
@Configuration
public class ContentfulConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ContentfulConfig.class);
    
    public ContentfulConfig() {
        log.info("Contentful integration is disabled. Using hardcoded 50,000 INR salary for all employees.");
    }
}
