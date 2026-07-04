package com.a3solutions.fsm.timesheet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/03/26
 */
public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntryEntity, Long> {

    boolean existsByWorkOrderId(Long workOrderId);

    Optional<TimesheetEntryEntity> findByWorkOrderId(Long workOrderId);

    List<TimesheetEntryEntity> findByTimesheetIdOrderByWorkDateAscIdAsc(Long timesheetId);
}
