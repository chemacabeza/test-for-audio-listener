package com.audiolistener.controller;

import com.audiolistener.dto.TranscriptionResponse;
import com.audiolistener.entity.TranscriptionStatus;
import com.audiolistener.service.TranscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TranscriptionController.class)
class TranscriptionControllerTest {

        @TestConfiguration
        static class MockConfig {
                @Bean
                public TranscriptionService transcriptionService() {
                        return Mockito.mock(TranscriptionService.class);
                }
        }

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private TranscriptionService transcriptionService;

        @Test
        void uploadAndTranscribe_returnsCreated() throws Exception {
                TranscriptionResponse response = TranscriptionResponse.builder()
                                .id(1L)
                                .filename("test.wav")
                                .status(TranscriptionStatus.COMPLETED)
                                .transcriptText("Hello world")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                when(transcriptionService.transcribeAudio(any(), anyBoolean())).thenReturn(response);

                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.wav", "audio/wav", "fake-audio".getBytes());

                mockMvc.perform(multipart("/api/transcriptions").file(file))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.status").value("COMPLETED"))
                                .andExpect(jsonPath("$.transcriptText").value("Hello world"));
        }

        @Test
        void getAllTranscriptions_returnsOk() throws Exception {
                TranscriptionResponse response = TranscriptionResponse.builder()
                                .id(1L)
                                .filename("test.wav")
                                .status(TranscriptionStatus.COMPLETED)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                when(transcriptionService.getAllTranscriptions(null)).thenReturn(List.of(response));

                mockMvc.perform(get("/api/transcriptions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        void getTranscription_returnsOk() throws Exception {
                TranscriptionResponse response = TranscriptionResponse.builder()
                                .id(1L)
                                .filename("test.wav")
                                .status(TranscriptionStatus.COMPLETED)
                                .transcriptText("Transcribed text")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                when(transcriptionService.getTranscription(1L)).thenReturn(response);

                mockMvc.perform(get("/api/transcriptions/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.transcriptText").value("Transcribed text"));
        }

        @Test
        void deleteTranscription_returnsNoContent() throws Exception {
                mockMvc.perform(delete("/api/transcriptions/1"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void uploadInvalidRequest_returnsBadRequest() throws Exception {
                when(transcriptionService.transcribeAudio(any(), anyBoolean()))
                                .thenThrow(new IllegalArgumentException("Unsupported file type"));

                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.txt", "text/plain", "not audio".getBytes());

                mockMvc.perform(multipart("/api/transcriptions").file(file))
                                .andExpect(status().isBadRequest());
        }
}
