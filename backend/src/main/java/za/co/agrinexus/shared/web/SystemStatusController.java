/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package za.co.agrinexus.shared.web;

import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/v1/system"})
public class SystemStatusController {
    @GetMapping(value={"/status"})
    ResponseEntity<SystemStatusResponse> status() {
        return ResponseEntity.ok(new SystemStatusResponse("AgriNexus API", "UP", Instant.now()));
    }

    record SystemStatusResponse(String service, String status, Instant timestamp) {
    }
}
