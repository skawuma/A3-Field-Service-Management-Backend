package com.a3solutions.fsm.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Objects;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.storage
 * @project A3 Field Service Management Backend
 * @date 11/19/25
 */
@Service
public class LocalStorageService implements StorageService {

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

            String originalName = Objects.requireNonNull(file.getOriginalFilename(), "file name is missing");
            String filename = System.currentTimeMillis() + "_" + originalName.replaceAll("\\s+", "_");

            Path destinationFile = this.rootLocation.resolve(filename).normalize();

            // Prevent path traversal
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new RuntimeException("Cannot store file outside the configured directory.");
            }

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file at {}", destinationFile);

            // Store as retrievable relative path
            return "/files/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public Resource loadAsResource(String storedPath) {
        try {
            String filename = extractFilename(storedPath);
            Path file = rootLocation.resolve(filename).normalize();

            if (!file.startsWith(rootLocation)) {
                throw new RuntimeException("Invalid file path.");
            }

            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            }

            throw new RuntimeException("Could not read file: " + storedPath);

        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not load file: " + storedPath, e);
        }
    }

    @Override
    public void delete(String storedPath) {
        try {
            String filename = extractFilename(storedPath);
            Path file = rootLocation.resolve(filename).normalize();

            if (!file.startsWith(rootLocation)) {
                throw new RuntimeException("Invalid file path.");
            }

            Files.deleteIfExists(file);
            log.info("Deleted file {}", file);

        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + storedPath, e);
        }
    }

    private String extractFilename(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new RuntimeException("Stored path is empty.");
        }

        // Example input: /files/1711152233445_invoice.pdf
        return Paths.get(storedPath).getFileName().toString();
    }


//    @Override
//    public String storeBytes(byte[] bytes, String filename, String contentType) {
//        try {
//            if (bytes == null || bytes.length == 0) {
//                throw new RuntimeException("Cannot store empty byte content.");
//            }
//
//            String safeFilename = System.currentTimeMillis() + "_" + filename.replaceAll("\\s+", "_");
//            Path destinationFile = this.rootLocation.resolve(safeFilename).normalize();
//
//            if (!destinationFile.startsWith(this.rootLocation)) {
//                throw new RuntimeException("Cannot store file outside the configured directory.");
//            }
//
//            Files.write(destinationFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//            log.info("Stored raw bytes at {}", destinationFile);
//
//            return "/files/" + safeFilename;
//
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to store byte content", e);
//        }
//    }

    @Override
    public String storeBytes(byte[] bytes, String filename, String contentType) {
        try {
            if (bytes == null || bytes.length == 0) {
                throw new RuntimeException("Cannot store empty byte content.");
            }

            String safeFilename = System.currentTimeMillis() + "_" + filename.replaceAll("\\s+", "_");
            Path destinationFile = this.rootLocation.resolve(safeFilename).normalize();

            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new RuntimeException("Cannot store file outside the configured directory.");
            }

            Files.write(destinationFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Stored raw bytes at {}", destinationFile);

            return "/files/" + safeFilename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store byte content", e);
        }
    }
}