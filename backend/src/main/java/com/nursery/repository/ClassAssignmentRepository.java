package com.nursery.repository;

import com.nursery.entity.ClassAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassAssignmentRepository extends JpaRepository<ClassAssignment, Long> {
    List<ClassAssignment> findByApplicationChildIdOrderByCreatedAtDesc(Long childId);
}
