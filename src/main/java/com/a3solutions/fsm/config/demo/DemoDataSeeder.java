package com.a3solutions.fsm.config.demo;

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

    public DemoDataSeeder(
            DemoDataProperties properties,
            DemoUserSeeder userSeeder,
            DemoTechnicianSeeder technicianSeeder,
            DemoWorkOrderSeeder workOrderSeeder
    ) {
        this.properties = properties;
        this.userSeeder = userSeeder;
        this.technicianSeeder = technicianSeeder;
        this.workOrderSeeder = workOrderSeeder;
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

        log.info(
                "Demo readiness data is available: 3 demo users, 4 technicians, {} new work orders.",
                createdWorkOrders
        );
    }
}
