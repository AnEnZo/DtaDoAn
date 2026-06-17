package com.example.DtaAssigement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Frontend-related configuration (e.g. the base URL the backend redirects to
 * after MoMo/PayPal/OAuth flows). Externalized so it is not hardcoded across
 * controllers and handlers and can differ per environment.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class FrontendProperties {

    /** Base URL of the SPA, e.g. http://localhost:3000 */
    private String frontendBaseUrl = "http://localhost:3000";
}
