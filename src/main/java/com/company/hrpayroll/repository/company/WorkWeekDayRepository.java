// repository/company/WorkWeekDayRepository.java
package com.company.hrpayroll.repository.company;

import com.company.hrpayroll.entity.company.WorkWeekDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkWeekDayRepository extends JpaRepository<WorkWeekDay, Long> {
}
