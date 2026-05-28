package com.example.pfe.repository;

import com.example.pfe.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByDestinataireIdOrderByTimestampDesc(Long userId);
    long countByDestinataireIdAndLuFalse(Long userId);
}