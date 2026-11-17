package com.jadenf7.jobscheduler.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record TaskRequestDto(
        @NotBlank String taskType,
        Map<String, String> data
) {

}
