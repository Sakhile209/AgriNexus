/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package za.co.agrinexus.notification.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.agrinexus.auth.model.User;
import za.co.agrinexus.auth.repository.UserRepository;
import za.co.agrinexus.farm.model.Farm;
import za.co.agrinexus.farm.service.FarmService;
import za.co.agrinexus.health.model.Vaccination;
import za.co.agrinexus.health.repository.VaccinationRepository;
import za.co.agrinexus.notification.model.Notification;
import za.co.agrinexus.notification.repository.NotificationRepository;
import za.co.agrinexus.shared.exception.ResourceNotFoundException;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final VaccinationRepository vaccinations;
    private final UserRepository users;
    private final FarmService farms;

    public NotificationService(NotificationRepository n, VaccinationRepository v, UserRepository u, FarmService f) {
        this.notifications = n;
        this.vaccinations = v;
        this.users = u;
        this.farms = f;
    }

    @Transactional
    public List<NotificationResponse> list(String email) {
        this.refresh(email);
        return this.notifications.findAllByUserEmailOrderByCreatedAtDesc(email).stream().map(this::response).toList();
    }

    @Transactional
    public void refresh(String email) {
        User user = this.users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Farmer profile was not found."));
        for (Farm farm : this.farmsOwned(email)) {
            for (Vaccination v : this.vaccinations.findAllByAnimalFarmIdAndAnimalFarmOwnerEmailAndNextDueOnBetween(farm.getId(), email, LocalDate.now().minusDays(30L), LocalDate.now().plusDays(14L))) {
                String key = "vaccination:" + String.valueOf(v.getId()) + ":" + String.valueOf(v.getNextDueOn());
                if (this.notifications.existsByUserIdAndSourceKey(user.getId(), key)) continue;
                String title = v.getNextDueOn().isBefore(LocalDate.now()) ? "Vaccination overdue" : "Vaccination due soon";
                this.notifications.save(new Notification(user, farm, "VACCINATION", title, v.getVaccineName() + " is due on " + String.valueOf(v.getNextDueOn()), v.getNextDueOn(), key, "/animals/" + String.valueOf(v.getAnimal().getId()) + "/vaccinations"));
            }
        }
    }

    @Transactional
    public NotificationResponse read(String e, UUID id) {
        Notification n = this.notifications.findByIdAndUserEmail(id, e).orElseThrow(() -> new ResourceNotFoundException("Notification was not found."));
        n.markRead();
        return this.response(n);
    }

    @Transactional
    public void readAll(String e) {
        this.notifications.markAllRead(e);
    }

    private List<Farm> farmsOwned(String e) {
        return this.farms.list(e).stream().map(x -> this.farms.findOwned(e, x.id())).toList();
    }

    private NotificationResponse response(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getDueOn(), n.getRelatedPath(), n.isRead(), n.getCreatedAt());
    }

    public record NotificationResponse(UUID id, String type, String title, String message, LocalDate dueOn, String relatedPath, boolean read, Instant createdAt) {
    }
}
