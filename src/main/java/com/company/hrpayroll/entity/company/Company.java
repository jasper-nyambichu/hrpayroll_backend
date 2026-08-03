// entity/company/Company.java
package com.company.hrpayroll.entity.company;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    private String logoUrl;
    private String address;
    private String phone;
    private String email;
    private String website;

    @Column(length = 20)
    private String kraPin;

    @Column(length = 20)
    private String shifNumber;

    @Column(length = 20)
    private String nssfNumber;

    @Column(length = 20)
    private String nitaNumber;

    @Builder.Default
    @Column(length = 10)
    private String currency = "KES";

    @Builder.Default
    @Column(length = 50)
    private String timezone = "Africa/Nairobi";
}