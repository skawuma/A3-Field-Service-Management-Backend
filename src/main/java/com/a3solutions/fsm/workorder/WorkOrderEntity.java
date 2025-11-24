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
