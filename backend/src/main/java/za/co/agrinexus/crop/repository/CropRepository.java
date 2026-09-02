/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package za.co.agrinexus.crop.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.agrinexus.crop.model.Crop;
import za.co.agrinexus.crop.model.CropStatus;

public interface CropRepository
extends JpaRepository<Crop, UUID> {
    public List<Crop> findAllByFieldIdOrderByPlantingDateDesc(UUID var1);

    public List<Crop> findAllByFieldFarmIdAndFieldFarmOwnerEmailOrderByPlantingDateDesc(UUID var1, String var2);

    public Optional<Crop> findByIdAndFieldFarmOwnerEmail(UUID var1, String var2);

    public long countByFieldFarmIdAndFieldFarmOwnerEmailAndStatusIn(UUID var1, String var2, Collection<CropStatus> var3);
}
