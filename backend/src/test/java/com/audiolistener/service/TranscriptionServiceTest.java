package com.audiolistener.service;

import com.audiolistener.client.OpenAiTranscriptionClient;
import com.audiolistener.config.AudioProperties;
import com.audiolistener.dto.TranscriptionResponse;
import com.audiolistener.entity.Transcription;
import com.audiolistener.entity.TranscriptionStatus;
import com.audiolistener.repository.TranscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceTest {

    @Mock
    private TranscriptionRepository repository;

    @Mock
    private OpenAiTranscriptionClient openAiClient;

    @Mock
    private AudioProperties audioProperties;

    @InjectMocks
    private TranscriptionService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(audioProperties.getUploadDir()).thenReturn(tempDir.toString());
        when(audioProperties.getAllowedTypes()).thenReturn("audio/mpeg,audio/wav,audio/ogg,audio/webm");
        when(audioProperties.getMaxFileSizeMb()).thenReturn(25);
    }

    @Test
    void transcribeAudio_success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.wav", "audio/wav", "fake-audio-data".getBytes());

        Transcription saved = Transcription.builder()
                .id(1L)
                .filename("test.wav")
                .originalFileType("audio/wav")
                .status(TranscriptionStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(repository.save(any(Transcription.class))).thenReturn(saved);
        when(openAiClient.transcribe(any(File.class), anyBoolean())).thenReturn("Hello world transcription");

        // Update saved to completed
        Transcription completed = Transcription.builder()
                .id(1L)
                .filename("test.wav")
                .originalFileType("audio/wav")
                .status(TranscriptionStatus.COMPLETED)
                .transcriptText("Hello world transcription")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(repository.save(any(Transcription.class))).thenReturn(completed);

        // When
        TranscriptionResponse response = service.transcribeAudio(file, false);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TranscriptionStatus.COMPLETED);
        assertThat(response.getTranscriptText()).isEqualTo("Hello world transcription");
        verify(openAiClient).transcribe(any(File.class), anyBoolean());
        verify(repository, times(2)).save(any(Transcription.class));
    }

    @Test
    void transcribeAudio_openAiFailure_setsFailedStatus() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.wav", "audio/wav", "fake-audio-data".getBytes());

        Transcription saved = Transcription.builder()
                .id(1L)
                .filename("test.wav")
                .status(TranscriptionStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(repository.save(any(Transcription.class))).thenReturn(saved);

        when(openAiClient.transcribe(any(File.class), anyBoolean()))
                .thenThrow(new OpenAiTranscriptionClient.OpenAiException("API error"));

        Transcription failed = Transcription.builder()
                .id(1L)
                .filename("test.wav")
                .status(TranscriptionStatus.FAILED)
                .errorMessage("API error")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(repository.save(any(Transcription.class))).thenReturn(failed);

        // When
        TranscriptionResponse response = service.transcribeAudio(file, false);

        // Then
        assertThat(response.getStatus()).isEqualTo(TranscriptionStatus.FAILED);
        assertThat(response.getErrorMessage()).isEqualTo("API error");
    }

    @Test
    void transcribeAudio_invalidFileType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "not audio".getBytes());

        assertThatThrownBy(() -> service.transcribeAudio(file, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void transcribeAudio_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.wav", "audio/wav", new byte[0]);

        assertThatThrownBy(() -> service.transcribeAudio(file, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void getAllTranscriptions_returnsOrderedList() {
        Transcription t1 = Transcription.builder()
                .id(1L).filename("a.wav").status(TranscriptionStatus.COMPLETED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        Transcription t2 = Transcription.builder()
                .id(2L).filename("b.wav").status(TranscriptionStatus.PROCESSING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(t2, t1));

        List<TranscriptionResponse> result = service.getAllTranscriptions(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void getAllTranscriptions_withQuery_usesSearchMethod() {
        when(repository.searchByQuery("hello")).thenReturn(List.of());

        service.getAllTranscriptions("hello");

        verify(repository).searchByQuery("hello");
        verify(repository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getTranscription_notFound_throwsException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTranscription(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteTranscription_success() {
        Transcription t = Transcription.builder()
                .id(1L).filename("test.wav").status(TranscriptionStatus.COMPLETED)
                .storedFilePath(tempDir.resolve("fake-file.wav").toString())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(t));

        service.deleteTranscription(1L);

        verify(repository).delete(t);
    }
}
