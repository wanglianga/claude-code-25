package com.nursery.repository;

import com.nursery.entity.NurseryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NurseryEventRepository extends JpaRepository<NurseryEvent, Long> {
    List<NurseryEvent> findAllByOrderByCreatedAtDesc();
    List<NurseryEvent> findByStatusOrderByCreatedAtDesc(NurseryEvent.Status status);
    List<NurseryEvent> findByTypeOrderByCreatedAtDesc(NurseryEvent.EventType type);
    List<NurseryEvent> findByStatusAndTypeOrderByCreatedAtDesc(NurseryEvent.Status status, NurseryEvent.EventType type);
    List<NurseryEvent> findByChildIdOrderByCreatedAtDesc(Long childId);
    List<NurseryEvent> findByChildIdAndCreatedAtBetweenOrderByCreatedAtAsc(Long childId, LocalDateTime from, LocalDateTime to);
    long countByChildIdAndCreatedAtAfter(Long childId, LocalDateTime since);
    long countByStatus(NurseryEvent.Status status);
}
