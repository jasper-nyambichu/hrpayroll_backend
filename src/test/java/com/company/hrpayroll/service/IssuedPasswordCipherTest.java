package com.company.hrpayroll.service;

import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.service.auth.impl.IssuedPasswordCipher;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class IssuedPasswordCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final IssuedPasswordCipher cipher = new IssuedPasswordCipher("", KEY, "jwt-secret");

    @Test
    void readsBackThePasswordForTheSameLogin() {
        byte[] sealed = cipher.encrypt("Branch2026", 7L);

        assertThat(new String(sealed)).doesNotContain("Branch2026");
        assertThat(cipher.decrypt(sealed, 7L)).isEqualTo("Branch2026");
    }

    @Test
    void refusesToReadItAsAnotherLogin() {
        byte[] sealed = cipher.encrypt("Branch2026", 7L);

        assertThat(cipher.decrypt(sealed, 8L)).isNull();
    }

    @Test
    void aDifferentKeyCannotReadIt() {
        byte[] sealed = cipher.encrypt("Branch2026", 7L);
        IssuedPasswordCipher other = new IssuedPasswordCipher("", "", "jwt-secret");

        assertThat(other.decrypt(sealed, 7L)).isNull();
    }

    @Test
    void choosingYourOwnPasswordForgetsTheIssuedOne() {
        User user = new User();
        user.setIssuedPasswordEnc(cipher.encrypt("Branch2026", 7L));
        user.setIssuedPasswordAt(LocalDateTime.of(2026, 11, 6, 9, 0));
        LocalDateTime now = LocalDateTime.of(2026, 11, 6, 10, 0);

        user.forgetIssuedPassword(now);

        assertThat(user.getIssuedPasswordEnc()).isNull();
        assertThat(user.getIssuedPasswordAt()).isNull();
        assertThat(user.getPasswordChangedByOwnerAt()).isEqualTo(now);
    }
}
