/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package za.co.agrinexus.livestock.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.agrinexus.livestock.model.AnimalEvent;

public interface AnimalEventRepository
extends JpaRepository<AnimalEvent, UUID> {
    public List<AnimalEvent> findAllByAnimalIdOrderByEventDateDesc(UUID var1);
}
