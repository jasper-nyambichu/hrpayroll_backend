package com.company.hrpayroll.entity.auth;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// A login's profile photo: PNG or JPEG. Apart from User so the image only loads when asked for.
@Entity
@Table(name = "user_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPhoto extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] content;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;
}
