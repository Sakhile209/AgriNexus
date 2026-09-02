/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package za.co.agrinexus.reporting.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.agrinexus.crop.model.Crop;
import za.co.agrinexus.crop.repository.CropRepository;
import za.co.agrinexus.farm.service.FarmService;
import za.co.agrinexus.health.model.HealthRecord;
import za.co.agrinexus.health.model.Vaccination;
import za.co.agrinexus.health.repository.HealthRecordRepository;
import za.co.agrinexus.health.repository.VaccinationRepository;
import za.co.agrinexus.livestock.model.Animal;
import za.co.agrinexus.livestock.repository.AnimalRepository;

@Service
public class ReportService {
    private final FarmService farms;
    private final AnimalRepository animals;
    private final HealthRecordRepository health;
    private final VaccinationRepository vaccines;
    private final CropRepository crops;

    public ReportService(FarmService f, AnimalRepository a, HealthRecordRepository h, VaccinationRepository v, CropRepository c) {
        this.farms = f;
        this.animals = a;
        this.health = h;
        this.vaccines = v;
        this.crops = c;
    }

    @Transactional(readOnly=true)
    public LivestockReport livestock(String e, UUID f) {
        this.farms.findOwned(e, f);
        List<Animal> a = this.animals.findAllByFarmIdAndFarmOwnerEmailOrderByInternalId(f, e);
        return new LivestockReport(a.size(), a.stream().collect(Collectors.groupingBy(x -> x.getStatus().name(), Collectors.counting())), a.stream().collect(Collectors.groupingBy(Animal::getSpecies, Collectors.counting())));
    }

    @Transactional(readOnly=true)
    public HealthReport health(String e, UUID f) {
        this.farms.findOwned(e, f);
        List<HealthRecord> h = this.health.findAllByAnimalFarmIdAndAnimalFarmOwnerEmailOrderByCreatedAtDesc(f, e);
        List<Vaccination> v = this.vaccines.findAllByAnimalFarmIdAndAnimalFarmOwnerEmailOrderByAdministeredOnDesc(f, e);
        return new HealthReport(h.size(), v.size(), v.stream().filter(x -> x.getNextDueOn() != null && x.getNextDueOn().isBefore(LocalDate.now())).count());
    }

    @Transactional(readOnly=true)
    public CropReport crops(String e, UUID f) {
        this.farms.findOwned(e, f);
        List<Crop> c = this.crops.findAllByFieldFarmIdAndFieldFarmOwnerEmailOrderByPlantingDateDesc(f, e);
        return new CropReport(c.size(), c.stream().collect(Collectors.groupingBy(x -> x.getStatus().name(), Collectors.counting())));
    }

    public record LivestockReport(long total, Map<String, Long> byStatus, Map<String, Long> bySpecies) {
    }

    public record HealthReport(long healthRecords, long vaccinations, long overdueVaccinations) {
    }

    public record CropReport(long total, Map<String, Long> byStatus) {
    }
}
