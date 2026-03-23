package com.example.DtaAssigement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WebClientConfig {

    @Value("${llama.api.connect-timeout:60}")
    private int llamaConnectTimeout;

    @Value("${llama.api.read-timeout:120}")
    private int llamaReadTimeout;

    @Bean
    @Primary
    public org.springframework.web.client.RestTemplate restTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) java.time.Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) java.time.Duration.ofSeconds(30).toMillis());

        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate(
                factory);
        restTemplate.setInterceptors(java.util.Collections.singletonList((request, body, execution) -> {
            System.out.println("Request: " + request.getMethod() + " " + request.getURI());
            return execution.execute(request, body);
        }));
        return restTemplate;
    }

    /**
     * RestTemplate with longer timeout for Llama AI API calls
     * Default: 60s connect timeout, 120s read timeout
     */
    @Bean("llamaRestTemplate")
    public org.springframework.web.client.RestTemplate llamaRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) java.time.Duration.ofSeconds(llamaConnectTimeout).toMillis());
        factory.setReadTimeout((int) java.time.Duration.ofSeconds(llamaReadTimeout).toMillis());

        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate(
                factory);
        restTemplate.setInterceptors(java.util.Collections.singletonList((request, body, execution) -> {
            System.out.println("Llama API Request: " + request.getMethod() + " " + request.getURI());
            return execution.execute(request, body);
        }));
        return restTemplate;
    }
}
