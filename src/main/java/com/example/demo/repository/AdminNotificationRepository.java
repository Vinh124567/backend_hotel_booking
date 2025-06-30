// ✅ AdminNotificationRepository.java - NEW
package com.example.demo.repository;

import com.example.demo.entity.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    // Get unread notifications ordered by priority and date
    @Query("""
    SELECT n FROM AdminNotification n 
    WHERE n.isRead = false 
    ORDER BY 
        CASE n.priority 
            WHEN com.example.demo.entity.AdminNotification.Priority.CAO THEN 1 
            WHEN com.example.demo.entity.AdminNotification.Priority.TRUNG_BINH THEN 2 
            WHEN com.example.demo.entity.AdminNotification.Priority.THAP THEN 3 
            ELSE 4
        END,
        n.createdAt DESC
""")
    List<AdminNotification> findUnreadNotificationsOrderByPriorityAndDate();


    // Get all notifications with pagination
    @Query("""
        SELECT n FROM AdminNotification n 
        ORDER BY n.createdAt DESC
    """)
    List<AdminNotification> findAllOrderByCreatedAtDesc();

    // Get notifications for specific booking
    List<AdminNotification> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    // Count unread notifications
    long countByIsReadFalse();

    // Get notifications by type
    List<AdminNotification> findByNotificationTypeOrderByCreatedAtDesc(
            AdminNotification.NotificationType notificationType);

    // Get notifications in date range
    @Query("""
        SELECT n FROM AdminNotification n 
        WHERE n.createdAt BETWEEN :startDate AND :endDate 
        ORDER BY n.createdAt DESC
    """)
    List<AdminNotification> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Mark notification as read
    @Query("UPDATE AdminNotification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);

    // Mark all as read
    @Query("UPDATE AdminNotification n SET n.isRead = true WHERE n.isRead = false")
    void markAllAsRead();

    // Delete old notifications (older than X days)
    @Query("DELETE FROM AdminNotification n WHERE n.createdAt < :cutoffDate")
    void deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}