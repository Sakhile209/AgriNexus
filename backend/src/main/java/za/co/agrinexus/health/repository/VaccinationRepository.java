/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package za.co.agrinexus.health.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.agrinexus.health.model.Vaccination;

public interface VaccinationRepository
extends JpaRepository<Vaccination, UUID> {
    public List<Vaccination> findAllByAnimalIdOrderByAdministeredOnDesc(UUID var1);

    public List<Vaccination> findAllByAnimalFarmIdAndAnimalFarmOwnerEmailOrderByAdministeredOnDesc(UUID var1, String var2);

    public List<Vaccination> findAllByAnimalFarmIdAndAnimalFarmOwnerEmailAndNextDueOnBetween(UUID var1, String var2, LocalDate var3, LocalDate var4);

    public long countByAnimalFarmIdAndAnimalFarmOwnerEmailAndNextDueOnBetween(UUID var1, String var2, LocalDate var3, LocalDate var4);
}
