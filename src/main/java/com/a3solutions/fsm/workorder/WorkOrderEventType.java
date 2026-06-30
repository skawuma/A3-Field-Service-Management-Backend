package com.a3solutions.fsm.workorder;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3_SOLUTIONS_PROJECT
 * @date 11/26/25
 */
public enum WorkOrderEventType {
    CREATED,
    UPDATED_DETAILS,
    STATUS_CHANGED,
    PRIORITY_CHANGED,
    ASSIGNED_TECHNICIAN,
    UNASSIGNED_TECHNICIAN,
    STARTED,
    TRAVEL_STARTED,
    ARRIVED_ONSITE,
    WORK_STARTED,
    SLA_NEAR_BREACH,
    SLA_BREACHED,
    SLA_MET,
    SCHEDULED_DATE_CHANGED,
    NOTE_ADDED,
    ATTACHMENT_ADDED,
    REOPENED,
    COMPLETED
}
