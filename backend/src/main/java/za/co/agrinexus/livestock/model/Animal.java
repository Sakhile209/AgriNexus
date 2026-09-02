/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.FetchType
 *  jakarta.persistence.Id
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.ManyToOne
 *  jakarta.persistence.Table
 *  org.hibernate.annotations.UuidGenerator
 */
package za.co.agrinexus.livestock.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import za.co.agrinexus.farm.model.Farm;
import za.co.agrinexus.livestock.model.AnimalStatus;

@Entity
@Table(name="animal")
public class Animal {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="farm_id")
    private Farm farm;
    @Column(name="internal_id", nullable=false, length=80)
    private String internalId;
    @Column(name="ear_tag_number", length=100)
    private String earTagNumber;
    @Column(nullable=false, length=80)
    private String species;
    @Column(length=100)
    private String breed;
    @Column(length=20)
    private String sex;
    @Column(name="date_of_birth")
    private LocalDate dateOfBirth;
    @Column(name="approximate_age_months")
    private Integer approximateAgeMonths;
    @Column(length=80)
    private String colour;
    @Column(name="identifying_markings", length=300)
    private String identifyingMarkings;
    @Column(name="weight_kg", precision=10, scale=2)
    private BigDecimal weightKg;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=30)
    private AnimalStatus status;
    @Column(length=1000)
    private String notes;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    protected Animal() {
    }

    public Animal(Farm farm, String internalId, String species) {
        this.farm = farm;
        this.internalId = internalId;
        this.species = species;
        this.status = AnimalStatus.ACTIVE;
        this.updatedAt = this.createdAt = Instant.now();
    }

    public void update(String earTagNumber, String species, String breed, String sex, LocalDate dateOfBirth, Integer approximateAgeMonths, String colour, String markings, BigDecimal weightKg, AnimalStatus status, String notes) {
        this.earTagNumber = earTagNumber;
        this.species = species;
        this.breed = breed;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
        this.approximateAgeMonths = approximateAgeMonths;
        this.colour = colour;
        this.identifyingMarkings = markings;
        this.weightKg = weightKg;
        this.status = status;
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(AnimalStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getInternalId() {
        return this.internalId;
    }

    public String getEarTagNumber() {
        return this.earTagNumber;
    }

    public String getSpecies() {
        return this.species;
    }

    public String getBreed() {
        return this.breed;
    }

    public String getSex() {
        return this.sex;
    }

    public LocalDate getDateOfBirth() {
        return this.dateOfBirth;
    }

    public Integer getApproximateAgeMonths() {
        return this.approximateAgeMonths;
    }

    public String getColour() {
        return this.colour;
    }

    public String getIdentifyingMarkings() {
        return this.identifyingMarkings;
    }

    public BigDecimal getWeightKg() {
        return this.weightKg;
    }

    public AnimalStatus getStatus() {
        return this.status;
    }

    public String getNotes() {
        return this.notes;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Farm getFarm() {
        return this.farm;
    }
}
