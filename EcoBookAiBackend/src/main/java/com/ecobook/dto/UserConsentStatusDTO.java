package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserConsentStatusDTO {
    private Boolean platformConsentGiven;
    private LocalDateTime platformConsentGivenAt;
    private Boolean aiConsentEnabled;
    private LocalDateTime aiConsentGivenAt;
    private LocalDateTime aiConsentRevokedAt;
    private List<ConsentRecordDTO> history;
}
