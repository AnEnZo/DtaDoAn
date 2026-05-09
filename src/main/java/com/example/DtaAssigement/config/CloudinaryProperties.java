package com.example.DtaAssigement.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "cloudinary")
@Validated
@Data
public class CloudinaryProperties {

    @NotBlank(message = "Cloud name is required")
    private String cloudName;

    @NotBlank(message = "API key is required")
    private String apiKey;

    @NotBlank(message = "API secret is required")
    private String apiSecret;
}
