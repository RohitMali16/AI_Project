package com.interviewprep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnswerRequest(
        @NotNull Long sessionId,
        @NotNull Integer questionIndex,
        @NotBlank @Size(max = 10000) String answer
) {}
