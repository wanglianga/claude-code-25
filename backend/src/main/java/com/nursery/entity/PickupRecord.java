package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 接送核验记录
 */
@Getter
@Setter
@Entity
@Table(name = "pickup_records")
public class PickupRecord {

    public enum AuthType { AUTHORIZED, TEMP_DELEGATION }

    public enum VerifyMethod { FACE, ID_CARD }

    public enum Result { SUCCESS, DENIED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(nullable = false)
    private LocalDate pickupDate;

    @Column(nullable = false, length = 64)
    private String pickupPersonName;

    @Column(length = 64)
    private String pickupPersonIdNumber;

    /** 核验到的授权类型（未授权人员为空） */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AuthType authType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerifyMethod verifyMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Result result;

    @Column(columnDefinition = "text")
    private String denyReason;

    /** 操作的前台 */
    @ManyToOne(optional = false)
    @JoinColumn(name = "operator_id")
    private User operator;

    private LocalDateTime createdAt = LocalDateTime.now();
}
