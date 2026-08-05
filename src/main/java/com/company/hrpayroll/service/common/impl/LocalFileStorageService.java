// service/common/impl/LocalFileStorageService.java
package com.company.hrpayroll.service.common.impl;

import com.company.hrpayroll.service.common.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// Interim implementation — stores files on local disk under ./uploads.
// Satisfies FileStorageService so the app can boot and be tested end to
// end now. Swap for an S3FileStorageService later; nothing else in the
// codebase references this class directly, only the FileStorageService
// interface, so that swap is a one-file change with zero ripple effects.
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.storage.local-path:./uploads}")
    private String basePath;

    @Override
    public String upload(MultipartFile file, String folder) {
        try {
            Path folderPath = Paths.get(basePath, folder);
            Files.createDirectories(folderPath);

            String originalName = StringUtils.cleanPath(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
            String storedName = UUID.randomUUID() + "-" + originalName;
            Path targetPath = folderPath.resolve(storedName);

            file.transferTo(targetPath);

            String relativeUrl = folder + "/" + storedName;
            log.info("Stored file locally at {}", targetPath);
            return relativeUrl;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            Path target = Paths.get(basePath, fileUrl);
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", fileUrl, e.getMessage());
        }
    }
}