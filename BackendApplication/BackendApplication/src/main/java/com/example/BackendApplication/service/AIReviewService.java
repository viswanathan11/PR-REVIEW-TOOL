package com.example.BackendApplication.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.BackendApplication.config.AppProperties;
import com.example.BackendApplication.dto.ReviewResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AIReviewService {

    private static final Logger log = LoggerFactory.getLogger(AIReviewService.class);

    private final RestTemplate restTemplate;
    private final AppProperties props;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        You are a senior software engineer performing a thorough code review.
        Analyze the provided git diff carefully.

        Return ONLY a valid JSON object with this exact structure — no markdown, no explanation:
        {
          "summary": "2-3 sentence overview of the changes and overall quality",
          "overall_score": 7,
          "issues": [
            {
              "file": "src/main/java/Example.java",
              "line": 42,
              "severity": "BUG",
              "comment": "Clear description of the issue",
              "suggestion": "Specific fix or improvement"
            }
          ]
        }

        Severity must be one of: BUG, SECURITY, PERFORMANCE, STYLE, INFO
        Focus on: null pointer risks, SQL injection, missing error handling,
        inefficient loops, hardcoded secrets, missing input validation.
        If no issues found, return an empty issues array.
        overall_score is 1-10 where 10 is perfect production-ready code.
        """;

    // Explicit constructor injection (No Lombok)
    public AIReviewService(RestTemplate restTemplate, AppProperties props, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public ReviewResultDTO analyzeCode(String diff) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of(
                    "text", "Review this pull request diff:\n\n```diff\n" + diff + "\n```"
                ))
            )),
            "systemInstruction", Map.of(
                "parts", List.of(Map.of(
                    "text", SYSTEM_PROMPT
                ))
            ),
            "generationConfig", Map.of(
                "responseMimeType", "application/json"
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Call Google Gemini API
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" 
            + props.getAi().getGeminiApiKey();

        ResponseEntity<Map> response = restTemplate.postForEntity(
            url,
            new HttpEntity<>(requestBody, headers),
            Map.class
        );

        String jsonText = extractContent(response.getBody());

        try {
            return objectMapper.readValue(jsonText, ReviewResultDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", jsonText);
            throw new RuntimeException("AI returned invalid JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null || !response.containsKey("candidates")) {
            throw new RuntimeException("Empty response body from Gemini API");
        }
        
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("No candidates returned from Gemini API");
        }
        
        Map<String, Object> firstCandidate = candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
        if (content == null || !content.containsKey("parts")) {
            throw new RuntimeException("No content parts in Gemini response");
        }
        
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new RuntimeException("No parts returned in Gemini candidate content");
        }
        
        return (String) parts.get(0).get("text");
    }
}
