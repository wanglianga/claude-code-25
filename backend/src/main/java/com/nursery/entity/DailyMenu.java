package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 厨房每日菜单：菜品、主要食材与营养说明。
 * 食材用于平台比对儿童过敏档案，触发过敏餐临时替换。
 */
@Getter
@Setter
@Entity
@Table(name = "daily_menus", uniqueConstraints = @UniqueConstraint(columnNames = {"menu_date", "meal_type"}))
public class DailyMenu {

    public enum MealType { LUNCH, SNACK }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate menuDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MealType mealType = MealType.LUNCH;

    /** 菜品（顿号/逗号分隔展示） */
    @Column(nullable = false, columnDefinition = "text")
    private String dishes;

    /** 主要食材（用于过敏源比对） */
    @Column(columnDefinition = "text")
    private String ingredients;

    /** 营养说明 */
    @Column(columnDefinition = "text")
    private String nutritionNotes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
