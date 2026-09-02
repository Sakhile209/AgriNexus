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
package za.co.agrinexus.livestock.model;

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
import za.co.agrinexus.livestock.model.Animal;

@Entity
@Table(name="animal_event")
public class AnimalEvent {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="animal_id")
    private Animal animal;
    @Column(name="event_type", nullable=false, length=40)
    private String eventType;
    @Column(name="event_date", nullable=false)
    private LocalDate eventDate;
    @Column(name="weight_kg", precision=10, scale=2)
    private BigDecimal weightKg;
    @Column(name="from_location", length=150)
    private String fromLocation;
    @Column(name="to_location", length=150)
    private String toLocation;
    @Column(length=1000)
    private String notes;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    protected AnimalEvent() {
    }

    public AnimalEvent(Animal a, String t, LocalDate d, BigDecimal w, String f, String to, String n) {
        this.animal = a;
        this.eventType = t;
        this.eventDate = d;
        this.weightKg = w;
        this.fromLocation = f;
        this.toLocation = to;
        this.notes = n;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getEventType() {
        return this.eventType;
    }

    public LocalDate getEventDate() {
        return this.eventDate;
    }

    public BigDecimal getWeightKg() {
        return this.weightKg;
    }

    public String getFromLocation() {
        return this.fromLocation;
    }

    public String getToLocation() {
        return this.toLocation;
    }

    public String getNotes() {
        return this.notes;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
