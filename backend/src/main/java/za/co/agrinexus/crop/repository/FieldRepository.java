/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package za.co.agrinexus.crop.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.agrinexus.crop.model.Field;

public interface FieldRepository
extends JpaRepository<Field, UUID> {
    public List<Field> findAllByFarmIdAndFarmOwnerEmailOrderByName(UUID var1, String var2);

    public Optional<Field> findByIdAndFarmOwnerEmail(UUID var1, String var2);

    public boolean existsByFarmIdAndNameIgnoreCase(UUID var1, String var2);
}
