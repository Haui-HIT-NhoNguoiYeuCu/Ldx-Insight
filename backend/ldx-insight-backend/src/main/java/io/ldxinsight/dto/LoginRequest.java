package io.ldxinsight.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginRequest {
    @NotEmpty
    private String username;

    @NotEmpty
    private String password;
}