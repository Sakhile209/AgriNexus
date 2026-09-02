/*
 * Decompiled with CFR 0.152.
 */
package za.co.agrinexus.farm.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FarmResponse(UUID id, String name, String farmType, String province, String municipality, BigDecimal latitude, BigDecimal longitude, BigDecimal sizeValue, String sizeUnit, String mainActivities, Instant createdAt, Instant updatedAt) {
}
