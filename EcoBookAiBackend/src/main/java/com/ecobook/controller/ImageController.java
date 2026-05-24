package com.ecobook.controller;

import com.ecobook.service.ImageAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/images")
@RequiredArgsConstructor
@Tag(name = "Imagens", description = "Gateway autenticado para imagens promovidas de materiais")
@SecurityRequirement(name = "bearer-jwt")
public class ImageController {

    private final ImageAccessService imageAccessService;

    /**
     * Handles the get image request.
     *
     * @param imageId the imageId value
     * @param side the side value
     * @param authentication the authenticated principal context
     * @return the HTTP response for the request
     */
    @GetMapping("/{imageId}")
    @Operation(
            summary = "Baixar imagem autenticada",
            description = "Retorna a capa ou o verso de uma imagem promovida de material respeitando as regras de acesso do runtime."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem carregada com sucesso"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Usuario sem acesso a imagem"),
            @ApiResponse(responseCode = "404", description = "Imagem nao encontrada")
    })
    public ResponseEntity<Resource> getImage(@PathVariable @Parameter(description = "Identificador do upload tracking promovido") String imageId,
                                             @RequestParam(defaultValue = "front") @Parameter(description = "Lado da imagem a retornar: front ou back") String side,
                                             Authentication authentication) {
        ImageAccessService.ImagePayload payload = imageAccessService.loadImage(authentication.getName(), imageId, side);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.resource());
    }
}
