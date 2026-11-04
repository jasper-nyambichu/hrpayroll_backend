package com.company.hrpayroll.service.auth.impl;

import com.company.hrpayroll.controller.auth.ProfileController.PasswordRequest;
import com.company.hrpayroll.controller.auth.ProfileController.ProfileRequest;
import com.company.hrpayroll.controller.auth.ProfileController.ProfileResponse;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.auth.UserPhoto;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.auth.UserPhotoRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/** What anyone may change about their own account: their name, email, password and photo. */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final int MAX_PHOTO_BYTES = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final UserPhotoRepository photoRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final PasswordResetMailer mailer;

    public ProfileResponse get(Long userId) {
        return toResponse(load(userId));
    }

    // Email decides where a password reset goes, so changing it needs the current password: a browser
    // left signed in must not be enough to take over the account.
    @Transactional
    public ProfileResponse update(Long userId, ProfileRequest request) {
        User user = load(userId);
        user.setDisplayName(request.displayName().trim());

        String email = request.email() == null || request.email().isBlank() ? null : request.email().trim();
        boolean emailChanged = email == null ? user.getEmail() != null : !email.equalsIgnoreCase(user.getEmail());
        if (emailChanged) {
            requireCurrentPassword(user, request.currentPassword());
            if (email != null && userRepository.findByEmailIgnoreCaseAndDeletedFalse(email)
                    .filter(other -> !other.getId().equals(userId)).isPresent()) {
                throw new DuplicateResourceException("This email is already used by another login");
            }
            user.setEmail(email);
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, PasswordRequest request) {
        User user = load(userId);
        requireCurrentPassword(user, request.currentPassword());
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new InvalidOperationException("Choose a password different from your current one");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        if (user.getEmail() != null) {
            mailer.sendPasswordChanged(user.getEmail(), user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
        }
    }

    @Transactional
    public void savePhoto(Long userId, MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read the uploaded photo", ex);
        }
        if (content.length == 0) {
            throw new InvalidOperationException("Choose a photo to upload");
        }
        if (content.length > MAX_PHOTO_BYTES) {
            throw new InvalidOperationException("The photo must be 5 MB or smaller");
        }

        // Going by the file's first bytes, not the name or type the browser claims it is.
        String contentType = detectImageType(content);
        if (contentType == null) {
            throw new InvalidOperationException("The photo must be a PNG or JPEG image");
        }

        UserPhoto photo = photoRepository.findByUserId(userId)
                .orElseGet(() -> UserPhoto.builder().userId(userId).build());
        photo.setContent(content);
        photo.setContentType(contentType);
        photoRepository.save(photo);
    }

    @Transactional(readOnly = true)
    public UserPhoto getPhoto(Long userId) {
        return photoRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No photo has been uploaded"));
    }

    @Transactional
    public void removePhoto(Long userId) {
        photoRepository.deleteByUserId(userId);
    }

    private void requireCurrentPassword(User user, String currentPassword) {
        String key = "profile:" + user.getId();
        if (loginAttempts.isLocked(key)) {
            throw new InvalidOperationException("Too many wrong passwords. Wait 15 minutes and try again.");
        }
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            loginAttempts.recordFailure(key);
            throw new InvalidOperationException("Your current password is incorrect");
        }
        loginAttempts.recordSuccess(key);
    }

    private User load(Long userId) {
        return userRepository.findById(userId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(user.getId(), user.getUsername(), user.getRole().name(), user.getDisplayName(),
                user.getEmail(), photoRepository.existsByUserId(user.getId()));
    }

    private static String detectImageType(byte[] content) {
        if (content.length >= 4 && (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G') {
            return "image/png";
        }
        if (content.length >= 3 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return null;
    }
}
