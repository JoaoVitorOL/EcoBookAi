package com.ecobook.event;

import java.util.UUID;

public record ProfileCompletedEvent(UUID userId, String email) {
}
