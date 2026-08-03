// mapper/shift/ShiftMapper.java
package com.company.hrpayroll.mapper.shift;

import com.company.hrpayroll.dto.request.shift.ShiftRequest;
import com.company.hrpayroll.dto.response.shift.ShiftResponse;
import com.company.hrpayroll.entity.shift.Shift;
import org.springframework.stereotype.Service;

@Service
public class ShiftMapper {

    public Shift toShift(ShiftRequest dto) {
        return Shift.builder()
                .name(dto.name())
                .type(dto.type())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .build();
    }

    public void updateShiftFromRequest(Shift shift, ShiftRequest dto) {
        shift.setName(dto.name());
        shift.setType(dto.type());
        shift.setStartTime(dto.startTime());
        shift.setEndTime(dto.endTime());
    }

    public ShiftResponse toShiftResponse(Shift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getName(),
                shift.getType(),
                shift.getStartTime(),
                shift.getEndTime()
        );
    }
}