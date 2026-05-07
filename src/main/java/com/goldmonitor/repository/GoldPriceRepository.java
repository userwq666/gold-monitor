package com.goldmonitor.repository;

import com.goldmonitor.model.GoldPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoldPriceRepository extends JpaRepository<GoldPrice, Long> {
    Optional<GoldPrice> findTopByOrderByFetchTimeDesc();

    Optional<GoldPrice> findTopBySourceAndFetchTimeAfterOrderByFetchTimeDesc(String source, LocalDateTime since);

    List<GoldPrice> findByFetchTimeAfterOrderByFetchTimeAsc(LocalDateTime since);

    List<GoldPrice> findBySourceAndFetchTimeAfterOrderByFetchTimeAsc(String source, LocalDateTime since);

    List<GoldPrice> findByFetchTimeBetweenOrderByFetchTimeAsc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT MAX(gp.price) FROM GoldPrice gp WHERE gp.source = ?1 AND gp.fetchTime >= ?2 AND gp.fetchTime < ?3")
    java.math.BigDecimal findMaxPriceForSourceBetween(String source, LocalDateTime start, LocalDateTime end);

    @Query("SELECT MIN(gp.price) FROM GoldPrice gp WHERE gp.source = ?1 AND gp.fetchTime >= ?2 AND gp.fetchTime < ?3")
    java.math.BigDecimal findMinPriceForSourceBetween(String source, LocalDateTime start, LocalDateTime end);

    void deleteByFetchTimeBefore(LocalDateTime before);
}
