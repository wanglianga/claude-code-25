package com.nursery.repository;

import com.nursery.entity.AnomalyAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnomalyAlertRepository extends JpaRepository<AnomalyAlert, Long> {
    List<AnomalyAlert> findByStatusOrderByCreatedAtDesc(AnomalyAlert.Status status);
    List<AnomalyAlert> findAllByOrderByCreatedAtDesc();
    List<AnomalyAlert> findByChildIdOrderByCreatedAtDesc(Long childId);
    boolean existsByChildIdAndStatus(Long childId, AnomalyAlert.Status status);
    long countByStatus(AnomalyAlert.Status status);
}
