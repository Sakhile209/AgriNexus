/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package za.co.agrinexus.farm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.agrinexus.farm.model.Farm;

public interface FarmRepository
extends JpaRepository<Farm, UUID> {
    public List<Farm> findAllByOwnerEmailOrderByName(String var1);

    public Optional<Farm> findByIdAndOwnerEmail(UUID var1, String var2);
}
