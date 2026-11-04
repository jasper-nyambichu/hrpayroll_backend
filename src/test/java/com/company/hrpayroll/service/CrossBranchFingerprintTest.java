package com.company.hrpayroll.service;

import com.company.hrpayroll.entity.attendance.BiometricDeviceMapping;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.repository.attendance.BiometricDeviceMappingRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.attendance.impl.FingerprintCipher;
import com.company.hrpayroll.service.attendance.impl.TerminalIdentity;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A fingerprint belongs to the employee, so it works at any branch, and only ever for that one person. */
class CrossBranchFingerprintTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final byte[] TEMPLATE = {1, 2, 3, 4, 5, 6, 7, 8};

    private final FingerprintCipher cipher = new FingerprintCipher(KEY);
    private final BiometricDeviceMappingRepository mappings = mock(BiometricDeviceMappingRepository.class);
    private final TerminalIdentity identity = new TerminalIdentity(mappings, mock(EmployeeRepository.class));

    private static BiometricDeviceMapping mapped(long device, String terminalId, long employee) {
        return BiometricDeviceMapping.builder().deviceId(device).terminalUserId(terminalId).employeeId(employee).build();
    }

    @Test
    void anIdEnrolledOnAnotherTerminalIsRecognisedHere() {
        when(mappings.findByDeviceIdAndTerminalUserId(9L, "emp-1")).thenReturn(Optional.empty());
        when(mappings.findByTerminalUserId("emp-1")).thenReturn(List.of(mapped(3L, "emp-1", 1L), mapped(4L, "emp-1", 1L)));

        assertEquals(Optional.of(1L), identity.employeeFor(9L, "emp-1"));
    }

    @Test
    void thisTerminalsOwnMappingWins() {
        when(mappings.findByDeviceIdAndTerminalUserId(9L, "1001")).thenReturn(Optional.of(mapped(9L, "1001", 5L)));

        assertEquals(Optional.of(5L), identity.employeeFor(9L, "1001"));
    }

    @Test
    void anIdThatMeansTwoPeopleIsNeverGuessed() {
        when(mappings.findByDeviceIdAndTerminalUserId(9L, "1001")).thenReturn(Optional.empty());
        when(mappings.findByTerminalUserId("1001")).thenReturn(List.of(mapped(3L, "1001", 1L), mapped(4L, "1001", 2L)));

        assertEquals(Optional.empty(), identity.employeeFor(9L, "1001"));
    }

    @Test
    void anUnknownIdIsRefused() {
        when(mappings.findByDeviceIdAndTerminalUserId(9L, "emp-777")).thenReturn(Optional.empty());
        when(mappings.findByTerminalUserId("emp-777")).thenReturn(List.of());

        assertEquals(Optional.empty(), identity.employeeFor(9L, "emp-777"));
    }

    @Test
    void aTemplateIsEncryptedAndComesBackIntact() {
        byte[] stored = cipher.encrypt(TEMPLATE, 1L, (short) 1);

        assertFalse(java.util.Arrays.equals(TEMPLATE, stored));
        assertArrayEquals(TEMPLATE, cipher.decrypt(stored, 1L, (short) 1));
    }

    @Test
    void aTemplateCopiedOntoAnotherEmployeeCannotBeRead() {
        byte[] stored = cipher.encrypt(TEMPLATE, 1L, (short) 1);

        assertThrows(IllegalStateException.class, () -> cipher.decrypt(stored, 2L, (short) 1));
    }

    @Test
    void withoutAKeyNothingIsStored() {
        assertThrows(InvalidOperationException.class, () -> new FingerprintCipher("").encrypt(TEMPLATE, 1L, (short) 1));
    }
}
