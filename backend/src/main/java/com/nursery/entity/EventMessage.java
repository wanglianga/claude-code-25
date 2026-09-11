package com.nursery.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 事件内的多方沟通消息
 */
@Getter
@Setter
@Entity
@Table(name = "event_messages")
public class EventMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private NurseryEvent event;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();
}
