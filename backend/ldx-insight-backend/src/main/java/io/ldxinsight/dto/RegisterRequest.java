package io.ldxinsight.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotEmpty(message = "Username không được để trống")
    private String username;

    @NotEmpty(message = "Password không được để trống")
    private String password;
}