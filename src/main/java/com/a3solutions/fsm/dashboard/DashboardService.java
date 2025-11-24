package com.a3solutions.fsm.dashboard;

import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@Service
public class DashboardService {
    private final TechnicianRepository techRepo;
    private final WorkOrderRepository woRepo;

    public DashboardService(TechnicianRepository techRepo, WorkOrderRepository woRepo) {
        this.techRepo = techRepo;
        this.woRepo = woRepo;
    }

    public DashboardSummary getSummary() {
        long totalTechs = techRepo.count();
        long totalWO = woRepo.count();
        long open = woRepo.countByStatus(WorkOrderStatus.OPEN);
        long inProgress = woRepo.countByStatus(WorkOrderStatus.IN_PROGRESS);
        long unassigned = woRepo.countByAssignedTechIdIsNull();

        long today = woRepo.countByScheduledDate(LocalDate.now());

        return new DashboardSummary(
                totalTechs,
                totalWO,
                open,
                inProgress,
                unassigned,
                today
        );
    }
}
