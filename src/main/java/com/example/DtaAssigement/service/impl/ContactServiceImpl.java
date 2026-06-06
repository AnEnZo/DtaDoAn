package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.dto.ContactRequest;
import com.example.DtaAssigement.dto.ContactResponse;
import com.example.DtaAssigement.entity.Contact;
import com.example.DtaAssigement.ennum.ContactStatus;
import com.example.DtaAssigement.mapper.ContactMapper;
import com.example.DtaAssigement.repository.ContactRepository;
import com.example.DtaAssigement.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of ContactService interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    @Override
    @Transactional
    public ContactResponse submitContact(ContactRequest request) {
        log.info("Submitting contact message from name: {}, email: {}", request.getName(), request.getEmail());

        Contact contact = Contact.builder()
                .name(request.getName())
                .email(request.getEmail())
                .message(request.getMessage())
                .status(ContactStatus.UNREAD)
                .build();

        Contact savedContact = contactRepository.save(contact);
        log.info("Saved contact message with ID: {}", savedContact.getId());
        return ContactMapper.toResponse(savedContact);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactResponse> getAllContacts(Pageable pageable) {
        log.info("Fetching all contact messages with pagination: {}", pageable);
        return contactRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ContactMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ContactResponse getContactById(Long id) {
        log.info("Fetching contact message with ID: {}", id);
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên hệ với ID: " + id));
        return ContactMapper.toResponse(contact);
    }

    @Override
    @Transactional
    public ContactResponse updateStatus(Long id, String statusStr) {
        log.info("Updating contact status for ID: {} to {}", id, statusStr);
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên hệ với ID: " + id));

        try {
            ContactStatus status = ContactStatus.valueOf(statusStr.trim().toUpperCase());
            contact.setStatus(status);
            Contact updatedContact = contactRepository.save(contact);
            log.info("Contact ID: {} status updated to {}", id, updatedContact.getStatus());
            return ContactMapper.toResponse(updatedContact);
        } catch (IllegalArgumentException e) {
            log.error("Invalid contact status: {}", statusStr);
            throw new RuntimeException("Trạng thái không hợp lệ: " + statusStr);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        log.info("Marking all unread contact messages as READ");
        contactRepository.markAllAsRead();
    }

    @Override
    @Transactional
    public void deleteContact(Long id) {
        log.info("Deleting contact message with ID: {}", id);
        if (!contactRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy liên hệ với ID: " + id);
        }
        contactRepository.deleteById(id);
        log.info("Deleted contact message with ID: {}", id);
    }
}
