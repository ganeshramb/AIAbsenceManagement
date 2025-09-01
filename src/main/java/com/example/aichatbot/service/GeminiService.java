package com.example.aichatbot.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.aichatbot.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Service
public class GeminiService {
    private final WebClient webClient;
    private final GeminiProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public GeminiService(GeminiProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getApiUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<String> chatCompletion(String prompt, String[] history) {
        StringBuilder combinedPrompt = new StringBuilder(systemPrompt());
        for (String msg : history) {
            combinedPrompt.append("\n").append(msg);
        }
        combinedPrompt.append("\nUser: ").append(prompt).append("\nAssistant:");

        RequestBody body = new RequestBody(combinedPrompt.toString());

        return webClient.post()
                .header("x-goog-api-key", properties.getApiKey())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractResponseText)  // Extract text cleanly before returning
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
    }

    private String systemPrompt() {
        return """
        You are an Absence Leave Assistant. Collect from the user:
        - Employee name (must match company records)
        - Employee ID (must match company records)
        - Absence type (vacation, sick, etc.)
        - Start and end dates YYYY-MM-DD
        - Reason for leave

        If info is missing, ask only for missing info without repeating. Confirm all info before saving.
        
        When the user provides dates as "today", "tomorrow", or natural language expressions, convert these into explicit YYYY-MM-DD dates based on the current date. 
        Always output dates in ISO format in your final JSON.
        
        If the user provides several pieces of information in a single message, extract all of them. Only ask for information that is not yet provided.
        
        You may use the employee name to infer the employee ID if only the name is provided.
        When completing the JSON confirmation, always provide both employee name and employeeId from the above records.

        Once confirmed, output JSON ONLY with keys employeeId, absenceType, startDate, endDate, reason, notifyTo.

        Respond conversationally before confirmation. Respond ONLY with JSON after confirmation.
        """;
    }

    // Method to extract AI reply text cleanly from Gemini API JSON string
    private String extractResponseText(String rawJson) {
        try {
            JsonNode root = mapper.readTree(rawJson);
            JsonNode textNode = root.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text");
            if (textNode.isMissingNode()) {
                return "Sorry, no valid response text found.";
            }
            return textNode.asText();
        } catch (Exception e) {
            return "Error parsing response: " + e.getMessage();
        }
    }

    // DTO Classes for request JSON body
    static class RequestBody {
        private Content[] contents;
        public RequestBody(String text) {
            this.contents = new Content[]{ new Content(new Part[]{ new Part(text) }) };
        }
        public Content[] getContents() { return contents; }
        public void setContents(Content[] contents) { this.contents = contents; }
    }
    static class Content {
        private Part[] parts;
        public Content(Part[] parts) { this.parts = parts; }
        public Part[] getParts() { return parts; }
        public void setParts(Part[] parts) { this.parts = parts; }
    }
    static class Part {
        private String text;
        public Part(String text) { this.text = text; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    // Helper method to parse structured JSON data out of AI response
    public ParsedData parseStructuredData(String llmResponse) {
        try {
            int start = llmResponse.indexOf("{");
            int end = llmResponse.lastIndexOf("}");
            if (start != -1 && end != -1 && end > start) {
                String jsonText = llmResponse.substring(start, end + 1);
                JsonNode root = mapper.readTree(jsonText);
                ParsedData data = new ParsedData();
                data.employeeId = root.path("employeeId").asText(null);
                data.absenceType = root.path("absenceType").asText(null);
                data.startDate = root.path("startDate").asText(null);
                data.endDate = root.path("endDate").asText(null);
                data.reason = root.path("reason").asText(null);
                data.notifyTo = root.path("notifyTo").asText(null);
                return data;
            }
        } catch (Exception e) {
            // parsing failed, return null
        }
        return null;
    }

    public static class ParsedData {
        public String employeeId;
        public String absenceType;
        public String startDate;
        public String endDate;
        public String reason;
        public String notifyTo;
    }
}
