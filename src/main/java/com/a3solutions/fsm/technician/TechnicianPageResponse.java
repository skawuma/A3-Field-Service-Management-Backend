package com.a3solutions.fsm.technician;

import com.a3solutions.fsm.common.PageResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated technician response.")
public class TechnicianPageResponse extends PageResponse<TechnicianDto> {

    @Override
    @ArraySchema(schema = @Schema(implementation = TechnicianDto.class))
    public java.util.List<TechnicianDto> getContent() {
        return super.getContent();
    }
}
