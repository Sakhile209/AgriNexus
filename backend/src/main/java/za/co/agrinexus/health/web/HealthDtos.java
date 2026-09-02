/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.NotNull
 *  jakarta.validation.constraints.PastOrPresent
 *  jakarta.validation.constraints.Size
 */
package za.co.agrinexus.health.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class HealthDtos {
    private HealthDtos() {
    }

    public record VaccinationResponse(UUID id, String vaccineName, LocalDate administeredOn, LocalDate nextDueOn, String administeredBy, String notes) {
    }

    public record VaccinationRequest(@NotBlank @Size(max=200) @NotBlank @Size(max=200) String vaccineName, @NotNull @PastOrPresent LocalDate administeredOn, LocalDate nextDueOn, @Size(max=200) @Size(max=200) String administeredBy, @Size(max=1000) @Size(max=1000) String notes) {
    }

    public record TreatmentResponse(UUID id, String treatment, String medication, LocalDate treatmentDate, String administeredBy, String notes) {
    }

    public record TreatmentRequest(@NotBlank @Size(max=500) @NotBlank @Size(max=500) String treatment, @Size(max=300) @Size(max=300) String medication, @NotNull @PastOrPresent LocalDate treatmentDate, @Size(max=200) @Size(max=200) String administeredBy, @Size(max=1000) @Size(max=1000) String notes) {
    }

    public record HealthResponse(UUID id, String observedSymptoms, LocalDate symptomsStartedOn, boolean veterinarianContacted, LocalDate veterinarianVisitOn, LocalDate followUpOn, String notes, Instant createdAt) {
    }

    public record HealthRequest(@NotBlank @Size(max=1500) @NotBlank @Size(max=1500) String observedSymptoms, @PastOrPresent LocalDate symptomsStartedOn, boolean veterinarianContacted, @PastOrPresent LocalDate veterinarianVisitOn, LocalDate followUpOn, @Size(max=1500) @Size(max=1500) String notes) {
    }
}
