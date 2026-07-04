package com.a3solutions.fsm.config.demo;

import com.a3solutions.fsm.auth.UserRepository;
import com.a3solutions.fsm.security.Role;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.timesheet.TimesheetEntryRepository;
import com.a3solutions.fsm.timesheet.TimesheetRepository;
import com.a3solutions.fsm.timesheet.TimesheetStatus;
import com.a3solutions.fsm.workorder.WorkOrderEventRepository;
import com.a3solutions.fsm.workorder.WorkOrderRepository;
import com.a3solutions.fsm.workorder.WorkOrderStatus;
import com.a3solutions.fsm.workordercompletion.WorkOrderCompletionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
class DemoDataSeederIntegrationTest {

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkOrderEventRepository eventRepository;

    @Autowired
    private WorkOrderCompletionRepository completionRepository;

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private TimesheetEntryRepository timesheetEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedsPortfolioReadyDemoData() {
        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(technicianRepository.count()).isEqualTo(4);
        assertThat(workOrderRepository.count()).isEqualTo(8);
        assertThat(eventRepository.count()).isEqualTo(20);
        assertThat(completionRepository.count()).isEqualTo(2);
        assertThat(timesheetRepository.count()).isEqualTo(1);
        assertThat(timesheetEntryRepository.count()).isEqualTo(2);
        assertThat(timesheetRepository.findAll().getFirst().getStatus()).isEqualTo(TimesheetStatus.DRAFT);

        assertThat(workOrderRepository.countByStatus(WorkOrderStatus.OPEN)).isEqualTo(2);
        assertThat(workOrderRepository.countByStatus(WorkOrderStatus.ASSIGNED)).isEqualTo(2);
        assertThat(workOrderRepository.countByStatus(WorkOrderStatus.EN_ROUTE)).isEqualTo(1);
        assertThat(workOrderRepository.countByStatus(WorkOrderStatus.WORK_STARTED)).isEqualTo(1);
        assertThat(workOrderRepository.countByStatus(WorkOrderStatus.COMPLETED)).isEqualTo(2);
        assertThat(workOrderRepository.findAll().stream().filter(wo -> wo.getSlaDueAt() != null).count()).isEqualTo(4);
        assertThat(workOrderRepository.findAll().stream().filter(wo -> Boolean.TRUE.equals(wo.getSlaBreached())).count()).isEqualTo(2);

        var admin = userRepository.findByEmail("admin.demo@a3fsm.com").orElseThrow();
        var dispatcher = userRepository.findByEmail("dispatcher.demo@a3fsm.com").orElseThrow();
        var technician = userRepository.findByEmail("tech.demo@a3fsm.com").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(dispatcher.getRole()).isEqualTo(Role.DISPATCH);
        assertThat(technician.getRole()).isEqualTo(Role.TECH);
        assertThat(passwordEncoder.matches("DemoAdmin2026!", admin.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("DemoDispatch2026!", dispatcher.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("DemoTech2026!", technician.getPassword())).isTrue();
    }

    @Test
    void rerunningSeederDoesNotDuplicateDemoRecords() throws Exception {
        long users = userRepository.count();
        long technicians = technicianRepository.count();
        long workOrders = workOrderRepository.count();
        long events = eventRepository.count();
        long completions = completionRepository.count();
        long timesheets = timesheetRepository.count();
        long timesheetEntries = timesheetEntryRepository.count();

        demoDataSeeder.run(new DefaultApplicationArguments(new String[0]));

        assertThat(userRepository.count()).isEqualTo(users);
        assertThat(technicianRepository.count()).isEqualTo(technicians);
        assertThat(workOrderRepository.count()).isEqualTo(workOrders);
        assertThat(eventRepository.count()).isEqualTo(events);
        assertThat(completionRepository.count()).isEqualTo(completions);
        assertThat(timesheetRepository.count()).isEqualTo(timesheets);
        assertThat(timesheetEntryRepository.count()).isEqualTo(timesheetEntries);
    }
}
