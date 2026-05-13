package com.tenantcore.logiflowservice.reconciliation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.reconciliation.policy")
public record ReconciliationPolicyProperties(
        @DefaultValue("true") boolean enforceDriverAssignment,
        @DefaultValue("72") int maxCodAgeHours
) {
    public int normalizedMaxCodAgeHours() {
        return Math.max(maxCodAgeHours, 0);
    }
}
