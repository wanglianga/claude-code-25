package com.nursery.repository;

import com.nursery.entity.MedicationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRequestRepository extends JpaRepository<MedicationRequest, Long> {
    List<MedicationRequest> findByChildIdOrderByCreatedAtDesc(Long childId);
    List<MedicationRequest> findByChildIdAndStatus(Long childId, MedicationRequest.Status status);
}
