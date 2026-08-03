// repository/leave/LeaveTypeRepository.java
package com.company.hrpayroll.repository.leave;

import com.company.hrpayroll.entity.leave.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    Optional<LeaveType> findByName(String name);
}