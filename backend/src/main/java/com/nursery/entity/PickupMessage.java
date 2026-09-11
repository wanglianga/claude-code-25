package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家长给前台的接送留言
 */
@Getter
@Setter
@Entity
@Table(name = "pickup_messages")
public class PickupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(nullable = false)
    private LocalDate msgDate;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime createdAt = LocalDateTime.now();
}
