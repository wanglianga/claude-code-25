package com.nursery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 协同处理事件：未授权人员到场、孩子发热、药品漏带、抓咬事件、伤情、退费争议等。
 * 老师、保健老师、园长、家长被拉入同一事件处理。
 */
@Getter
@Setter
@Entity
@Table(name = "events")
public class NurseryEvent {

    public enum EventType {
        UNAUTHORIZED_PICKUP, FEVER, MEDICATION_MISSING, BITE_INCIDENT, INJURY, REFUND_DISPUTE, OTHER
    }

    public enum Status { OPEN, PROCESSING, RESOLVED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "child_id")
    private Child child;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** 处理结论（风险复盘记录） */
    @Column(columnDefinition = "text")
    private String resolutionNote;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime closedAt;
}
