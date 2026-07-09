package com.a3solutions.fsm.timesheet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.timesheet
 * @project A3 Field Service Management Backend
 * @date 07/09/26
 */
@SpringBootTest(properties = {
        "springdoc.api-docs.path=/api-docs",
        "springdoc.swagger-ui.path=/swagger-ui"
})
@AutoConfigureMockMvc
class TimesheetOpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiIncludesTimesheetModuleAndSwaggerUiLoads() throws Exception {
        String apiDocs = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(apiDocs)
                .contains("\"name\":\"Timesheets\"")
                .contains("\"/api/timesheets/my/current\"")
                .contains("\"/api/timesheets/my\"")
                .contains("\"/api/timesheets/{id}\"")
                .contains("\"/api/timesheets/entries/{entryId}\"")
                .contains("\"/api/timesheets/{id}/submit\"")
                .contains("\"/api/timesheets/{id}/approve\"")
                .contains("\"/api/timesheets/{id}/reject\"")
                .contains("\"/api/timesheets/{id}/export/csv\"")
                .contains("TECH only")
                .contains("ADMIN and DISPATCH")
                .contains("Only SUBMITTED timesheets can be approved");

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
