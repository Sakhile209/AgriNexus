/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PatchMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package za.co.agrinexus.crop.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.agrinexus.crop.service.CropService;
import za.co.agrinexus.crop.web.CropDtos;

@RestController
@RequestMapping(value={"/api/v1"})
public class CropController {
    private final CropService s;

    public CropController(CropService s) {
        this.s = s;
    }

    @PostMapping(value={"/farms/{id}/fields"})
    CropDtos.FieldResponse createField(Authentication a, @PathVariable UUID id, @Valid @RequestBody CropDtos.FieldRequest r) {
        return this.s.createField(a.getName(), id, r);
    }

    @GetMapping(value={"/farms/{id}/fields"})
    List<CropDtos.FieldResponse> fields(Authentication a, @PathVariable UUID id) {
        return this.s.fields(a.getName(), id);
    }

    @GetMapping(value={"/fields/{id}"})
    CropDtos.FieldResponse field(Authentication a, @PathVariable UUID id) {
        return this.s.field(a.getName(), id);
    }

    @PatchMapping(value={"/fields/{id}"})
    CropDtos.FieldResponse updateField(Authentication a, @PathVariable UUID id, @Valid @RequestBody CropDtos.FieldRequest r) {
        return this.s.updateField(a.getName(), id, r);
    }

    @PostMapping(value={"/fields/{id}/crops"})
    CropDtos.CropResponse createCrop(Authentication a, @PathVariable UUID id, @Valid @RequestBody CropDtos.CropRequest r) {
        return this.s.createCrop(a.getName(), id, r);
    }

    @GetMapping(value={"/fields/{id}/crops"})
    List<CropDtos.CropResponse> crops(Authentication a, @PathVariable UUID id) {
        return this.s.crops(a.getName(), id);
    }

    @GetMapping(value={"/crops/{id}"})
    CropDtos.CropResponse crop(Authentication a, @PathVariable UUID id) {
        return this.s.crop(a.getName(), id);
    }

    @PatchMapping(value={"/crops/{id}"})
    CropDtos.CropResponse updateCrop(Authentication a, @PathVariable UUID id, @Valid @RequestBody CropDtos.CropRequest r) {
        return this.s.updateCrop(a.getName(), id, r);
    }

    @PostMapping(value={"/crops/{id}/activities"})
    CropDtos.ActivityResponse activity(Authentication a, @PathVariable UUID id, @Valid @RequestBody CropDtos.ActivityRequest r) {
        return this.s.addActivity(a.getName(), id, r);
    }

    @GetMapping(value={"/crops/{id}/activities"})
    List<CropDtos.ActivityResponse> activities(Authentication a, @PathVariable UUID id) {
        return this.s.activities(a.getName(), id);
    }

    @PostMapping(value={"/fields/{id}/soil-records"})
    CropDtos.SoilResponse soil(Authentication a, @PathVariable UUID id, @Valid @RequestBody CropDtos.SoilRequest r) {
        return this.s.addSoil(a.getName(), id, r);
    }

    @GetMapping(value={"/fields/{id}/soil-records"})
    List<CropDtos.SoilResponse> soils(Authentication a, @PathVariable UUID id) {
        return this.s.soils(a.getName(), id);
    }
}
