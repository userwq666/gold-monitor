package com.goldmonitor.repository;

import com.goldmonitor.model.MailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MailConfigRepository extends JpaRepository<MailConfig, Long> {
}
