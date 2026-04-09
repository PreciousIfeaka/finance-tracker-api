package com.precious.finance_tracker.dtos.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class GeminiRequest {
    private List<Content> contents;

    private GenerationConfig generationConfig;


    @Data
    public static class GenerationConfig {
        @JsonProperty("response_mime_type")
        private String responseMimeType;

        @JsonProperty("max_output_tokens")
        private Integer maxOutputTokens;
    }

    @Data
    public static class Content {
        private String role;
        private List<Part> parts;
    }

    @Data
    public static class Part {
        private String text;
        private FileData fileData;
    }

    @Data
    public static class FileData {
        private String mimeType;
        private String fileUri;
    }
}
