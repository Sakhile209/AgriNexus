/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package za.co.agrinexus.health.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.agrinexus.health.model.HealthRecord;
import za.co.agrinexus.health.model.Treatment;
import za.co.agrinexus.health.model.Vaccination;
import za.co.agrinexus.health.repository.HealthRecordRepository;
import za.co.agrinexus.health.repository.TreatmentRepository;
import za.co.agrinexus.health.repository.VaccinationRepository;
import za.co.agrinexus.health.web.HealthDtos;
import za.co.agrinexus.livestock.model.Animal;
import za.co.agrinexus.livestock.service.LivestockService;
import za.co.agrinexus.shared.exception.ConflictException;
import za.co.agrinexus.shared.exception.ResourceNotFoundException;

@Service
public class HealthService {
    private final HealthRecordRepository health;
    private final TreatmentRepository treatments;
    private final VaccinationRepository vaccines;
    private final LivestockService livestock;

    public HealthService(HealthRecordRepository h, TreatmentRepository t, VaccinationRepository v, LivestockService l) {
        this.health = h;
        this.treatments = t;
        this.vaccines = v;
        this.livestock = l;
    }

    @Transactional
    public HealthDtos.HealthResponse addHealth(String e, UUID animalId, HealthDtos.HealthRequest r) {
        Animal a = this.livestock.find(e, animalId);
        return this.hr((HealthRecord)this.health.save(new HealthRecord(a, r.observedSymptoms().trim(), r.symptomsStartedOn(), r.veterinarianContacted(), r.veterinarianVisitOn(), r.followUpOn(), this.trim(r.notes()))));
    }

    @Transactional(readOnly=true)
    public List<HealthDtos.HealthResponse> health(String e, UUID id) {
        this.livestock.find(e, id);
        return this.health.findAllByAnimalIdOrderByCreatedAtDesc(id).stream().map(this::hr).toList();
    }

    @Transactional
    public HealthDtos.TreatmentResponse addTreatment(String e, UUID healthId, HealthDtos.TreatmentRequest r) {
        HealthRecord h = this.health.findByIdAndAnimalFarmOwnerEmail(healthId, e).orElseThrow(() -> new ResourceNotFoundException("Health record was not found."));
        return this.tr((Treatment)this.treatments.save(new Treatment(h, r.treatment().trim(), this.trim(r.medication()), r.treatmentDate(), this.trim(r.administeredBy()), this.trim(r.notes()))));
    }

    @Transactional(readOnly=true)
    public List<HealthDtos.TreatmentResponse> treatments(String e, UUID healthId) {
        this.health.findByIdAndAnimalFarmOwnerEmail(healthId, e).orElseThrow(() -> new ResourceNotFoundException("Health record was not found."));
        return this.treatments.findAllByHealthRecordIdOrderByTreatmentDateDesc(healthId).stream().map(this::tr).toList();
    }

    @Transactional
    public HealthDtos.VaccinationResponse addVaccination(String e, UUID animalId, HealthDtos.VaccinationRequest r) {
        Animal a = this.livestock.find(e, animalId);
        if (r.nextDueOn() != null && r.nextDueOn().isBefore(r.administeredOn())) {
            throw new ConflictException("Next vaccination date cannot be before administration date.");
        }
        return this.vr((Vaccination)this.vaccines.save(new Vaccination(a, r.vaccineName().trim(), r.administeredOn(), r.nextDueOn(), this.trim(r.administeredBy()), this.trim(r.notes()))));
    }

    @Transactional(readOnly=true)
    public List<HealthDtos.VaccinationResponse> vaccinations(String e, UUID id) {
        this.livestock.find(e, id);
        return this.vaccines.findAllByAnimalIdOrderByAdministeredOnDesc(id).stream().map(this::vr).toList();
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private HealthDtos.HealthResponse hr(HealthRecord h) {
        return new HealthDtos.HealthResponse(h.getId(), h.getObservedSymptoms(), h.getSymptomsStartedOn(), h.isVeterinarianContacted(), h.getVeterinarianVisitOn(), h.getFollowUpOn(), h.getNotes(), h.getCreatedAt());
    }

    private HealthDtos.TreatmentResponse tr(Treatment t) {
        return new HealthDtos.TreatmentResponse(t.getId(), t.getTreatment(), t.getMedication(), t.getTreatmentDate(), t.getAdministeredBy(), t.getNotes());
    }

    private HealthDtos.VaccinationResponse vr(Vaccination v) {
        return new HealthDtos.VaccinationResponse(v.getId(), v.getVaccineName(), v.getAdministeredOn(), v.getNextDueOn(), v.getAdministeredBy(), v.getNotes());
    }
}
