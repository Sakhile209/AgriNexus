/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.security.authentication.AuthenticationManager
 *  org.springframework.security.authentication.UsernamePasswordAuthenticationToken
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.crypto.password.PasswordEncoder
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package za.co.agrinexus.auth.service;

import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.agrinexus.auth.dto.AuthResponse;
import za.co.agrinexus.auth.dto.LoginRequest;
import za.co.agrinexus.auth.dto.RegisterRequest;
import za.co.agrinexus.auth.model.User;
import za.co.agrinexus.auth.repository.UserRepository;
import za.co.agrinexus.auth.service.JwtService;
import za.co.agrinexus.shared.exception.ConflictException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final za.co.agrinexus.auth.verification.EmailVerificationService verification;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, za.co.agrinexus.auth.verification.EmailVerificationService verification) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.verification = verification;
    }

    public za.co.agrinexus.auth.verification.EmailVerificationService.Pending register(RegisterRequest request) {
        // Encode for both new and existing addresses to reduce registration timing differences.
        return verification.register(request, this.passwordEncoder.encode(request.password()));
    }

    @Transactional(readOnly=true)
    public AuthResponse login(LoginRequest request) {
        String email = AuthService.normalizeEmail(request.email());
        this.authenticationManager.authenticate((Authentication)new UsernamePasswordAuthenticationToken((Object)email, (Object)request.password()));
        User user=this.userRepository.findByEmail(email).orElseThrow();
        if(!user.isEmailVerified())throw new za.co.agrinexus.auth.verification.VerificationProblem("EMAIL_NOT_VERIFIED","Please verify your email address before logging in.",403);
        return this.responseFor(user);
    }

    private AuthResponse responseFor(User user) {
        JwtService.IssuedToken token = this.jwtService.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), new AuthResponse.UserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhoneNumber()));
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
