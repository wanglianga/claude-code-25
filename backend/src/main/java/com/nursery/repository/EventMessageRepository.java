package com.nursery.repository;

import com.nursery.entity.EventMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventMessageRepository extends JpaRepository<EventMessage, Long> {
    List<EventMessage> findByEventIdOrderByCreatedAtAsc(Long eventId);
    List<EventMessage> findByEventChildIdOrderByCreatedAtAsc(Long childId);
}
