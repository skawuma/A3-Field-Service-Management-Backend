package com.a3solutions.fsm.workorder;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3_SOLUTIONS_PROJECT
 * @date 11/26/25
 */

@Service
public class WorkOrderEventService {

    private final WorkOrderEventRepository repository;

    public WorkOrderEventService(WorkOrderEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WorkOrderEventEntity recordEvent(
            WorkOrderEntity workOrder,
            WorkOrderEventType type,
            String message,
            String oldValue,
            String newValue,
            String actor
    ) {
        WorkOrderEventEntity event = new WorkOrderEventEntity();
        event.setWorkOrder(workOrder);
        event.setEventType(type);
        event.setMessage(message);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setActor(actor);
        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderEventEntity> getTimeline(Long workOrderId) {
        return repository.findByWorkOrderIdOrderByCreatedAtAsc(workOrderId);
    }

    // Simple mapper to DTO
    public WorkOrderEventDto toDto(WorkOrderEventEntity e) {
        WorkOrderEventDto dto = new WorkOrderEventDto();
        dto.setId(e.getId());
        dto.setEventType(e.getEventType());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setMessage(e.getMessage());
        dto.setOldValue(e.getOldValue());
        dto.setNewValue(e.getNewValue());
        dto.setActor(e.getActor());
        return dto;
    }
}
