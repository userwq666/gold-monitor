package com.goldmonitor.repository;

import com.goldmonitor.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findBySentAtAfterOrderBySentAtDesc(LocalDateTime since);

    List<NotificationLog> findTop50ByOrderBySentAtDesc();

    void deleteBySentAtBefore(LocalDateTime before);
}
