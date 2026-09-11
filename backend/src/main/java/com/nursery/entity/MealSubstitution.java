package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 过敏餐临时替换单：厨房因食材缺货或菜单含儿童过敏源发起，
 * 平台比对儿童档案/替代食材/营养要求后，依次由保健老师 → 班级老师 → 家长确认；
 * 家长确认后厨房出餐、老师分餐并同步当日饮食记录；
 * 家长未确认（或任一环节拒绝）时，儿童进入人工照护提示，避免误食。
 */
@Getter
@Setter
@Entity
@Table(name = "meal_substitutions")
public class MealSubstitution {

    public enum Reason { INGREDIENT_SHORTAGE, ALLERGEN_RISK }

    public enum Status {
        PENDING_HEALTH,     // 待保健老师确认
        PENDING_TEACHER,    // 待班级老师确认
        PENDING_PARENT,     // 待家长确认（分餐页进入人工照护提示）
        CONFIRMED,          // 家长已确认，待厨房出餐
        EXECUTED,           // 厨房已出餐，待班级分餐
        SERVED,             // 已分餐（终态）
        REJECTED,           // 任一环节拒绝（终态，人工照护）
        CANCELLED           // 厨房取消（终态）
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "menu_id")
    private DailyMenu menu;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    /** 冗余餐次日期，便于分餐页按天查询 */
    @Column(nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Reason reason;

    /** 触发说明，如「花生酱缺货」「今日午餐含花生酱」 */
    @Column(columnDefinition = "text")
    private String triggerDetail;

    /** 原菜品 */
    @Column(nullable = false, length = 200)
    private String originalDish;

    /** 替代菜品 */
    @Column(nullable = false, length = 200)
    private String substituteDish;

    /** 替代食材（平台核验不含过敏源） */
    @Column(columnDefinition = "text")
    private String substituteIngredients;

    /** 平台比对命中的过敏源（缺货触发时为空） */
    @Column(length = 100)
    private String matchedAllergy;

    /** 平台生成的营养/档案比对说明 */
    @Column(columnDefinition = "text")
    private String nutritionCheck;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING_HEALTH;

    // ---------- 保健老师确认 ----------
    @ManyToOne
    @JoinColumn(name = "health_confirmed_by")
    private User healthConfirmedBy;

    private LocalDateTime healthConfirmedAt;

    @Column(columnDefinition = "text")
    private String healthNote;

    // ---------- 班级老师确认 ----------
    @ManyToOne
    @JoinColumn(name = "teacher_confirmed_by")
    private User teacherConfirmedBy;

    private LocalDateTime teacherConfirmedAt;

    @Column(columnDefinition = "text")
    private String teacherNote;

    // ---------- 家长确认 ----------
    private LocalDateTime parentConfirmedAt;

    @Column(columnDefinition = "text")
    private String parentNote;

    // ---------- 厨房执行 ----------
    @ManyToOne
    @JoinColumn(name = "kitchen_executed_by")
    private User kitchenExecutedBy;

    private LocalDateTime kitchenExecutedAt;

    // ---------- 班级分餐 ----------
    @ManyToOne
    @JoinColumn(name = "served_by")
    private User servedBy;

    private LocalDateTime servedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
