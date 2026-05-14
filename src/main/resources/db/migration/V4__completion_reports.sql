CREATE TABLE work_order_completions (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    fa_tag VARCHAR(100) NOT NULL,
    issue_resolved BOOLEAN NOT NULL,
    replacement_needed VARCHAR(20) NOT NULL,
    return_visit_required BOOLEAN NOT NULL,
    summary_of_work VARCHAR(3000) NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    completed_by_user_id BIGINT NOT NULL
);

ALTER TABLE work_order_completions
    ADD CONSTRAINT uk_work_order_completions_work_order_id UNIQUE (work_order_id);
