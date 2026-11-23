// service/company/AccountsOffice.java
package com.company.hrpayroll.service.company;

import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Whether this organisation uses the Accounts Office portal (role FINANCE_OFFICER). Set per organisation in
 * company_settings.accounts_office_enabled (V39): on for Kisiimatt, off for everyone else.
 *
 * Off: nothing changes. No Accounts Office login can be created or sign in, and the Admin releases agent payments
 * as before. On: the Accounts Office pays each agent invoice, recording the mode of payment and its proof, and
 * the Admin and the agent are told.
 */
@Component
@RequiredArgsConstructor
public class AccountsOffice {

    private final CompanySettingsRepository settingsRepository;

    public boolean enabled() {
        return settingsRepository.findAll().stream().findFirst()
                .map(CompanySettings::isAccountsOfficeEnabled)
                .orElse(false);
    }

    public void requireEnabled() {
        if (!enabled()) {
            throw new InvalidOperationException("This organisation does not use the Accounts Office.");
        }
    }
}
