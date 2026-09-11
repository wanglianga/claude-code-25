package com.nursery.repository;

import com.nursery.entity.TempDelegation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TempDelegationRepository extends JpaRepository<TempDelegation, Long> {
    List<TempDelegation> findByChildIdOrderByValidDateDesc(Long childId);
    List<TempDelegation> findByChildIdAndValidDateAndStatus(Long childId, LocalDate date, TempDelegation.Status status);
    List<TempDelegation> findByValidDateOrderByIdAsc(LocalDate date);
}
