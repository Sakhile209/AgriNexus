/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.NotNull
 *  jakarta.validation.constraints.PastOrPresent
 *  jakarta.validation.constraints.Pattern
 *  jakarta.validation.constraints.Positive
 *  jakarta.validation.constraints.PositiveOrZero
 *  jakarta.validation.constraints.Size
 */
package za.co.agrinexus.livestock.web;

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
import za.co.agrinexus.livestock.model.AnimalStatus;

public final class LivestockDtos {
    private LivestockDtos() {
    }

    public record EventResponse(UUID id, String eventType, LocalDate eventDate, BigDecimal weightKg, String fromLocation, String toLocation, String notes, Instant createdAt) {
    }

    public record EventRequest(@NotBlank @Pattern(regexp="BIRTH|DEATH|SALE|PURCHASE|WEIGHT|MOVEMENT|VACCINATION|TREATMENT|ILLNESS|VETERINARY_VISIT") @NotBlank @Pattern(regexp="BIRTH|DEATH|SALE|PURCHASE|WEIGHT|MOVEMENT|VACCINATION|TREATMENT|ILLNESS|VETERINARY_VISIT") String eventType, @NotNull @PastOrPresent LocalDate eventDate, @Positive BigDecimal weightKg, @Size(max=150) @Size(max=150) String fromLocation, @Size(max=150) @Size(max=150) String toLocation, @Size(max=1000) @Size(max=1000) String notes) {
    }

    public record AnimalResponse(UUID id, String internalId, String earTagNumber, String species, String breed, String sex, LocalDate dateOfBirth, Integer approximateAgeMonths, String colour, String identifyingMarkings, BigDecimal weightKg, AnimalStatus status, String notes, Instant createdAt, Instant updatedAt) {
    }

    public record AnimalRequest(@NotBlank @Size(max=80) @NotBlank @Size(max=80) String internalId, @Size(max=100) @Size(max=100) String earTagNumber, @NotBlank @Size(max=80) @NotBlank @Size(max=80) String species, @Size(max=100) @Size(max=100) String breed, @Size(max=20) @Size(max=20) String sex, @PastOrPresent LocalDate dateOfBirth, @PositiveOrZero Integer approximateAgeMonths, @Size(max=80) @Size(max=80) String colour, @Size(max=300) @Size(max=300) String identifyingMarkings, @Positive BigDecimal weightKg, AnimalStatus status, @Size(max=1000) @Size(max=1000) String notes) {
    }
}
