package com.example.DtaAssigement.queue.listener;

import com.example.DtaAssigement.dto.queue.EmailQueueMessage;
import com.github.sonus21.rqueue.annotation.RqueueListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Email Queue Listener
 * Processes email messages from the queue asynchronously
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailQueueListener {

    private final JavaMailSender mailSender;

    /**
     * Process email messages from the queue
     * Retry 3 times on failure, move to DLQ if all retries fail
     */
    @RqueueListener(value = "email-queue", numRetries = "3", deadLetterQueue = "email-dlq", concurrency = "5")
    public void processEmail(EmailQueueMessage message) {
        log.info("Processing email to: {}", message.getTo());

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(message.getTo());
            mailMessage.setSubject(message.getSubject());

            // Build email body
            StringBuilder body = new StringBuilder();
            if (message.getOtpCode() != null) {
                body.append("Your OTP Code: ").append(message.getOtpCode()).append("\n\n");
            }
            if (message.getBody() != null) {
                body.append(message.getBody());
            }

            mailMessage.setText(body.toString());

            mailSender.send(mailMessage);

            log.info("✅ Email sent successfully to: {}", message.getTo());
        } catch (Exception e) {
            log.error("❌ Failed to send email to: {}", message.getTo(), e);
            throw e; // Trigger retry
        }
    }

    /**
     * Dead Letter Queue handler for failed emails
     */
    @RqueueListener(value = "email-dlq")
    public void handleFailedEmails(EmailQueueMessage message) {
        log.error("📧 Email permanently failed after retries. To: {}, Subject: {}",
                message.getTo(), message.getSubject());
        // TODO: Store failed emails in database for manual review
    }
}
