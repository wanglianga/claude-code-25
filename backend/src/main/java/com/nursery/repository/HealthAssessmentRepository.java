package com.nursery.repository;

import com.nursery.entity.HealthAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthAssessmentRepository extends JpaRepository<HealthAssessment, Long> {
    List<HealthAssessment> findByApplicationChildIdOrderByCreatedAtDesc(Long childId);
}
