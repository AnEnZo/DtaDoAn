package com.example.DtaAssigement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Component to log Swagger UI URL when application starts
 */
@Component
@Slf4j
public class SwaggerUrlLogger {

    @Value("${server.port:8080}")
    private String serverPort;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void logSwaggerUrl() {
        try {
            // Wait for all initialization to complete
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String swaggerUrl = "http://localhost:" + serverPort + "/swagger-ui/index.html";
        String apiDocsUrl = "http://localhost:" + serverPort + "/v3/api-docs";

        log.info("\n\n" +
                "=============================================================\n" +
                "🚀 Application started successfully!\n" +
                "=============================================================\n" +
                "📚 Swagger UI: {}\n" +
                "📄 API Docs:   {}\n" +
                "=============================================================\n",
                swaggerUrl, apiDocsUrl);
    }
}
