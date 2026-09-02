/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.Email
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.Pattern
 *  jakarta.validation.constraints.Size
 */
package za.co.agrinexus.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank @Size(max=100) @NotBlank @Size(max=100) String firstName, @NotBlank @Size(max=100) @NotBlank @Size(max=100) String lastName, @NotBlank @Email @Size(max=254) @NotBlank @Email @Size(max=254) String email, @NotBlank @Pattern(regexp="^[+0-9][0-9 ()-]{6,29}$", message="must be a valid phone number") @NotBlank @Pattern(regexp="^[+0-9][0-9 ()-]{6,29}$", message="must be a valid phone number") String phoneNumber, @NotBlank @Size(min=10, max=128) @NotBlank @Size(min=10, max=128) String password) {
}
