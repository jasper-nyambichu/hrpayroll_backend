// service/company/OrganisationOperations.java
package com.company.hrpayroll.service.company;

import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.entity.company.WorkWeekDay;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import com.company.hrpayroll.repository.company.WorkWeekDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * How the organisation operates (V40), edited under Admin → Settings:
 *   - its work week: for each day, whether it works and from when to when. Late arrivals and early leaving are
 *     measured against that day's hours; a day it does not work is not counted as missed;
 *   - whether only one employee of a department (in the same branch) may be off on the same day.
 */
@Service
@RequiredArgsConstructor
public class OrganisationOperations {

    private static final LocalTime DEFAULT_START = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(17, 0);

    private final WorkWeekDayRepository weekRepository;
    private final CompanySettingsRepository settingsRepository;

    public record Day(String dayOfWeek, boolean working, LocalTime startTime, LocalTime endTime) {}

    public record Operations(List<Day> week, boolean oneOffPerDepartment) {}

    @Transactional(readOnly = true)
    public Operations get() {
        Map<DayOfWeek, Day> schedule = schedule();
        return new Operations(
                Arrays.stream(DayOfWeek.values()).map(schedule::get).toList(),
                settings().map(CompanySettings::isOneOffPerDepartment).orElse(false));
    }

    /** Every day of the week with its hours; a day not saved yet is open with the saved (or default) hours. */
    @Transactional(readOnly = true)
    public Map<DayOfWeek, Day> schedule() {
        Map<String, WorkWeekDay> saved = weekRepository.findAll().stream()
                .collect(Collectors.toMap(WorkWeekDay::getDayOfWeek, Function.identity(), (a, b) -> a));
        LocalTime start = settings().map(CompanySettings::getWorkStartTime).orElse(DEFAULT_START);
        LocalTime end = settings().map(CompanySettings::getWorkEndTime).orElse(DEFAULT_END);
        Map<DayOfWeek, Day> week = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            WorkWeekDay row = saved.get(day.name());
            week.put(day, row == null
                    ? new Day(day.name(), true, start, end)
                    : new Day(day.name(), row.isWorking(), row.getStartTime(), row.getEndTime()));
        }
        return week;
    }

    public boolean oneOffPerDepartment() {
        return settings().map(CompanySettings::isOneOffPerDepartment).orElse(false);
    }

    @Transactional
    public Operations update(Operations request) {
        if (request.week() == null || request.week().size() != 7) {
            throw new InvalidOperationException("Give the hours for all seven days of the week");
        }
        Set<DayOfWeek> seen = new HashSet<>();
        for (Day day : request.week()) {
            DayOfWeek dow;
            try {
                dow = DayOfWeek.valueOf(String.valueOf(day.dayOfWeek()).trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new InvalidOperationException("Unknown day: " + day.dayOfWeek());
            }
            if (!seen.add(dow)) throw new InvalidOperationException(label(dow) + " is given twice");
            if (day.startTime() == null || day.endTime() == null) {
                throw new InvalidOperationException("Give the start and end time for " + label(dow));
            }
            if (!day.endTime().isAfter(day.startTime())) {
                throw new InvalidOperationException(label(dow) + ": work must end after it starts");
            }
        }
        if (request.week().stream().noneMatch(Day::working)) {
            throw new InvalidOperationException("The organisation must work on at least one day of the week");
        }

        Map<String, WorkWeekDay> saved = weekRepository.findAll().stream()
                .collect(Collectors.toMap(WorkWeekDay::getDayOfWeek, Function.identity(), (a, b) -> a));
        for (Day day : request.week()) {
            String name = day.dayOfWeek().trim().toUpperCase(Locale.ROOT);
            WorkWeekDay row = saved.getOrDefault(name, WorkWeekDay.builder().dayOfWeek(name).build());
            row.setWorking(day.working());
            row.setStartTime(day.startTime());
            row.setEndTime(day.endTime());
            weekRepository.save(row);
        }

        CompanySettings settings = settings()
                .orElseThrow(() -> new InvalidOperationException("Save the company rules first"));
        settings.setOneOffPerDepartment(request.oneOffPerDepartment());
        // The single start/end kept for older screens follows the first working day.
        request.week().stream().filter(Day::working).findFirst().ifPresent(first -> {
            settings.setWorkStartTime(first.startTime());
            settings.setWorkEndTime(first.endTime());
        });
        settingsRepository.save(settings);
        return get();
    }

    private java.util.Optional<CompanySettings> settings() {
        return settingsRepository.findAll().stream().findFirst();
    }

    private static String label(DayOfWeek day) {
        String name = day.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
