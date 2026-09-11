package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 园长分班记录
 */
@Getter
@Setter
@Entity
@Table(name = "class_assignments")
public class ClassAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "application_id")
    private EnrollmentApplication application;

    @ManyToOne(optional = false)
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    /** 分班备注（容量/师幼比/特殊照护/家长期望考量） */
    @Column(columnDefinition = "text")
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
