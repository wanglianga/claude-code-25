package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 家长登记的接送授权人
 */
@Getter
@Setter
@Entity
@Table(name = "pickup_authorizations")
public class PickupAuthorization {

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

    @Column(length = 32)
    private String phone;

    /** 证件号，用于前台证件核验 */
    @Column(length = 64)
    private String idNumber;

    private Boolean active = true;
}
