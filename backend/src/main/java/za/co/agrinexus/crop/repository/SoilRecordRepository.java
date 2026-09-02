/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package za.co.agrinexus.crop.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.agrinexus.crop.model.SoilRecord;

public interface SoilRecordRepository
extends JpaRepository<SoilRecord, UUID> {
    public List<SoilRecord> findAllByFieldIdOrderByRecordedOnDesc(UUID var1);
}
