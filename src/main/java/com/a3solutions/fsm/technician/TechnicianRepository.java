package com.a3solutions.fsm.technician;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.technician
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public interface TechnicianRepository extends JpaRepository<TechnicianEntity, Long> {

    Optional<TechnicianEntity> findByEmail(String email);


    Optional<TechnicianEntity> findByUserId(Long userId);
}
