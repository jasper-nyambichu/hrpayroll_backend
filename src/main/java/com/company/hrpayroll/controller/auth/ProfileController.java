package com.company.hrpayroll.controller.auth;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.auth.UserPhoto;
import com.company.hrpayroll.service.auth.impl.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// Everything here acts on the signed-in account, whichever role it has. No id is taken from the request,
// so nobody can reach another person's profile through these.
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    public record ProfileResponse(Long id, String username, String role, String displayName, String email, boolean hasPhoto) {}

    public record ProfileRequest(
            @NotBlank(message = "Your name is required") @Size(max = 150, message = "Your name is too long") String displayName,
            @Email(message = "Enter a valid email address") @Size(max = 150, message = "That email address is too long") String email,
            String currentPassword
    ) {}

    public record PasswordRequest(
            @NotBlank(message = "Your current password is required") String currentPassword,
            @NotBlank(message = "A new password is required")
            @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
            @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$",
                    message = "Password needs an uppercase letter, a lowercase letter and a number")
            String newPassword
    ) {}

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> get(@AuthenticationPrincipal User user) {
        return ApiResponse.success(profileService.get(user.getId()));
    }

    @PutMapping("/profile")
    @Auditable(action = "PROFILE_UPDATED", entityType = "User")
    public ApiResponse<ProfileResponse> update(@AuthenticationPrincipal User user, @Valid @RequestBody ProfileRequest request) {
        return ApiResponse.success("Profile saved", profileService.update(user.getId(), request));
    }

    @PutMapping("/password")
    @Auditable(action = "PASSWORD_CHANGED", entityType = "User")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal User user, @Valid @RequestBody PasswordRequest request) {
        profileService.changePassword(user.getId(), request);
        return ApiResponse.success("Password changed", null);
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Auditable(action = "PROFILE_PHOTO_UPLOADED", entityType = "User")
    public ApiResponse<Void> uploadPhoto(@AuthenticationPrincipal User user, @RequestParam("file") MultipartFile file) {
        profileService.savePhoto(user.getId(), file);
        return ApiResponse.success("Photo saved", null);
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> getPhoto(@AuthenticationPrincipal User user) {
        UserPhoto photo = profileService.getPhoto(user.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(photo.getContent());
    }

    @DeleteMapping("/photo")
    @Auditable(action = "PROFILE_PHOTO_REMOVED", entityType = "User")
    public ApiResponse<Void> removePhoto(@AuthenticationPrincipal User user) {
        profileService.removePhoto(user.getId());
        return ApiResponse.success("Photo removed", null);
    }
}
