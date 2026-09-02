/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package za.co.agrinexus.auth.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.agrinexus.auth.model.User;

public interface UserRepository
extends JpaRepository<User, UUID> {
    public Optional<User> findByEmail(String var1);

    public boolean existsByEmail(String var1);
}
