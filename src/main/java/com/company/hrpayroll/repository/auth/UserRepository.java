// repository/auth/UserRepository.java
package com.company.hrpayroll.repository.auth;

import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(Role role);

    List<User> findByBranchId(Long branchId);

    Optional<User> findByEmployeeId(Long employeeId);

    Optional<User> findByAgentId(Long agentId);
}