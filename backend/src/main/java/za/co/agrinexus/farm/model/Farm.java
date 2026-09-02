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
package za.co.agrinexus.farm.model;

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
import za.co.agrinexus.auth.model.User;

@Entity
@Table(name="farm")
public class Farm {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="owner_id")
    private User owner;
    @Column(nullable=false, length=150)
    private String name;
    @Column(name="farm_type", nullable=false, length=80)
    private String farmType;
    @Column(nullable=false, length=80)
    private String province;
    @Column(length=150)
    private String municipality;
    @Column(precision=9, scale=6)
    private BigDecimal latitude;
    @Column(precision=9, scale=6)
    private BigDecimal longitude;
    @Column(name="size_value", precision=12, scale=2)
    private BigDecimal sizeValue;
    @Column(name="size_unit", length=20)
    private String sizeUnit;
    @Column(name="main_activities", length=500)
    private String mainActivities;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    protected Farm() {
    }

    public Farm(User owner, String name, String farmType, String province) {
        this.owner = owner;
        this.name = name;
        this.farmType = farmType;
        this.province = province;
        this.updatedAt = this.createdAt = Instant.now();
    }

    public void updateDetails(String name, String farmType, String province, String municipality, BigDecimal latitude, BigDecimal longitude, BigDecimal sizeValue, String sizeUnit, String mainActivities) {
        this.name = name;
        this.farmType = farmType;
        this.province = province;
        this.municipality = municipality;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sizeValue = sizeValue;
        this.sizeUnit = sizeUnit;
        this.mainActivities = mainActivities;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getFarmType() {
        return this.farmType;
    }

    public String getProvince() {
        return this.province;
    }

    public String getMunicipality() {
        return this.municipality;
    }

    public BigDecimal getLatitude() {
        return this.latitude;
    }

    public BigDecimal getLongitude() {
        return this.longitude;
    }

    public BigDecimal getSizeValue() {
        return this.sizeValue;
    }

    public String getSizeUnit() {
        return this.sizeUnit;
    }

    public String getMainActivities() {
        return this.mainActivities;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public User getOwner() {
        return this.owner;
    }
}
