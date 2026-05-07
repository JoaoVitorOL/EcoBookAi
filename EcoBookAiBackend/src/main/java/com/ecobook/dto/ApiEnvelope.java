package com.ecobook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ApiEnvelope<T> {
    private final int status;
    private final String message;
    private final LocalDateTime timestamp;
    private final String path;
    private final T data;
}
