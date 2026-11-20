package com.crm.identity.dto;

import java.util.Set;
import com.crm.identity.enums.RoleType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AdminUserRolesRequest(@NotEmpty Set<@NotNull RoleType> roles) { }
