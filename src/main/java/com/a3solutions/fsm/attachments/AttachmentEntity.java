package com.a3solutions.fsm.attachments;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.attachments
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workOrderId;

    private String filename;

    private String url;

    private long sizeBytes;

    private Instant uploadedAt = Instant.now();

    private String uploadedBy;
}