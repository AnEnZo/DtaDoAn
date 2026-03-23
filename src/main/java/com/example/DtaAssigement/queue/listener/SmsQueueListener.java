package com.example.DtaAssigement.queue.listener;

import com.example.DtaAssigement.dto.queue.SmsQueueMessage;
import com.vonage.client.VonageClient;
import com.vonage.client.sms.SmsSubmissionResponse;
import com.vonage.client.sms.SmsSubmissionResponseMessage;
import com.vonage.client.sms.messages.TextMessage;
import com.vonage.client.sms.MessageStatus;
import com.github.sonus21.rqueue.annotation.RqueueListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * SMS Queue Listener
 * Processes SMS messages from the queue asynchronously
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SmsQueueListener {

    @Value("${vonage.api-key}")
    private String apiKey;

    @Value("${vonage.api-secret}")
    private String apiSecret;

    private VonageClient vonageClient;

    @PostConstruct
    public void init() {
        this.vonageClient = VonageClient.builder()
                .apiKey(apiKey)
                .apiSecret(apiSecret)
                .build();
    }

    /**
     * Process SMS messages from the queue
     * Retry 5 times on failure, move to DLQ if all retries fail
     */
    @RqueueListener(value = "sms-queue", numRetries = "5", deadLetterQueue = "sms-dlq", concurrency = "3", visibilityTimeout = "30000" // 30
                                                                                                                                       // seconds
    )
    public void processSms(SmsQueueMessage message) {
        log.info("Processing SMS to: {}", message.getPhoneNumber());

        try {
            TextMessage textMessage = new TextMessage(
                    "Vonage APIs",
                    message.getPhoneNumber(),
                    message.getMessage());

            SmsSubmissionResponse response = vonageClient.getSmsClient().submitMessage(textMessage);
            SmsSubmissionResponseMessage responseMessage = response.getMessages().get(0);

            if (responseMessage.getStatus() == MessageStatus.OK) {
                log.info("✅ SMS sent successfully to: {}", message.getPhoneNumber());
            } else {
                log.error("❌ SMS failed: {}", responseMessage.getErrorText());
                throw new RuntimeException("SMS sending failed: " + responseMessage.getErrorText());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send SMS to: {}", message.getPhoneNumber(), e);
            throw e; // Trigger retry
        }
    }

    /**
     * Dead Letter Queue handler for failed SMS
     */
    @RqueueListener(value = "sms-dlq")
    public void handleFailedSms(SmsQueueMessage message) {
        log.error("📱 SMS permanently failed after retries. Phone: {}, Message: {}",
                message.getPhoneNumber(), message.getMessage());
        // TODO: Store failed SMS in database for manual review
    }
}
