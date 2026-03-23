package com.audiolistener.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transcriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transcription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "original_file_type")
    private String originalFileType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TranscriptionStatus status;

    @Column(name = "transcript_text", columnDefinition = "TEXT")
    private String transcriptText;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "audio_duration_seconds")
    private Double audioDurationSeconds;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "stored_file_path")
    private String storedFilePath;
}
