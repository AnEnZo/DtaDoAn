package com.example.DtaAssigement.service;

import com.example.DtaAssigement.dto.ContactRequest;
import com.example.DtaAssigement.dto.ContactResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for handling contact operations.
 */
public interface ContactService {

    /**
     * Submit a contact message.
     *
     * @param request The contact request payload
     * @return The saved contact message response
     */
    ContactResponse submitContact(ContactRequest request);

    /**
     * Get all contact messages with pagination.
     *
     * @param pageable pagination details
     * @return Page of contact responses
     */
    Page<ContactResponse> getAllContacts(Pageable pageable);

    /**
     * Find a contact message by ID.
     *
     * @param id The contact ID
     * @return The contact response
     */
    ContactResponse getContactById(Long id);

    /**
     * Update the status of a contact message (e.g. mark as READ).
     *
     * @param id     The contact ID
     * @param status The new status (READ, UNREAD)
     * @return The updated contact response
     */
    ContactResponse updateStatus(Long id, String status);

    /**
     * Mark all unread contact messages as READ.
     */
    void markAllAsRead();

    /**
     * Delete a contact message by ID.
     *
     * @param id The contact ID
     */
    void deleteContact(Long id);
}
