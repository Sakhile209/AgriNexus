/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package za.co.agrinexus.livestock.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.agrinexus.livestock.model.Animal;
import za.co.agrinexus.livestock.model.AnimalStatus;

public interface AnimalRepository
extends JpaRepository<Animal, UUID> {
    public List<Animal> findAllByFarmIdAndFarmOwnerEmailOrderByInternalId(UUID var1, String var2);

    public Optional<Animal> findByIdAndFarmOwnerEmail(UUID var1, String var2);

    public boolean existsByFarmIdAndInternalIdIgnoreCase(UUID var1, String var2);

    public long countByFarmIdAndFarmOwnerEmailAndStatus(UUID var1, String var2, AnimalStatus var3);
}
