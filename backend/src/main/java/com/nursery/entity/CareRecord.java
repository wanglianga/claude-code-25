package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 在园照护记录（午间照护/喂药/如厕/睡眠/活动伤情，进入同一条在园记录）
 */
@Getter
@Setter
@Entity
@Table(name = "care_records")
public class CareRecord {

    public enum CareType { NOON_CARE, MEDICATION, TOILET, SLEEP, INJURY }

    public enum Severity { NORMAL, ATTENTION, SERIOUS }

    public enum InjuryType { FALL, BUMP, SCRATCH_BITE, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CareType type;

    @Column(columnDefinition = "text")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity = Severity.NORMAL;

    /** 伤情类型（type=INJURY 时使用） */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private InjuryType injuryType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    private LocalDateTime createdAt = LocalDateTime.now();
}
