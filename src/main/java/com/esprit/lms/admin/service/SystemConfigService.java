package com.esprit.lms.admin.service;

import com.esprit.lms.shared.exception.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads/writes system_config key-value pairs.
 * Cached with 5-minute TTL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    @PersistenceContext
    private EntityManager em;

    @Cacheable(value = "systemConfig", key = "#key")
    public String getValue(String key) {
        Object result = em.createNativeQuery("SELECT value FROM system_config WHERE key = :key")
                .setParameter("key", key)
                .getSingleResult();
        return result != null ? result.toString() : null;
    }

    public int getInt(String key, int defaultValue) {
        String val = getValue(key);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    public int getLoanDurationDays() {
        return getInt("loan_duration_days", 14);
    }

    public int getRenewalDays() {
        return getInt("renewal_days", 7);
    }

    public int getDefaultBorrowLimit() {
        return getInt("default_borrow_limit", 3);
    }

    public int getReservationExpiryHours() {
        return getInt("reservation_expiry_hours", 48);
    }

    @Transactional
    @CacheEvict(value = "systemConfig", key = "#key")
    public void setValue(String key, String value) {
        int updated = em.createNativeQuery("UPDATE system_config SET value = :value WHERE key = :key")
                .setParameter("key", key)
                .setParameter("value", value)
                .executeUpdate();
        if (updated == 0) {
            throw ApiException.notFound("Config key not found: " + key);
        }
        log.info("Config updated: {} = {}", key, value);
    }
}
