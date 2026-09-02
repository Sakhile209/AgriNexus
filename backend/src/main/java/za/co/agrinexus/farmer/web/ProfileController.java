/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PatchMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package za.co.agrinexus.farmer.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.agrinexus.farmer.dto.ProfileResponse;
import za.co.agrinexus.farmer.dto.UpdateProfileRequest;
import za.co.agrinexus.farmer.service.ProfileService;

@RestController
@RequestMapping(value={"/api/v1/me"})
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    ResponseEntity<ProfileResponse> get(Authentication authentication) {
        return ResponseEntity.ok(this.profileService.get(authentication.getName()));
    }

    @PatchMapping
    ResponseEntity<ProfileResponse> update(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(this.profileService.update(authentication.getName(), request));
    }
}
