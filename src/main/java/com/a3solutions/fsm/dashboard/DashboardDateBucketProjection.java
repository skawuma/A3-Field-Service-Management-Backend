package com.a3solutions.fsm.dashboard;

import java.time.LocalDate;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/5/26
 */
public interface DashboardDateBucketProjection {

    LocalDate getBucketDate();

    long getTotal();
}
