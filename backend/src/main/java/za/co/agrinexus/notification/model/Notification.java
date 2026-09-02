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
package za.co.agrinexus.notification.model;

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
import za.co.agrinexus.auth.model.User;
import za.co.agrinexus.farm.model.Farm;

@Entity
@Table(name="notification")
public class Notification {
    @Id
    @UuidGenerator
    private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id")
    private User user;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="farm_id")
    private Farm farm;
    @Column(name="notification_type", nullable=false, length=40)
    private String type;
    @Column(nullable=false, length=200)
    private String title;
    @Column(nullable=false, length=1000)
    private String message;
    @Column(name="due_on")
    private LocalDate dueOn;
    @Column(name="source_key", nullable=false, length=250)
    private String sourceKey;
    @Column(name="related_path", length=500)
    private String relatedPath;
    @Column(name="is_read", nullable=false)
    private boolean read;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(User u, Farm f, String type, String title, String message, LocalDate due, String key, String path) {
        this.user = u;
        this.farm = f;
        this.type = type;
        this.title = title;
        this.message = message;
        this.dueOn = due;
        this.sourceKey = key;
        this.relatedPath = path;
        this.createdAt = Instant.now();
    }

    public void markRead() {
        this.read = true;
    }

    public UUID getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public String getTitle() {
        return this.title;
    }

    public String getMessage() {
        return this.message;
    }

    public LocalDate getDueOn() {
        return this.dueOn;
    }

    public String getRelatedPath() {
        return this.relatedPath;
    }

    public boolean isRead() {
        return this.read;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
