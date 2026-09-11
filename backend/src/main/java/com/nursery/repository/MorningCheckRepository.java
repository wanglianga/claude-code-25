package com.nursery.repository;

import com.nursery.entity.MorningCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MorningCheckRepository extends JpaRepository<MorningCheck, Long> {
    Optional<MorningCheck> findByChildIdAndCheckDate(Long childId, LocalDate checkDate);
    List<MorningCheck> findByCheckDateOrderByIdAsc(LocalDate date);
    List<MorningCheck> findByCheckDateAndChildClassroomIdOrderByIdAsc(LocalDate date, Long classroomId);
    List<MorningCheck> findByChildIdOrderByCheckDateDesc(Long childId);
    long countByChildIdAndCheckDateAfterAndResultNot(Long childId, LocalDate since, MorningCheck.Result result);
    long countByCheckDateAndResultNot(LocalDate date, MorningCheck.Result result);
}
