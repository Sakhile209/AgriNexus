/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.DecimalMax
 *  jakarta.validation.constraints.DecimalMin
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.Positive
 *  jakarta.validation.constraints.Size
 */
package za.co.agrinexus.farm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FarmRequest(@NotBlank @Size(max=150) @NotBlank @Size(max=150) String name, @NotBlank @Size(max=80) @NotBlank @Size(max=80) String farmType, @NotBlank @Size(max=80) @NotBlank @Size(max=80) String province, @Size(max=150) @Size(max=150) String municipality, @DecimalMin(value="-90") @DecimalMax(value="90") @DecimalMin(value="-90") @DecimalMax(value="90") BigDecimal latitude, @DecimalMin(value="-180") @DecimalMax(value="180") @DecimalMin(value="-180") @DecimalMax(value="180") BigDecimal longitude, @Positive BigDecimal sizeValue, @Size(max=20) @Size(max=20) String sizeUnit, @Size(max=500) @Size(max=500) String mainActivities) {
}
