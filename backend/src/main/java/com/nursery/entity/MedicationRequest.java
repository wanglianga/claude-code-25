package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家长喂药委托
 */
@Getter
@Setter
@Entity
@Table(name = "medication_requests")
public class MedicationRequest {

    public enum Status { ACTIVE, FINISHED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(nullable = false, length = 128)
    private String medicineName;

    /** 剂量，如 "5ml" */
    @Column(nullable = false, length = 64)
    private String dosage;

    /** 服药时间计划，如 "每日午饭后" */
    @Column(length = 128)
    private String timePlan;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(columnDefinition = "text")
    private String parentNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    private LocalDateTime createdAt = LocalDateTime.now();
}
