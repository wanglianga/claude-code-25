package com.nursery.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "event_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private NurseryEvent event;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** 加入事件时的角色快照 */
    @Column(length = 20)
    private String roleLabel;

    private LocalDateTime joinedAt = LocalDateTime.now();

    public EventParticipant() {}

    public EventParticipant(NurseryEvent event, User user) {
        this.event = event;
        this.user = user;
        this.roleLabel = user.getRole() == null ? null : user.getRole().name();
    }
}
