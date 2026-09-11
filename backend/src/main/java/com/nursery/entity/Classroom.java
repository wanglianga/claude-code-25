package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "classrooms")
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    /** 班级容量（最大在托儿童数） */
    @Column(nullable = false)
    private Integer capacity;

    /** 适龄段，如 "1-2岁" */
    @Column(length = 32)
    private String ageRange;

    @Column(length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "class_teachers",
            joinColumns = @JoinColumn(name = "classroom_id"),
            inverseJoinColumns = @JoinColumn(name = "teacher_id"))
    private Set<User> teachers = new LinkedHashSet<>();
}
