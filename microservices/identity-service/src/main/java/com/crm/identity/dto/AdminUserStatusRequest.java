package com.crm.identity.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserStatusRequest(@NotNull Boolean enabled) { }
