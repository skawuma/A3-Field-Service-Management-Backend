package com.a3solutions.fsm.workorder;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public enum WorkOrderStatus {
    OPEN,
    ASSIGNED,
    EN_ROUTE,
    ARRIVED,
    WORK_STARTED,
    /** @deprecated retained for work orders created before Sprint 11. */
    IN_PROGRESS,
    COMPLETED,
    CANCELLED

}
