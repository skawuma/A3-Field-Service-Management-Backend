package com.a3solutions.fsm.timesheet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
public interface TimesheetRepository extends JpaRepository<TimesheetEntity, Long>,
        JpaSpecificationExecutor<TimesheetEntity> {

    Optional<TimesheetEntity> findByTechnicianIdAndWeekStartDate(Long technicianId, LocalDate weekStartDate);
}
