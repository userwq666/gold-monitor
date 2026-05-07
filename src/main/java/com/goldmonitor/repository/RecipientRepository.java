package com.goldmonitor.repository;

import com.goldmonitor.model.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    List<Recipient> findByEnabledTrue();
}
