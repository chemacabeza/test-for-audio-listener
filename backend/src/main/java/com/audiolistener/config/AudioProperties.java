package com.audiolistener.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "audio")
public class AudioProperties {

    /** Directory to store uploaded audio files */
    private String uploadDir = "./uploads";

    /** Comma-separated list of allowed MIME types */
    private String allowedTypes = "audio/mpeg,audio/wav,audio/ogg,audio/flac,audio/mp4,audio/webm,audio/x-m4a";

    /** Maximum file size in megabytes */
    private int maxFileSizeMb = 25;
}
