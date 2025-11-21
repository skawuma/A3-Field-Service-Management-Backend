package com.a3solutions.fsm.workorder;

import org.springframework.data.jpa.domain.Specification;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/20/25
 */
public class WorkOrderSpecification {

    // ============================================================
    // TEXT SEARCH: clientName, address, description, status
    // ============================================================
    public static Specification<WorkOrderEntity> hasSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction(); // no filter
            }

            String pattern = "%" + search.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("clientName")), pattern),
                    cb.like(cb.lower(root.get("address")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("status").as(String.class)), pattern)
            );
        };
    }

    // ============================================================
    // PRIORITY FILTER
    // ============================================================
    public static Specification<WorkOrderEntity> hasPriority(String priority) {
        return (root, query, cb) -> {
            if (priority == null || priority.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(root.get("priority"), priority);
        };
    }

    // ============================================================
    // STATUS FILTER
    // ============================================================
    public static Specification<WorkOrderEntity> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(root.get("status"), WorkOrderStatus.valueOf(status));
        };
    }

    // ============================================================
    // COMBINE EVERYTHING
    // (not mandatory to use directly since Service handles this)
    // ============================================================
    public static Specification<WorkOrderEntity> build(
            String search,
            String priority,
            String status
    ) {
        return Specification.where(hasSearch(search))
                .and(hasPriority(priority))
                .and(hasStatus(status));
    }
}
