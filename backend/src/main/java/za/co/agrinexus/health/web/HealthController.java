/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package za.co.agrinexus.health.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.agrinexus.health.service.HealthService;
import za.co.agrinexus.health.web.HealthDtos;

@RestController
@RequestMapping(value={"/api/v1"})
public class HealthController {
    private final HealthService service;

    public HealthController(HealthService s) {
        this.service = s;
    }

    @PostMapping(value={"/animals/{id}/health-records"})
    HealthDtos.HealthResponse addHealth(Authentication a, @PathVariable UUID id, @Valid @RequestBody HealthDtos.HealthRequest r) {
        return this.service.addHealth(a.getName(), id, r);
    }

    @GetMapping(value={"/animals/{id}/health-records"})
    List<HealthDtos.HealthResponse> health(Authentication a, @PathVariable UUID id) {
        return this.service.health(a.getName(), id);
    }

    @PostMapping(value={"/health-records/{id}/treatments"})
    HealthDtos.TreatmentResponse treatment(Authentication a, @PathVariable UUID id, @Valid @RequestBody HealthDtos.TreatmentRequest r) {
        return this.service.addTreatment(a.getName(), id, r);
    }

    @GetMapping(value={"/health-records/{id}/treatments"})
    List<HealthDtos.TreatmentResponse> treatments(Authentication a, @PathVariable UUID id) {
        return this.service.treatments(a.getName(), id);
    }

    @PostMapping(value={"/animals/{id}/vaccinations"})
    HealthDtos.VaccinationResponse vaccination(Authentication a, @PathVariable UUID id, @Valid @RequestBody HealthDtos.VaccinationRequest r) {
        return this.service.addVaccination(a.getName(), id, r);
    }

    @GetMapping(value={"/animals/{id}/vaccinations"})
    List<HealthDtos.VaccinationResponse> vaccinations(Authentication a, @PathVariable UUID id) {
        return this.service.vaccinations(a.getName(), id);
    }
}
