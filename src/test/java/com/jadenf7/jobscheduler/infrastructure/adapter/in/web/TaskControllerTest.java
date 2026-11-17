package com.jadenf7.jobscheduler.infrastructure.adapter.in.web;

import com.jadenf7.jobscheduler.domain.model.Task;
import com.jadenf7.jobscheduler.domain.model.TaskId;
import com.jadenf7.jobscheduler.domain.model.TaskPayload;
import com.jadenf7.jobscheduler.domain.model.TaskStatus;
import com.jadenf7.jobscheduler.domain.port.in.GetTaskUseCase;
import com.jadenf7.jobscheduler.domain.port.in.SubmitTaskUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final String VALID_BODY = """
            {"taskType": "EMAIL", "data": {"to": "a@b.com"}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmitTaskUseCase submitTaskUseCase;

    @MockitoBean
    private GetTaskUseCase getTaskUseCase;


    @Test
    void submitTask_returnsTask_whenCreated() throws Exception {
        TaskId id = TaskId.of("abc-123");
        when(submitTaskUseCase.submitTask(any(TaskPayload.class))).thenReturn(id);
        when(getTaskUseCase.getTask(id)).thenReturn(Optional.of(pendingTask("abc-123")));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc-123"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.taskType").value("EMAIL"));

        verify(submitTaskUseCase).submitTask(any(TaskPayload.class));
    }

    @Test
    void submitTask_returns404_whenTaskCannotBeLoadedAfterSubmit() throws Exception {
        TaskId id = TaskId.of("abc-123");
        when(submitTaskUseCase.submitTask(any(TaskPayload.class))).thenReturn(id);
        when(getTaskUseCase.getTask(id)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitTask_returns400_whenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(submitTaskUseCase);
    }


    @Test
    void getTask_returnsTask_whenFound() throws Exception {
        when(getTaskUseCase.getTask(TaskId.of("abc-123")))
                .thenReturn(Optional.of(pendingTask("abc-123")));

        mockMvc.perform(get("/api/tasks/abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc-123"));
    }

    @Test
    void getTask_showsFailureReason_whenTaskFailed() throws Exception {
        Task failed = Task.reconstitute(
                TaskId.of("abc-123"),
                TaskPayload.of("EMAIL", Map.of("to", "a@b.com")),
                TaskStatus.FAILED,
                Instant.parse("2026-11-02T10:00:00Z"),
                Instant.parse("2026-11-02T10:01:00Z"),
                Instant.parse("2026-11-02T10:02:00Z"),
                0,
                "SMTP timeout",
                Instant.now()
        );
        when(getTaskUseCase.getTask(TaskId.of("abc-123"))).thenReturn(Optional.of(failed));

        mockMvc.perform(get("/api/tasks/abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("SMTP timeout"));
    }

    @Test
    void getTask_returns404_whenMissing() throws Exception {
        when(getTaskUseCase.getTask(TaskId.of("missing"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tasks/missing"))
                .andExpect(status().isNotFound());
    }


    private Task pendingTask(String id) {
        return Task.reconstitute(
                TaskId.of(id),
                TaskPayload.of("EMAIL", Map.of("to", "a@b.com")),
                TaskStatus.PENDING,
                Instant.parse("2026-11-02T10:00:00Z"),
                null,
                null,
                0,
                null,
                Instant.now()
        );
    }
}