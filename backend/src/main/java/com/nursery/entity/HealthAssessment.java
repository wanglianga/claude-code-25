package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 保健老师入托前健康评估
 */
@Getter
@Setter
@Entity
@Table(name = "health_assessments")
public class HealthAssessment {

    public enum Result {
        /** 通过 */
        PASS,
        /** 通过，需要过敏餐 */
        ALLERGY_MEAL,
        /** 通过，需临时观察 */
        TEMP_OBSERVATION,
        /** 需家长补充资料 */
        NEED_SUPPLEMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "application_id")
    private EnrollmentApplication application;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assessor_id")
    private User assessor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Result result;

    private Boolean allergyMealRequired = false;

    private Boolean observationRequired = false;

    /** 要求家长补充的资料说明 */
    @Column(columnDefinition = "text")
    private String supplementRequest;

    @Column(columnDefinition = "text")
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
