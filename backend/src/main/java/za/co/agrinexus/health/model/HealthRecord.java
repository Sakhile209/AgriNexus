/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.FetchType
 *  jakarta.persistence.Id
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.ManyToOne
 *  jakarta.persistence.Table
 *  org.hibernate.annotations.UuidGenerator
 */
package za.co.agrinexus.health.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import za.co.agrinexus.livestock.model.Animal;

@Entity
@Table(name="animal_health_record")
public class HealthRecord {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="animal_id")
    private Animal animal;
    @Column(name="observed_symptoms", nullable=false, length=1500)
    private String observedSymptoms;
    @Column(name="symptoms_started_on")
    private LocalDate symptomsStartedOn;
    @Column(name="veterinarian_contacted", nullable=false)
    private boolean veterinarianContacted;
    @Column(name="veterinarian_visit_on")
    private LocalDate veterinarianVisitOn;
    @Column(name="follow_up_on")
    private LocalDate followUpOn;
    @Column(length=1500)
    private String notes;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    protected HealthRecord() {
    }

    public HealthRecord(Animal a, String symptoms, LocalDate started, boolean contacted, LocalDate visit, LocalDate follow, String notes) {
        this.animal = a;
        this.observedSymptoms = symptoms;
        this.symptomsStartedOn = started;
        this.veterinarianContacted = contacted;
        this.veterinarianVisitOn = visit;
        this.followUpOn = follow;
        this.notes = notes;
        this.updatedAt = this.createdAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getObservedSymptoms() {
        return this.observedSymptoms;
    }

    public LocalDate getSymptomsStartedOn() {
        return this.symptomsStartedOn;
    }

    public boolean isVeterinarianContacted() {
        return this.veterinarianContacted;
    }

    public LocalDate getVeterinarianVisitOn() {
        return this.veterinarianVisitOn;
    }

    public LocalDate getFollowUpOn() {
        return this.followUpOn;
    }

    public String getNotes() {
        return this.notes;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
