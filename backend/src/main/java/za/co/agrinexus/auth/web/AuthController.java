/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package za.co.agrinexus.auth.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.agrinexus.auth.dto.AuthResponse;
import za.co.agrinexus.auth.dto.LoginRequest;
import za.co.agrinexus.auth.dto.RegisterRequest;
import za.co.agrinexus.auth.service.AuthService;

@RestController
@RequestMapping(value={"/api/v1/auth"})
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value={"/register"})
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status((HttpStatusCode)HttpStatus.CREATED).body(this.authService.register(request));
    }

    @PostMapping(value={"/login"})
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(this.authService.login(request));
    }

    @PostMapping(value={"/logout"})
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
