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
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.agrinexus.crop.model.CropStatus;
import za.co.agrinexus.crop.repository.CropRepository;
import za.co.agrinexus.farm.dto.FarmResponse;
import za.co.agrinexus.farm.service.FarmService;
import za.co.agrinexus.health.repository.VaccinationRepository;
import za.co.agrinexus.livestock.model.Animal;
import za.co.agrinexus.livestock.model.AnimalStatus;
import za.co.agrinexus.livestock.repository.AnimalRepository;
import za.co.agrinexus.notification.repository.NotificationRepository;
import za.co.agrinexus.weather.service.WeatherService;

@Service
public class DashboardService {
    private final FarmService farms;
    private final AnimalRepository animals;
    private final CropRepository crops;
    private final VaccinationRepository vaccinations;
    private final NotificationRepository notifications;
    private final WeatherService weather;

    public DashboardService(FarmService f, AnimalRepository a, CropRepository c, VaccinationRepository v, NotificationRepository n, WeatherService w) {
        this.farms = f;
        this.animals = a;
        this.crops = c;
        this.vaccinations = v;
        this.notifications = n;
        this.weather = w;
    }

    @Transactional(readOnly=true)
    public Dashboard dashboard(String e, UUID farmId) {
        FarmResponse farm = this.farms.get(e, farmId);
        List<Animal> list = this.animals.findAllByFarmIdAndFarmOwnerEmailOrderByInternalId(farmId, e);
        Map species = list.stream().filter(a -> a.getStatus() == AnimalStatus.ACTIVE).collect(Collectors.groupingBy(Animal::getSpecies, TreeMap::new, Collectors.counting()));
        long active = list.stream().filter(a -> a.getStatus() == AnimalStatus.ACTIVE).count();
        long activeCrops = this.crops.countByFieldFarmIdAndFieldFarmOwnerEmailAndStatusIn(farmId, e, List.of(CropStatus.PLANNED, CropStatus.PLANTED, CropStatus.GROWING));
        long upcoming = this.vaccinations.countByAnimalFarmIdAndAnimalFarmOwnerEmailAndNextDueOnBetween(farmId, e, LocalDate.now(), LocalDate.now().plusDays(14L));
        return new Dashboard(farm.id(), farm.name(), active, species, activeCrops, upcoming, this.notifications.countByUserEmailAndFarmIdAndReadFalse(e, farmId), this.weather.weather(e, farmId));
    }

    public record Dashboard(UUID farmId, String farmName, long activeLivestock, Map<String, Long> livestockBySpecies, long activeCrops, long upcomingVaccinations, long unreadNotifications, WeatherService.WeatherResponse weather) {
    }
}
