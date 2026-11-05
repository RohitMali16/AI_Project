package com.interviewprep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicRequest(@NotBlank @Size(max = 2000) String topic) {}
