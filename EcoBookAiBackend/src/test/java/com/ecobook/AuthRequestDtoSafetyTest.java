package com.ecobook;

import com.ecobook.dto.LoginRequestDTO;
import com.ecobook.dto.RegisterRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestDtoSafetyTest {

    @Test
    @DisplayName("RegisterRequestDTO toString should not expose the password")
    void registerRequestToStringShouldHidePassword() {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .email("user@example.com")
                .password("SenhaSegura123")
                .nome("Test User")
                .build();

        assertThat(request.toString())
                .contains("user@example.com")
                .doesNotContain("SenhaSegura123")
                .doesNotContain("password=");
    }

    @Test
    @DisplayName("LoginRequestDTO toString should not expose the password")
    void loginRequestToStringShouldHidePassword() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("user@example.com")
                .password("SenhaSegura123")
                .build();

        assertThat(request.toString())
                .contains("user@example.com")
                .doesNotContain("SenhaSegura123")
                .doesNotContain("password=");
    }
}
