package com.ecobook.controller;

import com.ecobook.service.ImageAccessService;
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
public class ImageController {

    private final ImageAccessService imageAccessService;

    @GetMapping("/{imageId}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageId,
                                             @RequestParam(defaultValue = "front") String side,
                                             Authentication authentication) {
        ImageAccessService.ImagePayload payload = imageAccessService.loadImage(authentication.getName(), imageId, side);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.resource());
    }
}
