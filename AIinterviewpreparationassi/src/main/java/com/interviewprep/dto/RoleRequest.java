package com.interviewprep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleRequest(@NotBlank @Size(max = 100) String role) {}
