package com.jadenf7.jobscheduler.infrastructure.adapter.in.web;

import com.jadenf7.jobscheduler.domain.model.TaskId;
import com.jadenf7.jobscheduler.domain.model.TaskPayload;
import com.jadenf7.jobscheduler.domain.port.in.GetTaskUseCase;
import com.jadenf7.jobscheduler.domain.port.in.SubmitTaskUseCase;
import com.jadenf7.jobscheduler.infrastructure.adapter.in.web.dto.TaskRequestDto;
import com.jadenf7.jobscheduler.infrastructure.adapter.in.web.dto.TaskResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final SubmitTaskUseCase submitTaskUseCase;
    private final GetTaskUseCase getTaskUseCase;

    public TaskController(SubmitTaskUseCase submitTaskUseCase, GetTaskUseCase getTaskUseCase) {
        this.submitTaskUseCase = submitTaskUseCase;
        this.getTaskUseCase = getTaskUseCase;
    }

    @PostMapping
    public ResponseEntity<TaskResponseDto> submitTask(@Valid @RequestBody TaskRequestDto request) {
        TaskPayload payload = TaskPayload.of(request.taskType(), request.data());
        TaskId id = submitTaskUseCase.submitTask(payload);

        return getTaskUseCase.getTask(id)
                .map(TaskResponseDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTask(@PathVariable String id) {
        return getTaskUseCase.getTask(TaskId.of(id))
                .map(TaskResponseDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
