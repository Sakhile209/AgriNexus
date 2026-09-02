/*
 * Decompiled with CFR 0.152.
 */
package za.co.agrinexus.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserSummary user) {

    public record UserSummary(UUID id, String firstName, String lastName, String email, String phoneNumber) {
    }
}
