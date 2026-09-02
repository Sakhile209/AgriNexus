/*
 * Decompiled with CFR 0.152.
 */
package za.co.agrinexus.farmer.dto;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(UUID id, String firstName, String lastName, String email, String phoneNumber, Instant createdAt, Instant updatedAt) {
}
