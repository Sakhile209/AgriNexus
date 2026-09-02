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
package za.co.agrinexus.crop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import za.co.agrinexus.crop.model.CropStatus;
import za.co.agrinexus.crop.model.Field;

@Entity
@Table(name="crop")
public class Crop {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="field_id")
    private Field field;
    @Column(name="crop_type", nullable=false, length=120)
    private String cropType;
    @Column(length=120)
    private String variety;
    @Column(name="planting_date", nullable=false)
    private LocalDate plantingDate;
    @Column(name="expected_harvest_date")
    private LocalDate expectedHarvestDate;
    @Column(name="actual_harvest_date")
    private LocalDate actualHarvestDate;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=30)
    private CropStatus status;
    @Column(length=1000)
    private String notes;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    protected Crop() {
    }

    public Crop(Field f, String type, LocalDate planted) {
        this.field = f;
        this.cropType = type;
        this.plantingDate = planted;
        this.status = CropStatus.PLANTED;
        this.updatedAt = this.createdAt = Instant.now();
    }

    public void update(String t, String v, LocalDate p, LocalDate e, LocalDate a, CropStatus s, String n) {
        this.cropType = t;
        this.variety = v;
        this.plantingDate = p;
        this.expectedHarvestDate = e;
        this.actualHarvestDate = a;
        this.status = s;
        this.notes = n;
        this.updatedAt = Instant.now();
    }

    public void harvest() {
        this.status = CropStatus.HARVESTED;
        this.actualHarvestDate = LocalDate.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getCropType() {
        return this.cropType;
    }

    public String getVariety() {
        return this.variety;
    }

    public LocalDate getPlantingDate() {
        return this.plantingDate;
    }

    public LocalDate getExpectedHarvestDate() {
        return this.expectedHarvestDate;
    }

    public LocalDate getActualHarvestDate() {
        return this.actualHarvestDate;
    }

    public CropStatus getStatus() {
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
}
