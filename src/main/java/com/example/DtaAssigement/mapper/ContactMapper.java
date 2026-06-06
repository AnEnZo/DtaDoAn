package com.example.DtaAssigement.mapper;

import com.example.DtaAssigement.dto.ContactResponse;
import com.example.DtaAssigement.entity.Contact;
import org.springframework.stereotype.Component;

/**
 * Mapper class for Contact Entity and DTO conversions.
 */
@Component
public class ContactMapper {

    public static ContactResponse toResponse(Contact contact) {
        if (contact == null) {
            return null;
        }

        return ContactResponse.builder()
                .id(contact.getId())
                .name(contact.getName())
                .email(contact.getEmail())
                .message(contact.getMessage())
                .status(contact.getStatus())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }
}
