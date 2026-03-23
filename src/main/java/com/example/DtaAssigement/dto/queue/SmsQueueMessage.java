package com.example.DtaAssigement.dto.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * SMS Queue Message DTO
 * Used for async SMS sending via Rqueue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsQueueMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String phoneNumber;
    private String message;
    private String provider; // "TWILIO" or "VONAGE"
    private Integer retryCount;
}
