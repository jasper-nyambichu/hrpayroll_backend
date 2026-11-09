// controller/company/MailTestController.java
package com.company.hrpayroll.controller.company;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.service.auth.impl.PasswordResetMailer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets an Admin prove the server can send email (the password reset codes depend on it) and, when it can't,
 * see the mail server's own reason instead of guessing.
 */
@RestController
@RequestMapping("/company/test-email")
@RequiredArgsConstructor
public class MailTestController {

    private final PasswordResetMailer mailer;

    public record TestEmailRequest(@NotBlank(message = "Enter an email address") @Email(message = "Enter a valid email address") String to) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "TEST_EMAIL_SENT", entityType = "Company")
    public ApiResponse<Void> send(@Valid @RequestBody TestEmailRequest request) {
        String error = mailer.sendTest(request.to().trim());
        if (error != null) {
            throw new InvalidOperationException("The email could not be sent: " + error);
        }
        return ApiResponse.success("Test email sent to " + request.to().trim() + ". Check that inbox (and its spam folder).", null);
    }
}
