package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日晨检记录
 */
@Getter
@Setter
@Entity
@Table(name = "morning_checks", uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "check_date"}))
public class MorningCheck {

    public enum Result {
        /** 正常入班 */
        ENTER_CLASS,
        /** 隔离观察 */
        ISOLATION,
        /** 通知家长接回 */
        PARENT_PICKUP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(nullable = false)
    private LocalDate checkDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    /** 体温（℃） */
    @Column(precision = 3, scale = 1)
    private BigDecimal temperature;

    /** 皮肤情况：正常/皮疹/其他 */
    @Column(length = 64)
    private String skinStatus;

    /** 情绪：正常/低落/烦躁/哭闹 */
    @Column(length = 64)
    private String mood;

    /** 饮食（早餐进食情况） */
    @Column(length = 64)
    private String diet;

    /** 携带物品 */
    @Column(columnDefinition = "text")
    private String carriedItems;

    /** 药品是否已带（有喂药委托的儿童需要核对） */
    private Boolean medicationBrought;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Result result = Result.ENTER_CLASS;

    @Column(columnDefinition = "text")
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
