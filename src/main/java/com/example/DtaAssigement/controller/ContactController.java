package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.ContactRequest;
import com.example.DtaAssigement.dto.ContactResponse;
import com.example.DtaAssigement.service.ContactService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing customer contact form submissions and administration.
 */
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ContactController {

    private final ContactService contactService;

    /**
     * Public endpoint to submit a contact message.
     *
     * @param request Contact form details
     * @return Saved contact response
     */
    @PostMapping
    public ResponseEntity<ContactResponse> submitContact(@Valid @RequestBody ContactRequest request) {
        log.info("POST /api/contacts - Name: {}, Email: {}", request.getName(), request.getEmail());
        ContactResponse response = contactService.submitContact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Admin endpoint to list all contact messages with pagination.
     *
     * @param page page index (0-based)
     * @param size page size
     * @param sortBy property to sort by
     * @param sortDir sorting direction (asc/desc)
     * @return Page of contact responses
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ContactResponse>> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("GET /api/contacts - Fetching contacts page: {}, size: {}, sort: {} {}", page, size, sortBy, sortDir);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ContactResponse> contacts = contactService.getAllContacts(pageable);
        return ResponseEntity.ok(contacts);
    }

    /**
     * Admin endpoint to get a single contact message detail.
     *
     * @param id Contact ID
     * @return Contact details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContactResponse> getContactById(@PathVariable Long id) {
        log.info("GET /api/contacts/{} - Fetching contact details", id);
        ContactResponse response = contactService.getContactById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to update status of a contact message (e.g. mark as READ).
     *
     * @param id     Contact ID
     * @param status New status string
     * @return Updated contact response
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContactResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        log.info("PUT /api/contacts/{}/status - Updating status to {}", id, status);
        ContactResponse response = contactService.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to mark all unread contact messages as READ.
     *
     * @return No content
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAllAsRead() {
        log.info("PUT /api/contacts/read-all - Marking all contacts as read");
        contactService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    /**
     * Admin endpoint to delete a contact message.
     *
     * @param id Contact ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        log.info("DELETE /api/contacts/{} - Deleting contact", id);
        contactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}
