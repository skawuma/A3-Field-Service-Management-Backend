package com.a3solutions.fsm.workordercompletion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workordercompletion
 * @project A3 Field Service Management Backend
 * @date 3/28/26
 */
public class WorkOrderCompletionRequest {


    @NotBlank(message = "FA Tag / Device is required")
    @Size(max = 100, message = "FA Tag / Device must be at most 100 characters")
    private String faTag;

    @NotNull(message = "Issue resolved is required")
    private Boolean issueResolved;

    @NotNull(message = "Replacement needed is required")
    private ReplacementNeeded replacementNeeded;

    @NotNull(message = "Return visit required is required")
    private Boolean returnVisitRequired;

    @NotBlank(message = "Summary of work performed is required")
    @Size(max = 3000, message = "Summary must be at most 3000 characters")
    private String summaryOfWork;

    public WorkOrderCompletionRequest() {
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
}