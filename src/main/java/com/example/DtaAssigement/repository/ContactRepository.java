package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Contact entities.
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /**
     * Retrieve all contacts ordered by their creation time descending.
     *
     * @param pageable pagination details
     * @return Page of contacts
     */
    Page<Contact> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Mark all unread contact messages as READ.
     *
     * @return Number of updated rows
     */
    @Modifying
    @Query("UPDATE Contact c SET c.status = com.example.DtaAssigement.ennum.ContactStatus.READ WHERE c.status = com.example.DtaAssigement.ennum.ContactStatus.UNREAD")
    int markAllAsRead();
}
