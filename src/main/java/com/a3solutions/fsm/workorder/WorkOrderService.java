package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.common.PageResponse;
import com.a3solutions.fsm.exceptions.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@Service
public class WorkOrderService {
    private final WorkOrderRepository repo;

    public WorkOrderService(WorkOrderRepository repo) {
        this.repo = repo;
    }

    public PageResponse<WorkOrderDto> getPage(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<WorkOrderEntity> result = repo.findAll(pageable);

        List<WorkOrderDto> items = result.getContent()
                .stream()
                .map(this::toDto)
                .toList();

        return PageResponse.of(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    public WorkOrderDto getById(Long id) {
        var wo = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found"));
        return toDto(wo);
    }

    @Transactional
    public WorkOrderDto create(WorkOrderCreateRequest req) {
        var entity = WorkOrderEntity.builder()
                .clientName(req.clientName())
                .address(req.address())
                .description(req.description())
                .assignedTechId(req.assignedTechId())
                .scheduledDate(req.scheduledDate())
                .priority(req.priority())
                .status(WorkOrderStatus.OPEN)
                .build();
        return toDto(repo.save(entity));
    }

    @Transactional
    public WorkOrderDto assignTechnician(Long id, AssignTechnicianRequest req) {
        var wo = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found"));

        wo.setAssignedTechId(req.technicianId());
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);

        repo.save(wo);

        return toDto(wo);
    }


    @Transactional
    public WorkOrderDto update(Long id, WorkOrderCreateRequest req) {
        var existing = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found"));

        existing.setClientName(req.clientName());
        existing.setAddress(req.address());
        existing.setDescription(req.description());
        existing.setAssignedTechId(req.assignedTechId());
        existing.setScheduledDate(req.scheduledDate());
        existing.setPriority(req.priority());

        return toDto(repo.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Work order not found");
        }
        repo.deleteById(id);
    }

    private WorkOrderDto toDto(WorkOrderEntity e) {
        return new WorkOrderDto(
                e.getId(),
                e.getClientName(),
                e.getAddress(),
                e.getDescription(),
                e.getStatus(),
                e.getAssignedTechId(),
                e.getScheduledDate(),
                e.getPriority()
        );
    }

}
