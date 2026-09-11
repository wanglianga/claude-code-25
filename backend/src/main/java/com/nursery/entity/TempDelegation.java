package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家长临时接送委托
 */
@Getter
@Setter
@Entity
@Table(name = "temp_delegations")
public class TempDelegation {

    public enum Status { ACTIVE, USED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(nullable = false, length = 64)
    private String delegateName;

    @Column(length = 32)
    private String delegatePhone;

    @Column(length = 64)
    private String delegateIdNumber;

    /** 委托生效日期 */
    @Column(nullable = false)
    private LocalDate validDate;

    @Column(columnDefinition = "text")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    private LocalDateTime createdAt = LocalDateTime.now();
}
