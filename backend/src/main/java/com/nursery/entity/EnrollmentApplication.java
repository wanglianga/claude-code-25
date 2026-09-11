package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 入托申请 / 入托评估任务
 */
@Getter
@Setter
@Entity
@Table(name = "enrollment_applications")
public class EnrollmentApplication {

    public enum Status {
        /** 待园长分班 */
        PENDING_CLASS,
        /** 待保健老师健康评估 */
        PENDING_HEALTH,
        /** 需家长补充资料 */
        NEED_SUPPLEMENT,
        /** 已完成入托 */
        COMPLETED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    /** 家长期望 */
    @Column(columnDefinition = "text")
    private String parentExpectations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING_CLASS;

    /** 保健老师要求家长补充的资料说明 */
    @Column(columnDefinition = "text")
    private String supplementRequest;

    /** 家长补充的资料 */
    @Column(columnDefinition = "text")
    private String supplementNote;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
