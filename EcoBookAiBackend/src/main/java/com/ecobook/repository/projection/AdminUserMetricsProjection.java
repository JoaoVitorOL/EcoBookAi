package com.ecobook.repository.projection;

import java.util.UUID;

public interface AdminUserMetricsProjection {
    UUID getUserId();
    Long getMaterialsCount();
    Long getDonatedMaterialsCount();
    Long getRequestsCount();
    Long getCompletedRequestsCount();
    Long getOpenReportsCount();
}
