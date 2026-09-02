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
package za.co.agrinexus.crop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import za.co.agrinexus.farm.model.Farm;

@Entity
@Table(name="field")
public class Field {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="farm_id")
    private Farm farm;
    @Column(nullable=false, length=150)
    private String name;
    @Column(name="size_value", precision=12, scale=2)
    private BigDecimal sizeValue;
    @Column(name="size_unit", length=20)
    private String sizeUnit;
    @Column(name="soil_type", length=100)
    private String soilType;
    @Column(length=1000)
    private String notes;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    protected Field() {
    }

    public Field(Farm f, String n) {
        this.farm = f;
        this.name = n;
        this.updatedAt = this.createdAt = Instant.now();
    }

    public void update(String n, BigDecimal s, String u, String soil, String notes) {
        this.name = n;
        this.sizeValue = s;
        this.sizeUnit = u;
        this.soilType = soil;
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public BigDecimal getSizeValue() {
        return this.sizeValue;
    }

    public String getSizeUnit() {
        return this.sizeUnit;
    }

    public String getSoilType() {
        return this.soilType;
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
