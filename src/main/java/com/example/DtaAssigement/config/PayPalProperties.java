package com.example.DtaAssigement.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "paypal")
@Validated
@Data
public class PayPalProperties {

    @NotNull
    private String mode; // sandbox or live

    @NotNull
    private String clientId;

    @NotNull
    private String clientSecret;

    @NotNull
    private String apiBaseUrl;

    private String apiVersion = "v2";

    private String currency = "USD";

    private String webhookId;

    @NotNull
    private String returnSuccessUrl;

    @NotNull
    private String returnCancelUrl;

    private Integer connectionTimeout = 30000;

    private Integer readTimeout = 30000;

    private Boolean loggingEnabled = true;
}
