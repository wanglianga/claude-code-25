package com.nursery.repository;

import com.nursery.entity.CareRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CareRecordRepository extends JpaRepository<CareRecord, Long> {
    List<CareRecord> findByChildIdAndRecordDateOrderByCreatedAtAsc(Long childId, LocalDate date);
    List<CareRecord> findByChildIdOrderByRecordDateDescCreatedAtDesc(Long childId);
    long countByChildIdAndRecordDateAfterAndSeverityNot(Long childId, LocalDate since, CareRecord.Severity severity);
}
