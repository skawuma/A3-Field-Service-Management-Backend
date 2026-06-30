package com.a3solutions.fsm.workorder;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Entity
@Table(name = "work_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientName;
    private String address;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private WorkOrderStatus status = WorkOrderStatus.OPEN;

    private Long assignedTechId;
    private LocalDate scheduledDate;
    private String priority;

//    @Column(nullable = false, updatable = false)
//    private Instant createdAt = Instant.now();
@Column(name = "signature_url", length = 1000)
private String signatureUrl;

    @Column(name = "completion_notes", length = 2000)
    private String completionNotes;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "en_route_at")
    private Instant enRouteAt;

    @Column(name = "arrived_at")
    private Instant arrivedAt;

    @Column(name = "work_started_at")
    private Instant workStartedAt;

    @Column(name = "sla_clock_started_at")
    private Instant slaClockStartedAt;

    @Column(name = "sla_due_at")
    private Instant slaDueAt;

    @Builder.Default
    @Column(name = "sla_breached", nullable = false)
    private Boolean slaBreached = false;

    @Column(name = "sla_duration_minutes")
    private Integer slaDurationMinutes;

    @Column(name = "actual_completion_minutes")
    private Integer actualCompletionMinutes;

    @Column(name = "breach_minutes")
    private Integer breachMinutes;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
