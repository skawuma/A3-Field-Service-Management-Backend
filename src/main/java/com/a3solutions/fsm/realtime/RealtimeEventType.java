package com.a3solutions.fsm.realtime;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.realtime
 * @project A3 Field Service Management Backend
 * @date 4/14/26
 */


/**
 * Types of realtime events broadcast to connected dashboard clients.
 */
public enum RealtimeEventType  {
    WORK_ORDER_CREATED,
    WORK_ORDER_ASSIGNED,
    WORK_ORDER_STATUS_CHANGED,
    WORK_ORDER_COMPLETED,
    SLA_BREACHED,
    ALERT_CREATED
}
