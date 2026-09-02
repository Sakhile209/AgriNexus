/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package za.co.agrinexus.health.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.agrinexus.health.model.Treatment;

public interface TreatmentRepository
extends JpaRepository<Treatment, UUID> {
    public List<Treatment> findAllByHealthRecordIdOrderByTreatmentDateDesc(UUID var1);
}
