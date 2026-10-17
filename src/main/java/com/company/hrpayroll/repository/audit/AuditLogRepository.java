// repository/audit/AuditLogRepository.java
package com.company.hrpayroll.repository.audit;

import com.company.hrpayroll.entity.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    Page<AuditLog> findByPerformedByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);

    long countByActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String action, LocalDateTime from, LocalDateTime to);

    @Query("select a.entityType, count(a) from AuditLog a " +
            "where a.createdAt >= :from and a.createdAt < :to " +
            "group by a.entityType order by count(a) desc")
    List<Object[]> countByEntityType(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select a.action, count(a) from AuditLog a " +
            "where a.createdAt >= :from and a.createdAt < :to " +
            "group by a.action order by count(a) desc")
    List<Object[]> countByAction(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select a.performedByUserId, count(a) from AuditLog a " +
            "where a.performedByUserId is not null and a.createdAt >= :from and a.createdAt < :to " +
            "group by a.performedByUserId order by count(a) desc")
    List<Object[]> countByUser(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("select a.createdAt from AuditLog a where a.createdAt >= :from and a.createdAt < :to")
    List<LocalDateTime> createdAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select distinct a.action from AuditLog a order by a.action")
    List<String> distinctActions();

    @Query("select distinct a.entityType from AuditLog a order by a.entityType")
    List<String> distinctEntityTypes();

    @Query("select distinct a.performedByUserId from AuditLog a where a.performedByUserId is not null")
    List<Long> distinctUserIds();
}