package com.audiolistener.service;

import com.audiolistener.client.OpenAiTranscriptionClient;
import com.audiolistener.config.AudioProperties;
import com.audiolistener.dto.TranscriptionResponse;
import com.audiolistener.entity.Transcription;
import com.audiolistener.entity.TranscriptionStatus;
import com.audiolistener.repository.TranscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptionService {

    private final TranscriptionRepository repository;
    private final OpenAiTranscriptionClient openAiClient;
    private final AudioProperties audioProperties;

    /**
     * Upload an audio file, store it, and trigger transcription via OpenAI.
     */
    @Transactional
    public TranscriptionResponse transcribeAudio(MultipartFile file) {
        // 1. Validate the uploaded file
        validateFile(file);

        // 2. Save the file to the upload directory
        String storedPath = saveFileToDisk(file);
        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "unknown";

        // 3. Create a PENDING transcription record
        Transcription transcription = Transcription.builder()
                .filename(originalFilename)
                .originalFileType(file.getContentType())
                .status(TranscriptionStatus.PROCESSING)
                .storedFilePath(storedPath)
                .build();
        transcription = repository.save(transcription);

        // 4. Convert to MP3
        String mp3Path = convertToMp3(storedPath);
        transcription.setStoredFilePath(mp3Path);
        // Rename filename to end with .mp3 for download consistency
        String newFilename = originalFilename.replaceAll("\\.[^.]+$", "") + ".mp3";
        transcription.setFilename(newFilename);
        transcription = repository.save(transcription);

        // 5. Call OpenAI for transcription
        try {
            File audioFile = new File(mp3Path);
            String transcriptText = openAiClient.transcribe(audioFile);

            transcription.setTranscriptText(transcriptText);
            transcription.setStatus(TranscriptionStatus.COMPLETED);
            log.info("Transcription completed for file: {}", newFilename);
        } catch (OpenAiTranscriptionClient.OpenAiException e) {
            log.error("Transcription failed for file: {}", newFilename, e);
            transcription.setStatus(TranscriptionStatus.FAILED);
            transcription.setErrorMessage(e.getMessage());
        }

        transcription = repository.save(transcription);
        return toResponse(transcription);
    }

    /**
     * Get all transcriptions, optionally filtered by search query.
     */
    @Transactional(readOnly = true)
    public List<TranscriptionResponse> getAllTranscriptions(String query) {
        List<Transcription> transcriptions;
        if (query != null && !query.isBlank()) {
            transcriptions = repository.searchByQuery(query.trim());
        } else {
            transcriptions = repository.findAllByOrderByCreatedAtDesc();
        }
        return transcriptions.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get a single transcription by ID.
     */
    @Transactional(readOnly = true)
    public TranscriptionResponse getTranscription(Long id) {
        Transcription transcription = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transcription not found with id: " + id));
        return toResponse(transcription);
    }

    /**
     * Delete a transcription and its associated audio file.
     */
    @Transactional
    public void deleteTranscription(Long id) {
        Transcription transcription = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transcription not found with id: " + id));

        // Delete the stored audio file if it exists
        if (transcription.getStoredFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(transcription.getStoredFilePath()));
                log.info("Deleted audio file: {}", transcription.getStoredFilePath());
            } catch (IOException e) {
                log.warn("Could not delete audio file: {}", transcription.getStoredFilePath(), e);
            }
        }

        repository.delete(transcription);
        log.info("Deleted transcription with id: {}", id);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    public Resource getAudioResource(Long id) {
        Transcription transcription = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transcription not found with id: " + id));

        try {
            Path filePath = Paths.get(transcription.getStoredFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + transcription.getFilename());
            }
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + transcription.getFilename(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType != null) {
            // Strip parameters like ;codecs=opus
            contentType = contentType.split(";")[0].trim();
        }

        List<String> allowedTypes = Arrays.asList(audioProperties.getAllowedTypes().split(","));
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + file.getContentType() +
                            ". Allowed: " + audioProperties.getAllowedTypes());
        }

        // Validate file size
        long maxBytes = (long) audioProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "File too large: " + (file.getSize() / (1024 * 1024)) +
                            " MB. Maximum: " + audioProperties.getMaxFileSizeMb() + " MB");
        }
    }

    /**
     * Convert an audio file to MP3 format using ffmpeg.
     */
    private String convertToMp3(String inputPath) {
        String outputPath = inputPath.replaceAll("\\.[^.]+$", "") + ".mp3";
        if (outputPath.equals(inputPath)) {
            outputPath = inputPath + ".mp3";
        }

        try {
            log.info("Converting {} to MP3...", inputPath);
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", inputPath,
                    "-codec:a", "libmp3lame", "-qscale:a", "2",
                    "-y", outputPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output to avoid filling buffer
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Optional: log ffmpeg output at debug level if needed
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("ffmpeg failed with exit code " + exitCode);
            }

            log.info("Conversion successful: {}", outputPath);

            // Delete original file to save space
            try {
                Files.deleteIfExists(Paths.get(inputPath));
            } catch (IOException e) {
                log.warn("Could not delete original file after conversion: {}", inputPath);
            }

            return outputPath;
        } catch (IOException | InterruptedException e) {
            log.error("Failed to convert audio to MP3", e);
            // Re-throw as runtime exception or return original path as fallback
            // In this case, we'll fail fast because the user explicitly asked for MP3
            throw new RuntimeException("MP3 conversion failed", e);
        }
    }

    /**
     * Save the uploaded file to disk. In production, this could be swapped
     * for an S3 or object-storage implementation.
     */
    private String saveFileToDisk(MultipartFile file) {
        try {
            Path uploadDir = Paths.get(audioProperties.getUploadDir());
            Files.createDirectories(uploadDir);

            String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path destination = uploadDir.resolve(uniqueName);
            file.transferTo(destination.toFile());

            log.info("Saved audio file to: {}", destination);
            return destination.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded file", e);
        }
    }

    private TranscriptionResponse toResponse(Transcription t) {
        return TranscriptionResponse.builder()
                .id(t.getId())
                .filename(t.getFilename())
                .originalFileType(t.getOriginalFileType())
                .status(t.getStatus())
                .transcriptText(t.getTranscriptText())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .audioDurationSeconds(t.getAudioDurationSeconds())
                .errorMessage(t.getErrorMessage())
                .build();
    }
}
