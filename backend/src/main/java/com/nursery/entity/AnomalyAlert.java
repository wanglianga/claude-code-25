package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 同一儿童多次异常时生成的预警（建议调整班级/增加保健观察/约谈家长）
 */
@Getter
@Setter
@Entity
@Table(name = "anomaly_alerts")
public class AnomalyAlert {

    public enum Suggestion { CLASS_ADJUST, HEALTH_OBSERVATION, PARENT_MEETING }

    public enum Status { OPEN, HANDLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    /** 统计窗口内异常次数 */
    private Integer anomalyCount;

    /** 统计窗口（天） */
    private Integer windowDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Suggestion suggestion;

    @Column(columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @ManyToOne
    @JoinColumn(name = "handled_by")
    private User handledBy;

    @Column(columnDefinition = "text")
    private String handleNote;

    private LocalDateTime handledAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
