/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package za.co.agrinexus.crop.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.agrinexus.crop.model.Crop;
import za.co.agrinexus.crop.model.CropActivity;
import za.co.agrinexus.crop.model.CropStatus;
import za.co.agrinexus.crop.model.Field;
import za.co.agrinexus.crop.model.SoilRecord;
import za.co.agrinexus.crop.repository.CropActivityRepository;
import za.co.agrinexus.crop.repository.CropRepository;
import za.co.agrinexus.crop.repository.FieldRepository;
import za.co.agrinexus.crop.repository.SoilRecordRepository;
import za.co.agrinexus.crop.web.CropDtos;
import za.co.agrinexus.farm.model.Farm;
import za.co.agrinexus.farm.service.FarmService;
import za.co.agrinexus.shared.exception.ConflictException;
import za.co.agrinexus.shared.exception.ResourceNotFoundException;

@Service
public class CropService {
    private final FieldRepository fields;
    private final CropRepository crops;
    private final CropActivityRepository activities;
    private final SoilRecordRepository soils;
    private final FarmService farms;

    public CropService(FieldRepository f, CropRepository c, CropActivityRepository a, SoilRecordRepository s, FarmService fs) {
        this.fields = f;
        this.crops = c;
        this.activities = a;
        this.soils = s;
        this.farms = fs;
    }

    @Transactional
    public CropDtos.FieldResponse createField(String e, UUID farmId, CropDtos.FieldRequest r) {
        Farm farm = this.farms.findOwned(e, farmId);
        if (this.fields.existsByFarmIdAndNameIgnoreCase(farmId, r.name().trim())) {
            throw new ConflictException("Field name already exists on this farm.");
        }
        Field f = new Field(farm, r.name().trim());
        f.update(r.name().trim(), r.sizeValue(), this.trim(r.sizeUnit()), this.trim(r.soilType()), this.trim(r.notes()));
        return this.fr((Field)this.fields.save(f));
    }

    @Transactional(readOnly=true)
    public List<CropDtos.FieldResponse> fields(String e, UUID farm) {
        this.farms.findOwned(e, farm);
        return this.fields.findAllByFarmIdAndFarmOwnerEmailOrderByName(farm, e).stream().map(this::fr).toList();
    }

    @Transactional(readOnly=true)
    public CropDtos.FieldResponse field(String e, UUID id) {
        return this.fr(this.findField(e, id));
    }

    @Transactional
    public CropDtos.FieldResponse updateField(String e, UUID id, CropDtos.FieldRequest r) {
        Field f = this.findField(e, id);
        f.update(r.name().trim(), r.sizeValue(), this.trim(r.sizeUnit()), this.trim(r.soilType()), this.trim(r.notes()));
        return this.fr(f);
    }

    @Transactional
    public CropDtos.CropResponse createCrop(String e, UUID fieldId, CropDtos.CropRequest r) {
        Field f = this.findField(e, fieldId);
        this.validateDates(r);
        Crop c = new Crop(f, r.cropType().trim(), r.plantingDate());
        this.apply(c, r);
        return this.cr((Crop)this.crops.save(c));
    }

    @Transactional(readOnly=true)
    public List<CropDtos.CropResponse> crops(String e, UUID field) {
        this.findField(e, field);
        return this.crops.findAllByFieldIdOrderByPlantingDateDesc(field).stream().map(this::cr).toList();
    }

    @Transactional(readOnly=true)
    public CropDtos.CropResponse crop(String e, UUID id) {
        return this.cr(this.findCrop(e, id));
    }

    @Transactional
    public CropDtos.CropResponse updateCrop(String e, UUID id, CropDtos.CropRequest r) {
        this.validateDates(r);
        Crop c = this.findCrop(e, id);
        this.apply(c, r);
        return this.cr(c);
    }

    @Transactional
    public CropDtos.ActivityResponse addActivity(String e, UUID cropId, CropDtos.ActivityRequest r) {
        Crop c = this.findCrop(e, cropId);
        CropActivity a = (CropActivity)this.activities.save(new CropActivity(c, r.activityType(), r.activityDate(), this.trim(r.details()), this.trim(r.notes())));
        if (r.activityType().equals("HARVESTING")) {
            c.harvest();
        }
        return this.ar(a);
    }

    @Transactional(readOnly=true)
    public List<CropDtos.ActivityResponse> activities(String e, UUID crop) {
        this.findCrop(e, crop);
        return this.activities.findAllByCropIdOrderByActivityDateDesc(crop).stream().map(this::ar).toList();
    }

    @Transactional
    public CropDtos.SoilResponse addSoil(String e, UUID fieldId, CropDtos.SoilRequest r) {
        Field f = this.findField(e, fieldId);
        return this.sr((SoilRecord)this.soils.save(new SoilRecord(f, r.recordedOn(), this.trim(r.soilType()), r.moisture(), r.ph(), r.nitrogen(), r.phosphorus(), r.potassium(), r.electricalConductivity(), this.trim(r.laboratoryName()), this.trim(r.notes()))));
    }

    @Transactional(readOnly=true)
    public List<CropDtos.SoilResponse> soils(String e, UUID field) {
        this.findField(e, field);
        return this.soils.findAllByFieldIdOrderByRecordedOnDesc(field).stream().map(this::sr).toList();
    }

    public Field findField(String e, UUID id) {
        return this.fields.findByIdAndFarmOwnerEmail(id, e).orElseThrow(() -> new ResourceNotFoundException("Field was not found."));
    }

    public Crop findCrop(String e, UUID id) {
        return this.crops.findByIdAndFieldFarmOwnerEmail(id, e).orElseThrow(() -> new ResourceNotFoundException("Crop was not found."));
    }

    private void validateDates(CropDtos.CropRequest r) {
        if (r.expectedHarvestDate() != null && r.expectedHarvestDate().isBefore(r.plantingDate())) {
            throw new ConflictException("Expected harvest cannot be before planting.");
        }
        if (r.actualHarvestDate() != null && r.actualHarvestDate().isBefore(r.plantingDate())) {
            throw new ConflictException("Actual harvest cannot be before planting.");
        }
    }

    private void apply(Crop c, CropDtos.CropRequest r) {
        c.update(r.cropType().trim(), this.trim(r.variety()), r.plantingDate(), r.expectedHarvestDate(), r.actualHarvestDate(), r.status() == null ? CropStatus.PLANTED : r.status(), this.trim(r.notes()));
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private CropDtos.FieldResponse fr(Field f) {
        return new CropDtos.FieldResponse(f.getId(), f.getName(), f.getSizeValue(), f.getSizeUnit(), f.getSoilType(), f.getNotes(), f.getCreatedAt(), f.getUpdatedAt());
    }

    private CropDtos.CropResponse cr(Crop c) {
        return new CropDtos.CropResponse(c.getId(), c.getCropType(), c.getVariety(), c.getPlantingDate(), c.getExpectedHarvestDate(), c.getActualHarvestDate(), c.getStatus(), c.getNotes(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private CropDtos.ActivityResponse ar(CropActivity a) {
        return new CropDtos.ActivityResponse(a.getId(), a.getActivityType(), a.getActivityDate(), a.getDetails(), a.getNotes(), a.getCreatedAt());
    }

    private CropDtos.SoilResponse sr(SoilRecord s) {
        return new CropDtos.SoilResponse(s.getId(), s.getRecordedOn(), s.getSoilType(), s.getMoisture(), s.getPh(), s.getNitrogen(), s.getPhosphorus(), s.getPotassium(), s.getElectricalConductivity(), s.getLaboratoryName(), s.getNotes());
    }
}
