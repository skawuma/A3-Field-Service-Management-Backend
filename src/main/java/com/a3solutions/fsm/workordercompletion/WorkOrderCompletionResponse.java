package com.a3solutions.fsm.workordercompletion;

import java.time.LocalDateTime;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workordercompletion
 * @project A3 Field Service Management Backend
 * @date 3/28/26
 */
public class WorkOrderCompletionResponse {
    private Long id;
    private Long workOrderId;
    private String faTag;
    private Boolean issueResolved;
    private ReplacementNeeded replacementNeeded;
    private Boolean returnVisitRequired;
    private String summaryOfWork;
    private LocalDateTime completedAt;
    private Long completedByUserId;

    public WorkOrderCompletionResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public String getFaTag() {
        return faTag;
    }

    public void setFaTag(String faTag) {
        this.faTag = faTag;
    }

    public Boolean getIssueResolved() {
        return issueResolved;
    }

    public void setIssueResolved(Boolean issueResolved) {
        this.issueResolved = issueResolved;
    }

    public ReplacementNeeded getReplacementNeeded() {
        return replacementNeeded;
    }

    public void setReplacementNeeded(ReplacementNeeded replacementNeeded) {
        this.replacementNeeded = replacementNeeded;
    }

    public Boolean getReturnVisitRequired() {
        return returnVisitRequired;
    }

    public void setReturnVisitRequired(Boolean returnVisitRequired) {
        this.returnVisitRequired = returnVisitRequired;
    }

    public String getSummaryOfWork() {
        return summaryOfWork;
    }

    public void setSummaryOfWork(String summaryOfWork) {
        this.summaryOfWork = summaryOfWork;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getCompletedByUserId() {
        return completedByUserId;
    }

    public void setCompletedByUserId(Long completedByUserId) {
        this.completedByUserId = completedByUserId;
    }
}