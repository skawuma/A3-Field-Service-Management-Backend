package com.a3solutions.fsm.workordercompletion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workordercompletion
 * @project A3 Field Service Management Backend
 * @date 3/28/26
 */
@Schema(description = "Stored structured completion report for a work order.")
public class WorkOrderCompletionResponse {
    @Schema(description = "Completion report identifier.", example = "12")
    private Long id;
    @Schema(description = "Associated work order identifier.", example = "42")
    private Long workOrderId;
    @Schema(description = "FA tag, asset tag, or device reference.", example = "FA-2241")
    private String faTag;
    @Schema(description = "Whether the issue was resolved.", example = "true")
    private Boolean issueResolved;
    @Schema(description = "Whether replacement was required.", example = "NO")
    private ReplacementNeeded replacementNeeded;
    @Schema(description = "Whether a return visit is required.", example = "false")
    private Boolean returnVisitRequired;
    @Schema(description = "Summary of work performed onsite.")
    private String summaryOfWork;
    @Schema(description = "Timestamp when the report was saved.")
    private LocalDateTime completedAt;
    @Schema(description = "User id of the submitting technician.", example = "7")
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
