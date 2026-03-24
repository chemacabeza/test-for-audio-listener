package com.audiolistener.controller;

import com.audiolistener.dto.TranscriptionResponse;
import com.audiolistener.service.TranscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/transcriptions")
@RequiredArgsConstructor
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    /**
     * Upload an audio file and create a new transcription.
     *
     * POST /api/transcriptions
     * Content-Type: multipart/form-data
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscriptionResponse> uploadAndTranscribe(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "offline", defaultValue = "false") boolean offline) {
        log.info("Received audio upload: name={}, size={} bytes, type={}, offline={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType(), offline);
        TranscriptionResponse response = transcriptionService.transcribeAudio(file, offline);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all transcriptions, optionally filtered by a search query.
     *
     * GET /api/transcriptions
     * GET /api/transcriptions?q=search+term
     */
    @GetMapping
    public ResponseEntity<List<TranscriptionResponse>> getAllTranscriptions(
            @RequestParam(name = "q", required = false) String query) {
        List<TranscriptionResponse> transcriptions = transcriptionService.getAllTranscriptions(query);
        return ResponseEntity.ok(transcriptions);
    }

    /**
     * Get a single transcription by its ID.
     *
     * GET /api/transcriptions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TranscriptionResponse> getTranscription(@PathVariable Long id) {
        TranscriptionResponse response = transcriptionService.getTranscription(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a transcription by its ID.
     *
     * DELETE /api/transcriptions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTranscription(@PathVariable Long id) {
        transcriptionService.deleteTranscription(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Download the original audio file for a transcription.
     *
     * GET /api/transcriptions/{id}/download
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadAudio(@PathVariable Long id) {
        Resource resource = transcriptionService.getAudioResource(id);
        TranscriptionResponse transcription = transcriptionService.getTranscription(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + transcription.getFilename() + "\"")
                .body(resource);
    }
}
