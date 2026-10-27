// mapper/dayswork/DaysWorkedMapper.java
package com.company.hrpayroll.mapper.dayswork;

import com.company.hrpayroll.dto.response.dayswork.DaysWorkedResponse;
import com.company.hrpayroll.entity.dayswork.DaysWorkedRecord;
import com.company.hrpayroll.entity.employee.Employee;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DaysWorkedMapper {

    public DaysWorkedResponse toResponse(DaysWorkedRecord record, Employee employee) {
        return new DaysWorkedResponse(
                record.getEmployeeId(),
                employee.getFullName(),
                employee.getEmployeeNumber(),
                record.getBranchId(),
                record.getPeriodStart(),
                record.getPeriodEnd(),
                record.getSystemDays(),
                record.getDaysWorked(),
                record.isAdjusted(),
                record.getAdjustmentReason(),
                true,
                record.getLockedAt()
        );
    }

    // A period that has not been locked yet: the live attendance count.
    public DaysWorkedResponse toOpenResponse(Employee employee, LocalDate start, LocalDate end, int systemDays) {
        return new DaysWorkedResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getEmployeeNumber(),
                employee.getBranchId(),
                start,
                end,
                systemDays,
                systemDays,
                false,
                null,
                false,
                null
        );
    }
}
