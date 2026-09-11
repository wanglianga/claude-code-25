package com.nursery.repository;

import com.nursery.entity.MealSubstitution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MealSubstitutionRepository extends JpaRepository<MealSubstitution, Long> {
    List<MealSubstitution> findByMealDateOrderByCreatedAtDesc(LocalDate mealDate);
    List<MealSubstitution> findByChildIdAndMealDate(Long childId, LocalDate mealDate);
    List<MealSubstitution> findByChildIdOrderByCreatedAtDesc(Long childId);
    List<MealSubstitution> findByStatusOrderByCreatedAtDesc(MealSubstitution.Status status);
    List<MealSubstitution> findByChildParentIdOrderByCreatedAtDesc(Long parentId);
    long countByMealDate(LocalDate mealDate);
    long countByMealDateAndStatus(LocalDate mealDate, MealSubstitution.Status status);
    long countByStatus(MealSubstitution.Status status);
}
