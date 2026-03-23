package com.audiolistener.dto;

import com.audiolistener.entity.TranscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionResponse {
    private Long id;
    private String filename;
    private String originalFileType;
    private TranscriptionStatus status;
    private String transcriptText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double audioDurationSeconds;
    private String errorMessage;
}
