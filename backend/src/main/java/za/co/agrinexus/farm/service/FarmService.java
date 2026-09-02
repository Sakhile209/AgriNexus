/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package za.co.agrinexus.farm.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.agrinexus.auth.model.User;
import za.co.agrinexus.auth.repository.UserRepository;
import za.co.agrinexus.farm.dto.FarmRequest;
import za.co.agrinexus.farm.dto.FarmResponse;
import za.co.agrinexus.farm.model.Farm;
import za.co.agrinexus.farm.repository.FarmRepository;
import za.co.agrinexus.shared.exception.ResourceNotFoundException;

@Service
public class FarmService {
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;

    public FarmService(FarmRepository farmRepository, UserRepository userRepository) {
        this.farmRepository = farmRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FarmResponse create(String email, FarmRequest request) {
        User owner = this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Farmer profile was not found."));
        Farm farm = new Farm(owner, request.name().trim(), request.farmType().trim(), request.province().trim());
        this.apply(farm, request);
        return this.toResponse((Farm)this.farmRepository.save(farm));
    }

    @Transactional(readOnly=true)
    public List<FarmResponse> list(String email) {
        return this.farmRepository.findAllByOwnerEmailOrderByName(email).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly=true)
    public FarmResponse get(String email, UUID id) {
        return this.toResponse(this.findOwned(email, id));
    }

    @Transactional
    public FarmResponse update(String email, UUID id, FarmRequest request) {
        Farm farm = this.findOwned(email, id);
        this.apply(farm, request);
        return this.toResponse(farm);
    }

    public Farm findOwned(String email, UUID id) {
        return this.farmRepository.findByIdAndOwnerEmail(id, email).orElseThrow(() -> new ResourceNotFoundException("Farm was not found."));
    }

    private void apply(Farm farm, FarmRequest r) {
        farm.updateDetails(r.name().trim(), r.farmType().trim(), r.province().trim(), this.trim(r.municipality()), r.latitude(), r.longitude(), r.sizeValue(), this.trim(r.sizeUnit()), this.trim(r.mainActivities()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private FarmResponse toResponse(Farm f) {
        return new FarmResponse(f.getId(), f.getName(), f.getFarmType(), f.getProvince(), f.getMunicipality(), f.getLatitude(), f.getLongitude(), f.getSizeValue(), f.getSizeUnit(), f.getMainActivities(), f.getCreatedAt(), f.getUpdatedAt());
    }
}
