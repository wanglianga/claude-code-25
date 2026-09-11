package com.nursery.repository;

import com.nursery.entity.PickupMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PickupMessageRepository extends JpaRepository<PickupMessage, Long> {
    List<PickupMessage> findByChildIdAndMsgDateOrderByCreatedAtAsc(Long childId, LocalDate date);
    List<PickupMessage> findByChildIdOrderByCreatedAtDesc(Long childId);
}
