// repository/company/CompanyRepository.java
package com.company.hrpayroll.repository.company;

import com.company.hrpayroll.entity.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
}