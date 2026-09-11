package com.nursery.repository;

import com.nursery.entity.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {
    List<EventParticipant> findByEventIdOrderByJoinedAtAsc(Long eventId);
    List<EventParticipant> findByUserId(Long userId);
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
}
