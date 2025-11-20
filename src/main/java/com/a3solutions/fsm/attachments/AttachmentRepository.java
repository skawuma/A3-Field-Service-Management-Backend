package com.a3solutions.fsm.attachments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.attachments
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
public interface AttachmentRepository extends JpaRepository<AttachmentEntity, Long> {
    List<AttachmentEntity> findByWorkOrderId(Long workOrderId);
}
