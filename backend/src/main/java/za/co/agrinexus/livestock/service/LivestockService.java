/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package za.co.agrinexus.livestock.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.agrinexus.farm.model.Farm;
import za.co.agrinexus.farm.service.FarmService;
import za.co.agrinexus.livestock.model.Animal;
import za.co.agrinexus.livestock.model.AnimalEvent;
import za.co.agrinexus.livestock.model.AnimalStatus;
import za.co.agrinexus.livestock.repository.AnimalEventRepository;
import za.co.agrinexus.livestock.repository.AnimalRepository;
import za.co.agrinexus.livestock.web.LivestockDtos;
import za.co.agrinexus.shared.exception.ConflictException;
import za.co.agrinexus.shared.exception.ResourceNotFoundException;

@Service
public class LivestockService {
    private final AnimalRepository animals;
    private final AnimalEventRepository events;
    private final FarmService farms;

    public LivestockService(AnimalRepository a, AnimalEventRepository e, FarmService f) {
        this.animals = a;
        this.events = e;
        this.farms = f;
    }

    @Transactional
    public LivestockDtos.AnimalResponse create(String email, UUID farmId, LivestockDtos.AnimalRequest r) {
        Farm f = this.farms.findOwned(email, farmId);
        String internal = r.internalId().trim();
        if (this.animals.existsByFarmIdAndInternalIdIgnoreCase(farmId, internal)) {
            throw new ConflictException("Animal ID already exists on this farm.");
        }
        Animal a = new Animal(f, internal, r.species().trim());
        this.apply(a, r);
        return this.response((Animal)this.animals.save(a));
    }

    @Transactional(readOnly=true)
    public List<LivestockDtos.AnimalResponse> list(String e, UUID f) {
        this.farms.findOwned(e, f);
        return this.animals.findAllByFarmIdAndFarmOwnerEmailOrderByInternalId(f, e).stream().map(this::response).toList();
    }

    @Transactional(readOnly=true)
    public LivestockDtos.AnimalResponse get(String e, UUID id) {
        return this.response(this.find(e, id));
    }

    @Transactional
    public LivestockDtos.AnimalResponse update(String e, UUID id, LivestockDtos.AnimalRequest r) {
        Animal a = this.find(e, id);
        if (!a.getInternalId().equalsIgnoreCase(r.internalId())) {
            throw new ConflictException("Internal animal ID cannot be changed.");
        }
        this.apply(a, r);
        return this.response(a);
    }

    @Transactional
    public LivestockDtos.EventResponse addEvent(String e, UUID id, LivestockDtos.EventRequest r) {
        Animal a = this.find(e, id);
        AnimalEvent event = (AnimalEvent)this.events.save(new AnimalEvent(a, r.eventType(), r.eventDate(), r.weightKg(), this.trim(r.fromLocation()), this.trim(r.toLocation()), this.trim(r.notes())));
        if (r.eventType().equals("SALE")) {
            a.changeStatus(AnimalStatus.SOLD);
        }
        if (r.eventType().equals("DEATH")) {
            a.changeStatus(AnimalStatus.DECEASED);
        }
        return this.eventResponse(event);
    }

    @Transactional(readOnly=true)
    public List<LivestockDtos.EventResponse> events(String e, UUID id) {
        this.find(e, id);
        return this.events.findAllByAnimalIdOrderByEventDateDesc(id).stream().map(this::eventResponse).toList();
    }

    public Animal find(String email, UUID id) {
        return this.animals.findByIdAndFarmOwnerEmail(id, email).orElseThrow(() -> new ResourceNotFoundException("Animal was not found."));
    }

    private void apply(Animal a, LivestockDtos.AnimalRequest r) {
        a.update(this.trim(r.earTagNumber()), r.species().trim(), this.trim(r.breed()), this.trim(r.sex()), r.dateOfBirth(), r.approximateAgeMonths(), this.trim(r.colour()), this.trim(r.identifyingMarkings()), r.weightKg(), r.status() == null ? AnimalStatus.ACTIVE : r.status(), this.trim(r.notes()));
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private LivestockDtos.AnimalResponse response(Animal a) {
        return new LivestockDtos.AnimalResponse(a.getId(), a.getInternalId(), a.getEarTagNumber(), a.getSpecies(), a.getBreed(), a.getSex(), a.getDateOfBirth(), a.getApproximateAgeMonths(), a.getColour(), a.getIdentifyingMarkings(), a.getWeightKg(), a.getStatus(), a.getNotes(), a.getCreatedAt(), a.getUpdatedAt());
    }

    private LivestockDtos.EventResponse eventResponse(AnimalEvent x) {
        return new LivestockDtos.EventResponse(x.getId(), x.getEventType(), x.getEventDate(), x.getWeightKg(), x.getFromLocation(), x.getToLocation(), x.getNotes(), x.getCreatedAt());
    }
}
