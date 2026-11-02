// repository/branch/BranchRepository.java
package com.company.hrpayroll.repository.branch;

import com.company.hrpayroll.entity.branch.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    List<Branch> findByStatusNotOrderByCodeAsc(String status);

    long countByStatusNot(String status);

    // Two steps because the codes are unique and one row's new code can be another row's current one.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE branches SET code = 'TMP-' || id WHERE status <> 'DELETED'", nativeQuery = true)
    void clearCodes();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE branches b SET code = lpad(CAST(r.position AS TEXT), GREATEST(3, length(CAST(r.position AS TEXT))), '0') "
            + "FROM (SELECT id, row_number() OVER (ORDER BY id) AS position FROM branches WHERE status <> 'DELETED') r "
            + "WHERE b.id = r.id", nativeQuery = true)
    void assignCodesInOrder();
}