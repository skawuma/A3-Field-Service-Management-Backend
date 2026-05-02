package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.common.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "WorkOrderPageResponse",
        description = "Paginated work order results."
)
public class WorkOrderPageResponse extends PageResponse<WorkOrderDto> {
}
