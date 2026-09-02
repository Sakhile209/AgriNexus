/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package za.co.agrinexus.health.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.agrinexus.health.model.HealthRecord;

public interface HealthRecordRepository
extends JpaRepository<HealthRecord, UUID> {
    public List<HealthRecord> findAllByAnimalIdOrderByCreatedAtDesc(UUID var1);

    public List<HealthRecord> findAllByAnimalFarmIdAndAnimalFarmOwnerEmailOrderByCreatedAtDesc(UUID var1, String var2);

    public Optional<HealthRecord> findByIdAndAnimalFarmOwnerEmail(UUID var1, String var2);
}
