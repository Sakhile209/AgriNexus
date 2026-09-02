/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  org.hibernate.annotations.UuidGenerator
 */
package za.co.agrinexus.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import za.co.agrinexus.auth.model.AccountStatus;
import za.co.agrinexus.auth.model.UserRole;

@Entity
@Table(name="app_user")
public class User {
    @Id
    @UuidGenerator
    private UUID id;
    @Column(name="first_name", nullable=false, length=100)
    private String firstName;
    @Column(name="last_name", nullable=false, length=100)
    private String lastName;
    @Column(nullable=false, unique=true, length=254)
    private String email;
    @Column(name="phone_number", nullable=false, length=30)
    private String phoneNumber;
    @Column(name="password_hash", nullable=false)
    private String passwordHash;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=30)
    private UserRole role = UserRole.FARMER;
    @Enumerated(value=EnumType.STRING)
    @Column(name="account_status", nullable=false, length=30)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String firstName, String lastName, String email, String phoneNumber, String passwordHash) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.updatedAt = this.createdAt = Instant.now();
    }

    public void updateProfile(String firstName, String lastName, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public UserRole getRole() {
        return this.role;
    }

    public AccountStatus getAccountStatus() {
        return this.accountStatus;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }
}
