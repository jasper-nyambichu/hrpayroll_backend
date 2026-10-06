// config/BusinessClock.java
package com.company.hrpayroll.config;

import com.company.hrpayroll.entity.company.Company;
import com.company.hrpayroll.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * One definition of "now" and "today" for attendance: the company's own timezone
 * (companies.timezone, default Africa/Nairobi), independent of the server's OS timezone.
 */
@Component
@RequiredArgsConstructor
public class BusinessClock {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Africa/Nairobi");

    private final CompanyRepository companyRepository;

    public ZoneId zone() {
        try {
            return companyRepository.findAll().stream()
                    .map(Company::getTimezone)
                    .filter(tz -> tz != null && !tz.isBlank())
                    .findFirst()
                    .map(ZoneId::of)
                    .orElse(DEFAULT_ZONE);
        } catch (Exception ex) {
            return DEFAULT_ZONE;
        }
    }

    public LocalDate today() {
        return LocalDate.now(zone());
    }

    public LocalDateTime now() {
        return LocalDateTime.now(zone());
    }

    /** Prefers the exact UTC instant (immune to a mis-set PC timezone); falls back to the wall-clock value sent. */
    public LocalDateTime resolve(LocalDateTime wallClock, Instant utc) {
        return utc != null ? LocalDateTime.ofInstant(utc, zone()) : wallClock;
    }
}