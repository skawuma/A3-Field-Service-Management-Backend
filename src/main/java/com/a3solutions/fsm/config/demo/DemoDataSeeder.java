package com.a3solutions.fsm.config.demo;

import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.timesheet.TimesheetEntryRepository;
import com.a3solutions.fsm.timesheet.TimesheetService;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final DemoDataProperties properties;
    private final DemoUserSeeder userSeeder;
    private final DemoTechnicianSeeder technicianSeeder;
    private final DemoWorkOrderSeeder workOrderSeeder;
    private final WorkOrderRepository workOrderRepository;
    private final TechnicianRepository technicianRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final TimesheetService timesheetService;

    public DemoDataSeeder(
            DemoDataProperties properties,
            DemoUserSeeder userSeeder,
            DemoTechnicianSeeder technicianSeeder,
            DemoWorkOrderSeeder workOrderSeeder,
            WorkOrderRepository workOrderRepository,
            TechnicianRepository technicianRepository,
            TimesheetEntryRepository timesheetEntryRepository,
            TimesheetService timesheetService
    ) {
        this.properties = properties;
        this.userSeeder = userSeeder;
        this.technicianSeeder = technicianSeeder;
        this.workOrderSeeder = workOrderSeeder;
        this.workOrderRepository = workOrderRepository;
        this.technicianRepository = technicianRepository;
        this.timesheetEntryRepository = timesheetEntryRepository;
        this.timesheetService = timesheetService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isMode() || !properties.isDataEnabled()) {
            log.info("Demo profile is active, but demo data seeding is disabled.");
            return;
        }

        DemoUserSeeder.DemoUsers users = userSeeder.seed();
        DemoTechnicianSeeder.DemoTechnicians technicians = technicianSeeder.seed(users);
        int createdWorkOrders = workOrderSeeder.seed(users, technicians);
        long timesheetEntriesBefore = timesheetEntryRepository.count();
        workOrderRepository.findAll().stream()
                .filter(workOrder -> workOrder.getStatus() == WorkOrderStatus.COMPLETED)
                .filter(workOrder -> workOrder.getAssignedTechId() != null)
                .forEach(workOrder -> technicianRepository.findById(workOrder.getAssignedTechId())
                        .ifPresent(technician -> timesheetService.autoAddCompletedWorkOrder(workOrder, technician)));
        long createdTimesheetEntries = timesheetEntryRepository.count() - timesheetEntriesBefore;

        log.info(
                "Demo readiness data is available: 3 demo users, 4 technicians, {} new work orders, {} new timesheet entries.",
                createdWorkOrders,
                createdTimesheetEntries
        );
    }
}
