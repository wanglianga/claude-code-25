package com.nursery.repository;

import com.nursery.entity.PickupAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickupAuthorizationRepository extends JpaRepository<PickupAuthorization, Long> {
    List<PickupAuthorization> findByChildIdAndActiveTrue(Long childId);
    List<PickupAuthorization> findByChildId(Long childId);
}
