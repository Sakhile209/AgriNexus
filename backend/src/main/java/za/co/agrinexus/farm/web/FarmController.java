/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PatchMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package za.co.agrinexus.farm.web;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.agrinexus.farm.dto.FarmRequest;
import za.co.agrinexus.farm.dto.FarmResponse;
import za.co.agrinexus.farm.service.FarmService;

@RestController
@RequestMapping(value={"/api/v1/farms"})
public class FarmController {
    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    @PostMapping
    ResponseEntity<FarmResponse> create(Authentication auth, @Valid @RequestBody FarmRequest request) {
        FarmResponse response = this.farmService.create(auth.getName(), request);
        return ResponseEntity.created((URI)URI.create("/api/v1/farms/" + String.valueOf(response.id()))).body(response);
    }

    @GetMapping
    ResponseEntity<List<FarmResponse>> list(Authentication auth) {
        return ResponseEntity.ok(this.farmService.list(auth.getName()));
    }

    @GetMapping(value={"/{id}"})
    ResponseEntity<FarmResponse> get(Authentication auth, @PathVariable UUID id) {
        return ResponseEntity.ok(this.farmService.get(auth.getName(), id));
    }

    @PatchMapping(value={"/{id}"})
    ResponseEntity<FarmResponse> update(Authentication auth, @PathVariable UUID id, @Valid @RequestBody FarmRequest request) {
        return ResponseEntity.ok(this.farmService.update(auth.getName(), id, request));
    }
}
