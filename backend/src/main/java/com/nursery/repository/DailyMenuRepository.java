package com.nursery.repository;

import com.nursery.entity.DailyMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyMenuRepository extends JpaRepository<DailyMenu, Long> {
    Optional<DailyMenu> findByMenuDateAndMealType(LocalDate menuDate, DailyMenu.MealType mealType);
    List<DailyMenu> findByMenuDateOrderByMealTypeAsc(LocalDate menuDate);
    long countByMenuDate(LocalDate menuDate);
}
