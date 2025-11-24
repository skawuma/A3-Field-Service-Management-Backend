package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.common.PageResponse;
import com.a3solutions.fsm.exceptions.NotFoundException;
import com.a3solutions.fsm.technician.TechnicianRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final TechnicianRepository technicianRepo;


    public WorkOrderService(
            WorkOrderRepository repo,
            TechnicianRepository technicianRepo
    ) {
        this.repo = repo;
        this.technicianRepo = technicianRepo;
    }


    // ============================================================
    // PAGE + SEARCH + FILTER + SORT
    // ============================================================
    public PageResponse<WorkOrderDto> getPage(
            int page,
            int size,
            String search,
            String priority,
            String status,
            String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);

        Specification<WorkOrderEntity> spec = Specification.where(
                        WorkOrderSpecification.hasSearch(search)
                )
                .and(WorkOrderSpecification.hasPriority(priority))
                .and(WorkOrderSpecification.hasStatus(status));

        Page<WorkOrderEntity> results = repo.findAll(spec, pageable);

        List<WorkOrderDto> items = results
                .map(this::toDto)
                .getContent();

        return PageResponse.of(
                items,
                results.getNumber(),
                results.getSize(),
                results.getTotalElements()
        );
    }



    // ============================================================
    // SORT PARSER: "field,direction"  (example: "clientName,asc")
    // ============================================================
    private Pageable buildPageable(int page, int size, String sort) {
        // No sort → fallback
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by("id").descending());
        }

        // Split into "field,direction"
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String direction = (parts.length > 1) ? parts[1].trim().toLowerCase() : "desc";

        Sort sortObj;

        switch (direction) {
            case "asc" -> sortObj = Sort.by(field).ascending();
            case "desc" -> sortObj = Sort.by(field).descending();
            default -> sortObj = Sort.by(field).descending();  // fallback
        }

        return PageRequest.of(page, size, sortObj);
    }


    // ============================================================
    // CRUD BELOW REMAINS THE SAME (just formatting improvements)
    // ============================================================

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

    // ============================================================
    // ASSIGN TECHNICIAN (updated)
    // ============================================================
    @Transactional
    public WorkOrderDto assignTechnician(Long id, AssignTechnicianRequest req) {

        var wo = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found: " + id));

        // ✔ Validate technician exists
        technicianRepo.findById(req.technicianId())
                .orElseThrow(() ->
                        new NotFoundException("Technician not found: " + req.technicianId())
                );

        // ✔ Perform assignment
        wo.setAssignedTechId(req.technicianId());
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);

        return toDto(repo.save(wo));
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

    // ============================================================
    // DTO MAPPER
    // ============================================================
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
