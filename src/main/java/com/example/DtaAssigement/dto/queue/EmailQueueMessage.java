package com.example.DtaAssigement.dto.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Email Queue Message DTO
 * Used for async email processing via Rqueue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueueMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String to;
    private String subject;
    private String body;
    private String otpCode;
    private String userName;
    private Long userId;
}
