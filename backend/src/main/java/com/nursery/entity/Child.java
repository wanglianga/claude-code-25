package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "children")
public class Child {

    public enum Status { PENDING, ASSESSING, ASSIGNED, ENROLLED, SUSPENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent_id")
    private User parent;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 8)
    private String gender;

    private LocalDate birthDate;

    /** 过敏史 */
    @Column(columnDefinition = "text")
    private String allergyHistory;

    /** 疫苗接种情况 */
    @Column(columnDefinition = "text")
    private String vaccinationStatus;

    /** 午睡习惯 */
    @Column(columnDefinition = "text")
    private String napHabit;

    /** 如厕能力 */
    @Column(columnDefinition = "text")
    private String toiletAbility;

    /** 常用药 */
    @Column(columnDefinition = "text")
    private String medications;

    /** 特殊照护需求 */
    @Column(columnDefinition = "text")
    private String specialCareNeeds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    /** 健康评估结论：是否需要过敏餐 */
    private Boolean allergyMealRequired = false;

    /** 健康评估结论：是否需要保健观察 */
    private Boolean healthObservation = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
