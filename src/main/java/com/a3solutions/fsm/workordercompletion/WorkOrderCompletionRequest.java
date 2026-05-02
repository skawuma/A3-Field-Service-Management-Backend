package com.a3solutions.fsm.workordercompletion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workordercompletion
 * @project A3 Field Service Management Backend
 * @date 3/28/26
 */
@Schema(description = "Structured field-service completion report submitted before final sign-off.")
public class WorkOrderCompletionRequest {


    @Schema(description = "FA tag, asset tag, or device reference captured in the field.", example = "FA-2241")
    @NotBlank(message = "FA Tag / Device is required")
    @Size(max = 100, message = "FA Tag / Device must be at most 100 characters")
    private String faTag;

    @Schema(description = "Whether the reported issue was resolved during the visit.", example = "true")
    @NotNull(message = "Issue resolved is required")
    private Boolean issueResolved;

    @Schema(description = "Whether a replacement was needed.", example = "NO")
    @NotNull(message = "Replacement needed is required")
    private ReplacementNeeded replacementNeeded;

    @Schema(description = "Whether a return visit is required.", example = "false")
    @NotNull(message = "Return visit required is required")
    private Boolean returnVisitRequired;

    @Schema(description = "Narrative summary of the work performed onsite.", example = "Reset control board, replaced fuse, tested startup sequence, and verified output stability.")
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
