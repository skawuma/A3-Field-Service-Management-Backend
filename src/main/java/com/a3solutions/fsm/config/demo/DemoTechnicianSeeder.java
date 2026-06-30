package com.a3solutions.fsm.config.demo;

import com.a3solutions.fsm.technician.TechnicianEntity;
import com.a3solutions.fsm.technician.TechnicianRepository;
import com.a3solutions.fsm.technician.TechnicianStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoTechnicianSeeder {

    private final TechnicianRepository technicianRepository;
    private final DemoDataProperties properties;

    public DemoTechnicianSeeder(
            TechnicianRepository technicianRepository,
            DemoDataProperties properties
    ) {
        this.technicianRepository = technicianRepository;
        this.properties = properties;
    }

    public DemoTechnicians seed(DemoUserSeeder.DemoUsers users) {
        TechnicianEntity james = upsertTechnician(
                "James",
                "Carter",
                "617-555-0101",
                properties.getTechnician().getEmail(),
                "CompTIA A+, Network+",
                TechnicianStatus.ACTIVE,
                users.technician().getId()
        );
        TechnicianEntity maria = upsertTechnician(
                "Maria",
                "Lopez",
                "617-555-0102",
                "maria.lopez.demo@a3fsm.com",
                "Hardware Support, POS Systems",
                TechnicianStatus.ACTIVE,
                null
        );
        TechnicianEntity daniel = upsertTechnician(
                "Daniel",
                "Brooks",
                "617-555-0103",
                "daniel.brooks.demo@a3fsm.com",
                "Network+, Structured Cabling",
                TechnicianStatus.ACTIVE,
                null
        );
        TechnicianEntity aisha = upsertTechnician(
                "Aisha",
                "Morgan",
                "617-555-0104",
                "aisha.morgan.demo@a3fsm.com",
                "Microsoft 365, Endpoint Support",
                TechnicianStatus.INACTIVE,
                null
        );

        return new DemoTechnicians(james, maria, daniel, aisha);
    }

    private TechnicianEntity upsertTechnician(
            String firstName,
            String lastName,
            String phone,
            String email,
            String certifications,
            TechnicianStatus status,
            Long userId
    ) {
        TechnicianEntity technician = technicianRepository.findByEmail(email)
                .orElseGet(TechnicianEntity::new);

        technician.setFirstName(firstName);
        technician.setLastName(lastName);
        technician.setPhone(phone);
        technician.setEmail(email);
        technician.setCertifications(certifications);
        technician.setStatus(status);
        technician.setUserId(userId);

        return technicianRepository.save(technician);
    }

    public record DemoTechnicians(
            TechnicianEntity james,
            TechnicianEntity maria,
            TechnicianEntity daniel,
            TechnicianEntity aisha
    ) {
    }
}
