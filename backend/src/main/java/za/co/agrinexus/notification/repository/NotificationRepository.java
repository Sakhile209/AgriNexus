/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package za.co.agrinexus.notification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.agrinexus.notification.model.Notification;

public interface NotificationRepository
extends JpaRepository<Notification, UUID> {
    public List<Notification> findAllByUserEmailOrderByCreatedAtDesc(String var1);

    public Optional<Notification> findByIdAndUserEmail(UUID var1, String var2);

    public boolean existsByUserIdAndSourceKey(UUID var1, String var2);

    public long countByUserEmailAndFarmIdAndReadFalse(String var1, UUID var2);

    @Modifying
    @Query(value="update Notification n set n.read=true where n.user.email=:email and n.read=false")
    public int markAllRead(@Param(value="email") String var1);
}
