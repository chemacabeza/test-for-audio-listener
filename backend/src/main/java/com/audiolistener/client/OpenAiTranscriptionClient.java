package com.audiolistener.client;

import com.audiolistener.config.OpenAiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/**
 * Client responsible for communicating with the OpenAI Audio API.
 * This is the ONLY class that sends requests to OpenAI.
 * The API key is read from environment variables and never exposed.
 */
@Slf4j
@Component
public class OpenAiTranscriptionClient {

    private final OpenAiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiTranscriptionClient(OpenAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Sends an audio file to the OpenAI Audio API for transcription.
     *
     * @param audioFile the audio file to transcribe
     * @return the transcription text returned by OpenAI
     * @throws OpenAiException if the API call fails after retries
     */
    public String transcribe(File audioFile, boolean offline) throws OpenAiException {
        int attempts = 0;
        Exception lastException = null;
        
        String endpointUrl = offline ? "http://whisper:8000/v1/audio/transcriptions" : "https://api.openai.com/v1/audio/transcriptions";
        String apiKey = properties.getApiKey();

        while (attempts <= properties.getMaxRetries()) {
            try {
                attempts++;
                log.info("Sending transcription request to {} (attempt {}/{})",
                        offline ? "Offline Whisper CPU" : "OpenAI Cloud", attempts, properties.getMaxRetries() + 1);

                // Build the multipart request body for the OpenAI transcription endpoint
                String boundary = UUID.randomUUID().toString();
                byte[] requestBody = buildMultipartBody(boundary, audioFile);

                // Send the POST request to the dynamically selected API endpoint
                // The API key is passed as a Bearer token in the Authorization header
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpointUrl))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode json = objectMapper.readTree(response.body());
                    String text = json.path("text").asText("");
                    log.info("Transcription received successfully ({} chars)", text.length());
                    return text;
                } else if (response.statusCode() >= 500 && attempts <= properties.getMaxRetries()) {
                    log.warn("OpenAI server error ({}), retrying...", response.statusCode());
                    lastException = new OpenAiException(
                            "OpenAI API returned status " + response.statusCode() + ": " + response.body());
                    Thread.sleep(1000L * attempts); // Exponential backoff
                } else {
                    throw new OpenAiException(
                            "OpenAI API error (status " + response.statusCode() + "): " + response.body());
                }
            } catch (OpenAiException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OpenAiException("Transcription interrupted", e);
            } catch (Exception e) {
                lastException = e;
                if (attempts > properties.getMaxRetries()) {
                    break;
                }
                log.warn("Transcription attempt {} failed: {}", attempts, e.getMessage());
            }
        }

        throw new OpenAiException("Transcription failed after " + attempts + " attempts", lastException);
    }

    /**
     * Builds a multipart/form-data request body for the OpenAI API.
     * Includes the audio file and the model parameter.
     */
    private byte[] buildMultipartBody(String boundary, File audioFile) throws IOException {
        String fileName = audioFile.getName();
        byte[] fileBytes = java.nio.file.Files.readAllBytes(audioFile.toPath());
        String lineBreak = "\r\n";

        StringBuilder builder = new StringBuilder();

        // Add model field
        builder.append("--").append(boundary).append(lineBreak);
        builder.append("Content-Disposition: form-data; name=\"model\"").append(lineBreak);
        builder.append(lineBreak);
        builder.append(properties.getModel()).append(lineBreak);

        // Add response_format field
        builder.append("--").append(boundary).append(lineBreak);
        builder.append("Content-Disposition: form-data; name=\"response_format\"").append(lineBreak);
        builder.append(lineBreak);
        builder.append("json").append(lineBreak);

        // Add file field header
        builder.append("--").append(boundary).append(lineBreak);
        builder.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(fileName).append("\"").append(lineBreak);
        builder.append("Content-Type: application/octet-stream").append(lineBreak);
        builder.append(lineBreak);

        // Combine text parts + file bytes + closing boundary
        byte[] headerBytes = builder.toString().getBytes();
        String closingBoundary = lineBreak + "--" + boundary + "--" + lineBreak;
        byte[] closingBytes = closingBoundary.getBytes();

        byte[] body = new byte[headerBytes.length + fileBytes.length + closingBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(closingBytes, 0, body, headerBytes.length + fileBytes.length, closingBytes.length);

        return body;
    }

    /**
     * Custom exception for OpenAI API errors.
     */
    public static class OpenAiException extends Exception {
        public OpenAiException(String message) {
            super(message);
        }

        public OpenAiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
