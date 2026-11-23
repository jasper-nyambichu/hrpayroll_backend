// entity/company/WorkWeekDay.java
package com.company.hrpayroll.entity.company;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

// One day of the organisation's work week (V40): whether it works that day and its hours. Seven rows, MONDAY…SUNDAY.
@Entity
@Table(name = "work_week_days")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkWeekDay extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String dayOfWeek;

    @Column(nullable = false)
    private boolean working;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;
}
