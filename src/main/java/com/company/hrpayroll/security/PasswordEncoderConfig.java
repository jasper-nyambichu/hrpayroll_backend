// security/PasswordEncoderConfig.java
package com.company.hrpayroll.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Pulled out of SecurityConfig deliberately: SecurityConfig depends on
// DeviceAuthFilter (constructor injection), and DeviceAuthFilter depends on
// PasswordEncoder. If PasswordEncoder were still a @Bean method inside
// SecurityConfig, Spring would need SecurityConfig fully built before it
// could supply DeviceAuthFilter's dependency — but SecurityConfig can't be
// built without DeviceAuthFilter already existing. A separate, independent
// class breaks that cycle entirely.
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}