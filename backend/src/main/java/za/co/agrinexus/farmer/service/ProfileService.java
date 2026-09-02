/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package za.co.agrinexus.farmer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.agrinexus.auth.model.User;
import za.co.agrinexus.auth.repository.UserRepository;
import za.co.agrinexus.farmer.dto.ProfileResponse;
import za.co.agrinexus.farmer.dto.UpdateProfileRequest;
import za.co.agrinexus.shared.exception.ResourceNotFoundException;

@Service
public class ProfileService {
    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly=true)
    public ProfileResponse get(String email) {
        return this.toResponse(this.find(email));
    }

    @Transactional
    public ProfileResponse update(String email, UpdateProfileRequest request) {
        User user = this.find(email);
        user.updateProfile(request.firstName().trim(), request.lastName().trim(), request.phoneNumber().trim());
        return this.toResponse(user);
    }

    private User find(String email) {
        return this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Farmer profile was not found."));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhoneNumber(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
