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
package za.co.agrinexus.livestock.web;

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
import za.co.agrinexus.livestock.service.LivestockService;
import za.co.agrinexus.livestock.web.LivestockDtos;

@RestController
@RequestMapping(value={"/api/v1"})
public class LivestockController {
    private final LivestockService service;

    public LivestockController(LivestockService s) {
        this.service = s;
    }

    @PostMapping(value={"/farms/{farmId}/animals"})
    ResponseEntity<LivestockDtos.AnimalResponse> create(Authentication a, @PathVariable UUID farmId, @Valid @RequestBody LivestockDtos.AnimalRequest r) {
        LivestockDtos.AnimalResponse x = this.service.create(a.getName(), farmId, r);
        return ResponseEntity.created((URI)URI.create("/api/v1/animals/" + String.valueOf(x.id()))).body(x);
    }

    @GetMapping(value={"/farms/{farmId}/animals"})
    List<LivestockDtos.AnimalResponse> list(Authentication a, @PathVariable UUID farmId) {
        return this.service.list(a.getName(), farmId);
    }

    @GetMapping(value={"/animals/{id}"})
    LivestockDtos.AnimalResponse get(Authentication a, @PathVariable UUID id) {
        return this.service.get(a.getName(), id);
    }

    @PatchMapping(value={"/animals/{id}"})
    LivestockDtos.AnimalResponse update(Authentication a, @PathVariable UUID id, @Valid @RequestBody LivestockDtos.AnimalRequest r) {
        return this.service.update(a.getName(), id, r);
    }

    @PostMapping(value={"/animals/{id}/events"})
    LivestockDtos.EventResponse event(Authentication a, @PathVariable UUID id, @Valid @RequestBody LivestockDtos.EventRequest r) {
        return this.service.addEvent(a.getName(), id, r);
    }

    @GetMapping(value={"/animals/{id}/events"})
    List<LivestockDtos.EventResponse> events(Authentication a, @PathVariable UUID id) {
        return this.service.events(a.getName(), id);
    }
}
