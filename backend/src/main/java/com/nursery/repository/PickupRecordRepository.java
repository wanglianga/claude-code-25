package com.nursery.repository;

import com.nursery.entity.PickupRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PickupRecordRepository extends JpaRepository<PickupRecord, Long> {
    List<PickupRecord> findByPickupDateOrderByCreatedAtAsc(LocalDate date);
    List<PickupRecord> findByChildIdOrderByCreatedAtDesc(Long childId);
    List<PickupRecord> findByChildIdAndPickupDate(Long childId, LocalDate date);
    long countByPickupDate(LocalDate date);
    long countByPickupDateAndResult(LocalDate date, PickupRecord.Result result);
}
