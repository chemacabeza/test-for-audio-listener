package com.audiolistener.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    /**
     * OpenAI API key — read from env var OPENAI_API_KEY.
     * Never log or expose this value.
     */
    private String apiKey;

    /** Model to use for transcription (default: whisper-1) */
    private String model = "whisper-1";

    /** OpenAI Audio transcription endpoint URL */
    private String apiUrl = "https://api.openai.com/v1/audio/transcriptions";

    /** Timeout in seconds for the OpenAI API call */
    private int timeoutSeconds = 120;

    /** Maximum number of retries on transient failures */
    private int maxRetries = 2;
}
