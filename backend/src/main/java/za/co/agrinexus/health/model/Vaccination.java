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
@Table(name="vaccination")
public class Vaccination {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="animal_id")
    private Animal animal;
    @Column(name="vaccine_name", nullable=false, length=200)
    private String vaccineName;
    @Column(name="administered_on", nullable=false)
    private LocalDate administeredOn;
    @Column(name="next_due_on")
    private LocalDate nextDueOn;
    @Column(name="administered_by", length=200)
    private String administeredBy;
    @Column(length=1000)
    private String notes;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    protected Vaccination() {
    }

    public Vaccination(Animal a, String v, LocalDate on, LocalDate next, String by, String n) {
        this.animal = a;
        this.vaccineName = v;
        this.administeredOn = on;
        this.nextDueOn = next;
        this.administeredBy = by;
        this.notes = n;
        this.updatedAt = this.createdAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getVaccineName() {
        return this.vaccineName;
    }

    public LocalDate getAdministeredOn() {
        return this.administeredOn;
    }

    public LocalDate getNextDueOn() {
        return this.nextDueOn;
    }

    public String getAdministeredBy() {
        return this.administeredBy;
    }

    public String getNotes() {
        return this.notes;
    }

    public Animal getAnimal() {
        return this.animal;
    }
}
