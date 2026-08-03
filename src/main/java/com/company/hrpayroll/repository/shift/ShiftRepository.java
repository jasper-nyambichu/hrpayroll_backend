// repository/shift/ShiftRepository.java
package com.company.hrpayroll.repository.shift;

import com.company.hrpayroll.entity.shift.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
}