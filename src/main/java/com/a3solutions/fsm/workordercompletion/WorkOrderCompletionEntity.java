package com.a3solutions.fsm.workordercompletion;
import com.a3solutions.fsm.workorder.WorkOrderEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
/**
 * @author samuelkawuma
 * @package workordercompletion
 * @project A3 Field Service Management Backend
 * @date 3/28/26
 */
@Entity
@Table(name = "work_order_completions")
public class WorkOrderCompletionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false, unique = true)
    private WorkOrderEntity workOrder;

    @Column(name = "fa_tag", nullable = false, length = 100)
    private String faTag;

    @Column(name = "issue_resolved", nullable = false)
    private Boolean issueResolved;

    @Enumerated(EnumType.STRING)
    @Column(name = "replacement_needed", nullable = false, length = 20)
    private ReplacementNeeded replacementNeeded;

    @Column(name = "return_visit_required", nullable = false)
    private Boolean returnVisitRequired;

    @Column(name = "summary_of_work", nullable = false, length = 3000)
    private String summaryOfWork;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "completed_by_user_id", nullable = false)
    private Long completedByUserId;

    public WorkOrderCompletionEntity() {
    }

    public Long getId() {
        return id;
    }

    public WorkOrderEntity getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrderEntity workOrder) {
        this.workOrder = workOrder;
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
