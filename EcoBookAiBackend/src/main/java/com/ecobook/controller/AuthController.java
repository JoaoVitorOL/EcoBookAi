package com.ecobook.controller;

import com.ecobook.dto.ApiEnvelope;
import com.ecobook.dto.ApiEnvelopeResponses;
import com.ecobook.dto.AuthResponseDTO;
import com.ecobook.dto.LoginRequestDTO;
import com.ecobook.dto.RegisterRequestDTO;
import com.ecobook.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Cadastro e login do usuario com email, senha e JWT")
public class AuthController {

    private final AuthService authService;

    /**
     * Handles the register request.
     *
     * @param request the request payload
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PostMapping("/register")
    @Operation(
            summary = "Criar conta",
            description = "Cria uma conta com email e senha e retorna o JWT inicial do usuario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Email invalido ou payload mal formatado"),
            @ApiResponse(responseCode = "409", description = "Email ja cadastrado")
    })
    public ResponseEntity<ApiEnvelope<AuthResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request,
                                                                HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.created(
                servletRequest,
                "Conta criada com sucesso",
                authService.register(request)
        );
    }

    /**
     * Handles the login request.
     *
     * @param request the request payload
     * @param servletRequest the current HTTP request
     * @return the HTTP response for the request
     */
    @PostMapping("/login")
    @Operation(
            summary = "Autenticar usuario",
            description = "Autentica o usuario com email e senha e retorna um novo JWT com o snapshot atual do perfil."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticacao realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas")
    })
    public ResponseEntity<ApiEnvelope<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request,
                                                             HttpServletRequest servletRequest) {
        return ApiEnvelopeResponses.ok(
                servletRequest,
                "Autenticacao realizada com sucesso",
                authService.login(request)
        );
    }
}
