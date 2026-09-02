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
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import za.co.agrinexus.crop.model.Field;

@Entity
@Table(name="soil_record")
public class SoilRecord {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="field_id")
    private Field field;
    @Column(name="recorded_on", nullable=false)
    private LocalDate recordedOn;
    @Column(name="soil_type", length=100)
    private String soilType;
    @Column(length=20)
    private String moisture;
    @Column(precision=4, scale=2)
    private BigDecimal ph;
    @Column(precision=12, scale=3)
    private BigDecimal nitrogen;
    @Column(precision=12, scale=3)
    private BigDecimal phosphorus;
    @Column(precision=12, scale=3)
    private BigDecimal potassium;
    @Column(name="electrical_conductivity", precision=12, scale=3)
    private BigDecimal electricalConductivity;
    @Column(name="laboratory_name", length=200)
    private String laboratoryName;
    @Column(length=1000)
    private String notes;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    protected SoilRecord() {
    }

    public SoilRecord(Field f, LocalDate d, String soil, String moisture, BigDecimal ph, BigDecimal n, BigDecimal p, BigDecimal k, BigDecimal ec, String lab, String notes) {
        this.field = f;
        this.recordedOn = d;
        this.soilType = soil;
        this.moisture = moisture;
        this.ph = ph;
        this.nitrogen = n;
        this.phosphorus = p;
        this.potassium = k;
        this.electricalConductivity = ec;
        this.laboratoryName = lab;
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public LocalDate getRecordedOn() {
        return this.recordedOn;
    }

    public String getSoilType() {
        return this.soilType;
    }

    public String getMoisture() {
        return this.moisture;
    }

    public BigDecimal getPh() {
        return this.ph;
    }

    public BigDecimal getNitrogen() {
        return this.nitrogen;
    }

    public BigDecimal getPhosphorus() {
        return this.phosphorus;
    }

    public BigDecimal getPotassium() {
        return this.potassium;
    }

    public BigDecimal getElectricalConductivity() {
        return this.electricalConductivity;
    }

    public String getLaboratoryName() {
        return this.laboratoryName;
    }

    public String getNotes() {
        return this.notes;
    }
}
