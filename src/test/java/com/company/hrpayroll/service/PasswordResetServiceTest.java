package com.company.hrpayroll.service;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.entity.auth.PasswordReset;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.repository.auth.PasswordResetRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.security.LoginAttemptService;
import com.company.hrpayroll.service.audit.AuditService;
import com.company.hrpayroll.service.auth.impl.PasswordResetMailer;
import com.company.hrpayroll.service.auth.impl.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Forgotten passwords must not reveal who has an account, and each step must only work once and in time. */
class PasswordResetServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 12, 0);

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordResetRepository resets = mock(PasswordResetRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PasswordResetMailer mailer = mock(PasswordResetMailer.class);
    private final LoginAttemptService attempts = mock(LoginAttemptService.class);
    private final BusinessClock clock = mock(BusinessClock.class);
    private final PasswordResetService service =
            new PasswordResetService(users, resets, encoder, mailer, attempts, mock(AuditService.class), clock);

    private User account(String email) {
        User user = User.builder().username("manager").role(Role.BRANCH_MANAGER).email(email).active(true).password("old").build();
        user.setId(7L);
        when(users.findByUsername("manager")).thenReturn(Optional.of(user));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(clock.now()).thenReturn(NOW);
        return user;
    }

    private PasswordReset waiting(LocalDateTime expiresAt) {
        PasswordReset reset = PasswordReset.builder().userId(7L).codeHash("hash").expiresAt(expiresAt).build();
        when(resets.findFirstByUserIdAndUsedAtIsNullOrderByIdDesc(7L)).thenReturn(Optional.of(reset));
        return reset;
    }

    @Test
    void anUnknownAccountSendsNothingAndDoesNotFail() {
        when(users.findByUsername("nobody")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCaseAndDeletedFalse("nobody")).thenReturn(Optional.empty());

        service.requestCode("nobody");

        verify(mailer, never()).sendCode(any(), any(), any(), any(), anyInt());
    }

    @Test
    void anAccountWithoutAnEmailSendsNothingAndDoesNotFail() {
        account(null);

        service.requestCode("manager");

        verify(mailer, never()).sendCode(any(), any(), any(), any(), anyInt());
    }

    @Test
    void aRealAccountGetsASixDigitCodeAndOnlyItsHashIsStored() {
        account("manager@example.com");
        when(encoder.encode(anyString())).thenReturn("stored-hash");
        when(resets.findByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of());

        service.requestCode("manager");

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendCode(eq("manager@example.com"), eq("manager"), eq("manager"), code.capture(), eq(10));
        assertTrue(code.getValue().matches("\\d{6}"));

        ArgumentCaptor<PasswordReset> saved = ArgumentCaptor.forClass(PasswordReset.class);
        verify(resets).save(saved.capture());
        assertEquals("stored-hash", saved.getValue().getCodeHash());
        assertEquals(NOW.plusMinutes(10), saved.getValue().getExpiresAt());
    }

    @Test
    void askingAgainCancelsTheEarlierCode() {
        account("manager@example.com");
        when(encoder.encode(anyString())).thenReturn("stored-hash");
        PasswordReset earlier = PasswordReset.builder().userId(7L).build();
        when(resets.findByUserIdAndUsedAtIsNull(7L)).thenReturn(List.of(earlier));

        service.requestCode("manager");

        assertEquals(NOW, earlier.getUsedAt());
    }

    @Test
    void floodingIsCutOffWithoutSayingSo() {
        when(attempts.isLocked("forgot:manager")).thenReturn(true);

        service.requestCode("manager");

        verify(users, never()).findByUsername(any());
        verify(mailer, never()).sendCode(any(), any(), any(), any(), anyInt());
    }

    @Test
    void theRightCodeGivesATokenWhoseHashIsStored() {
        account("manager@example.com");
        PasswordReset reset = waiting(NOW.plusMinutes(5));
        when(encoder.matches("123456", "hash")).thenReturn(true);

        String token = service.verifyCode("manager", "123456");

        assertNotNull(token);
        assertNotNull(reset.getResetTokenHash());
        assertTrue(!reset.getResetTokenHash().equals(token));
        assertEquals(NOW.plusMinutes(15), reset.getResetTokenExpiresAt());
    }

    @Test
    void aWrongCodeCountsAndTheFifthCancelsIt() {
        account("manager@example.com");
        PasswordReset reset = waiting(NOW.plusMinutes(5));
        reset.setAttempts(4);
        when(encoder.matches("000000", "hash")).thenReturn(false);

        assertThrows(InvalidOperationException.class, () -> service.verifyCode("manager", "000000"));

        assertEquals(5, reset.getAttempts());
        assertEquals(NOW, reset.getUsedAt());
        verify(attempts).recordFailure("reset-code:manager");
    }

    @Test
    void anExpiredCodeIsRefusedEvenIfCorrect() {
        account("manager@example.com");
        waiting(NOW.minusSeconds(1));
        when(encoder.matches("123456", "hash")).thenReturn(true);

        assertThrows(InvalidOperationException.class, () -> service.verifyCode("manager", "123456"));
    }

    @Test
    void anExpiredTokenIsRefusedAndThePasswordStays() {
        User user = account("manager@example.com");
        PasswordReset reset = PasswordReset.builder().userId(7L).resetTokenExpiresAt(NOW.minusSeconds(1)).build();
        when(resets.findByResetTokenHashAndUsedAtIsNull(anyString())).thenReturn(Optional.of(reset));

        assertThrows(InvalidOperationException.class, () -> service.resetPassword("token", "NewPass123"));
        assertEquals("old", user.getPassword());
    }

    @Test
    void aGoodTokenChangesThePasswordAndIsUsedUp() {
        User user = account("manager@example.com");
        PasswordReset reset = PasswordReset.builder().userId(7L).resetTokenExpiresAt(NOW.plusMinutes(5)).build();
        when(resets.findByResetTokenHashAndUsedAtIsNull(anyString())).thenReturn(Optional.of(reset));
        when(encoder.encode("NewPass123")).thenReturn("new-hash");

        assertEquals("BRANCH_MANAGER", service.resetPassword("token", "NewPass123"));

        assertEquals("new-hash", user.getPassword());
        assertEquals(NOW, reset.getUsedAt());
        verify(mailer).sendPasswordChanged("manager@example.com", "manager");
    }
}
