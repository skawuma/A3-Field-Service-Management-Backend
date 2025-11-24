package com.a3solutions.fsm.technician;

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
 * @package com.a3solutions.fsm.technician
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */

@Service
public class TechnicianService {

    private final TechnicianRepository repo;

    public TechnicianService(TechnicianRepository repo) {
        this.repo = repo;
    }

    // ============================================================
    // PAGINATION (tailored to match WorkOrder logic)
    // ============================================================
    public PageResponse<TechnicianDto> getPage(int page, int size, String sort) {

        Pageable pageable = buildPageable(page, size, sort);

        Page<TechnicianEntity> result = repo.findAll(pageable);

        List<TechnicianDto> items = result
                .getContent()
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

    private Pageable buildPageable(int page, int size, String sort) {

        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by("lastName").ascending());
        }

        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String direction = (parts.length > 1)
                ? parts[1].trim().toLowerCase()
                : "asc";

        Sort sortObj = switch (direction) {
            case "desc" -> Sort.by(field).descending();
            default -> Sort.by(field).ascending();
        };

        return PageRequest.of(page, size, sortObj);
    }

    // ============================================================
    // CRUD (unchanged)
    // ============================================================
    public TechnicianDto getById(Long id) {
        var tech = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Technician not found"));
        return toDto(tech);
    }

    @Transactional
    public TechnicianDto create(TechnicianCreateRequest req) {
        var entity = TechnicianEntity.builder()
                .firstName(req.firstName())
                .lastName(req.lastName())
                .phone(req.phone())
                .email(req.email())
                .certifications(req.certifications())
                .status(TechnicianStatus.ACTIVE)
                .build();

        return toDto(repo.save(entity));
    }

    @Transactional
    public TechnicianDto update(Long id, TechnicianCreateRequest req) {
        var existing = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Technician not found"));

        existing.setFirstName(req.firstName());
        existing.setLastName(req.lastName());
        existing.setPhone(req.phone());
        existing.setEmail(req.email());
        existing.setCertifications(req.certifications());

        return toDto(repo.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Technician not found");
        }
        repo.deleteById(id);
    }

    private TechnicianDto toDto(TechnicianEntity e) {
        return new TechnicianDto(
                e.getId(),
                e.getFirstName(),
                e.getLastName(),
                e.getPhone(),
                e.getEmail(),
                e.getCertifications(),
                e.getStatus()
        );
    }
}
