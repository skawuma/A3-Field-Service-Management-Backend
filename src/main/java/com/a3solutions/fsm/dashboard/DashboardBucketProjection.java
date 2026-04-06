package com.a3solutions.fsm.dashboard;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.dashboard
 * @project A3 Field Service Management Backend
 * @date 4/5/26
 */
public interface DashboardBucketProjection {

    String getBucketKey();

    long getTotal();
}
