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
import za.co.agrinexus.health.model.HealthRecord;

@Entity
@Table(name="treatment")
public class Treatment {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="health_record_id")
    private HealthRecord healthRecord;
    @Column(nullable=false, length=500)
    private String treatment;
    @Column(length=300)
    private String medication;
    @Column(name="treatment_date", nullable=false)
    private LocalDate treatmentDate;
    @Column(name="administered_by", length=200)
    private String administeredBy;
    @Column(length=1000)
    private String notes;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    protected Treatment() {
    }

    public Treatment(HealthRecord h, String t, String m, LocalDate d, String by, String n) {
        this.healthRecord = h;
        this.treatment = t;
        this.medication = m;
        this.treatmentDate = d;
        this.administeredBy = by;
        this.notes = n;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getTreatment() {
        return this.treatment;
    }

    public String getMedication() {
        return this.medication;
    }

    public LocalDate getTreatmentDate() {
        return this.treatmentDate;
    }

    public String getAdministeredBy() {
        return this.administeredBy;
    }

    public String getNotes() {
        return this.notes;
    }
}
