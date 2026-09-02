/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PatchMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package za.co.agrinexus.notification.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.agrinexus.notification.service.NotificationService;

@RestController
@RequestMapping(value={"/api/v1/notifications"})
public class NotificationController {
    private final NotificationService s;

    public NotificationController(NotificationService s) {
        this.s = s;
    }

    @GetMapping
    List<NotificationService.NotificationResponse> list(Authentication a) {
        return this.s.list(a.getName());
    }

    @PatchMapping(value={"/{id}/read"})
    NotificationService.NotificationResponse read(Authentication a, @PathVariable UUID id) {
        return this.s.read(a.getName(), id);
    }

    @PostMapping(value={"/read-all"})
    ResponseEntity<Void> all(Authentication a) {
        this.s.readAll(a.getName());
        return ResponseEntity.noContent().build();
    }
}
