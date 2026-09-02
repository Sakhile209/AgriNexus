/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.DecimalMax
 *  jakarta.validation.constraints.DecimalMin
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.NotNull
 *  jakarta.validation.constraints.PastOrPresent
 *  jakarta.validation.constraints.Pattern
 *  jakarta.validation.constraints.Positive
 *  jakarta.validation.constraints.PositiveOrZero
 *  jakarta.validation.constraints.Size
 */
package za.co.agrinexus.crop.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import za.co.agrinexus.crop.model.CropStatus;

public final class CropDtos {
    private CropDtos() {
    }

    public record SoilResponse(UUID id, LocalDate recordedOn, String soilType, String moisture, BigDecimal ph, BigDecimal nitrogen, BigDecimal phosphorus, BigDecimal potassium, BigDecimal electricalConductivity, String laboratoryName, String notes) {
    }

    public record SoilRequest(@NotNull @PastOrPresent LocalDate recordedOn, @Size(max=100) @Size(max=100) String soilType, @Pattern(regexp="DRY|MOIST|WET") @Pattern(regexp="DRY|MOIST|WET") String moisture, @DecimalMin(value="0") @DecimalMax(value="14") @DecimalMin(value="0") @DecimalMax(value="14") BigDecimal ph, @PositiveOrZero BigDecimal nitrogen, @PositiveOrZero BigDecimal phosphorus, @PositiveOrZero BigDecimal potassium, @PositiveOrZero BigDecimal electricalConductivity, @Size(max=200) @Size(max=200) String laboratoryName, @Size(max=1000) @Size(max=1000) String notes) {
    }

    public record ActivityResponse(UUID id, String activityType, LocalDate activityDate, String details, String notes, Instant createdAt) {
    }

    public record ActivityRequest(@NotBlank @Pattern(regexp="PLANTING|IRRIGATION|FERTILIZER|PESTICIDE|WEED_CONTROL|INSPECTION|HARVESTING") @NotBlank @Pattern(regexp="PLANTING|IRRIGATION|FERTILIZER|PESTICIDE|WEED_CONTROL|INSPECTION|HARVESTING") String activityType, @NotNull @PastOrPresent LocalDate activityDate, @Size(max=1000) @Size(max=1000) String details, @Size(max=1000) @Size(max=1000) String notes) {
    }

    public record CropResponse(UUID id, String cropType, String variety, LocalDate plantingDate, LocalDate expectedHarvestDate, LocalDate actualHarvestDate, CropStatus status, String notes, Instant createdAt, Instant updatedAt) {
    }

    public record CropRequest(@NotBlank @Size(max=120) @NotBlank @Size(max=120) String cropType, @Size(max=120) @Size(max=120) String variety, @NotNull LocalDate plantingDate, LocalDate expectedHarvestDate, LocalDate actualHarvestDate, CropStatus status, @Size(max=1000) @Size(max=1000) String notes) {
    }

    public record FieldResponse(UUID id, String name, BigDecimal sizeValue, String sizeUnit, String soilType, String notes, Instant createdAt, Instant updatedAt) {
    }

    public record FieldRequest(@NotBlank @Size(max=150) @NotBlank @Size(max=150) String name, @Positive BigDecimal sizeValue, @Size(max=20) @Size(max=20) String sizeUnit, @Size(max=100) @Size(max=100) String soilType, @Size(max=1000) @Size(max=1000) String notes) {
    }
}
