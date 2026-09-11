package com.nursery.repository;

import com.nursery.entity.EnrollmentApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EnrollmentApplicationRepository extends JpaRepository<EnrollmentApplication, Long> {
    List<EnrollmentApplication> findByStatusOrderByCreatedAtAsc(EnrollmentApplication.Status status);
    List<EnrollmentApplication> findAllByOrderByCreatedAtDesc();
    List<EnrollmentApplication> findByChildParentIdOrderByCreatedAtDesc(Long parentId);
    List<EnrollmentApplication> findByChildIdOrderByCreatedAtDesc(Long childId);
    boolean existsByChildIdAndStatusIn(Long childId, Collection<EnrollmentApplication.Status> statuses);
    long countByStatus(EnrollmentApplication.Status status);
}
