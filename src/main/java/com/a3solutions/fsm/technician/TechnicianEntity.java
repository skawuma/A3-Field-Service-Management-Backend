package com.a3solutions.fsm.technician;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.technician
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Entity
@Table(name = "technicians")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phone;
    private String email;
    private String certifications;

    @Enumerated(EnumType.STRING)
    private TechnicianStatus status = TechnicianStatus.ACTIVE;
    @Column(name = "user_id")
    private Long userId;
//    @Builder.Default
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

    // NEW: full name helper
    public String getFullName() {
        if (firstName == null && lastName == null) return "";
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return (firstName + " " + lastName).trim();
    }
}

