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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import za.co.agrinexus.crop.model.Crop;

@Entity
@Table(name="crop_activity")
public class CropActivity {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="crop_id")
    private Crop crop;
    @Column(name="activity_type", nullable=false, length=50)
    private String activityType;
    @Column(name="activity_date", nullable=false)
    private LocalDate activityDate;
    @Column(length=1000)
    private String details;
    @Column(length=1000)
    private String notes;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    protected CropActivity() {
    }

    public CropActivity(Crop c, String t, LocalDate d, String details, String notes) {
        this.crop = c;
        this.activityType = t;
        this.activityDate = d;
        this.details = details;
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getActivityType() {
        return this.activityType;
    }

    public LocalDate getActivityDate() {
        return this.activityDate;
    }

    public String getDetails() {
        return this.details;
    }

    public String getNotes() {
        return this.notes;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
