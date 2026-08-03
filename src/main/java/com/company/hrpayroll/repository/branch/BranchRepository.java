// repository/branch/BranchRepository.java
package com.company.hrpayroll.repository.branch;

import com.company.hrpayroll.entity.branch.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByCode(String code);

    boolean existsByCode(String code);

    List<Branch> findByManagerId(Long managerId);

    List<Branch> findByStatus(String status);

    List<Branch> findByIdIn(List<Long> ids);
}