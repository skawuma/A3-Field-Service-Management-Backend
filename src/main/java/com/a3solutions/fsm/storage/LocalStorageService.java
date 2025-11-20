package com.a3solutions.fsm.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.storage
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@Service
public class LocalStorageService implements StorageService{


    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path rootLocation;

    public LocalStorageService(@Value("${storage.local-root:uploads}") String rootDir) {
        this.rootLocation = Paths.get(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }
    @Override
    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path dest = this.rootLocation.resolve(filename);
            Files.copy(file.getInputStream(), dest);
            log.info("Stored file at {}", dest);
            // For now just return relative path; later: full URL or S3 URL
            return "/files/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}
