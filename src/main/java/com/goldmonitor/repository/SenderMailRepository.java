package com.goldmonitor.repository;

import com.goldmonitor.model.SenderMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SenderMailRepository extends JpaRepository<SenderMail, Long> {
    List<SenderMail> findByEnabledTrue();
}
