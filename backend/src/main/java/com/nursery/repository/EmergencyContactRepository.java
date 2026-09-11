package com.nursery.repository;

import com.nursery.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {
    List<EmergencyContact> findByChildIdOrderByPriorityAsc(Long childId);
}
