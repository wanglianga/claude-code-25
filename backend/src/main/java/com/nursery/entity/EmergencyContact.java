package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "emergency_contacts")
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String relation;

    @Column(nullable = false, length = 32)
    private String phone;

    /** 联系优先级，1 最高 */
    private Integer priority = 1;
}
