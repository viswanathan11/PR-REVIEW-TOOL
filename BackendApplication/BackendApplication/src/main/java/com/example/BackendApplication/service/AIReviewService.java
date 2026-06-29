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
            "model",      props.getAi().getModel(),
            "max_tokens", props.getAi().getMaxTokens(),
            "system",     SYSTEM_PROMPT,
            "messages",   List.of(Map.of(
                "role",    "user",
                "content", "Review this pull request diff:\n\n```diff\n" + diff + "\n```"
            ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", props.getAi().getAnthropicApiKey());
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.anthropic.com/v1/messages",
            new HttpEntity<>(requestBody, headers),
            Map.class
        );

        String jsonText = extractContent(response.getBody());
        
        // Strip any accidental markdown blocks that the LLM might have returned
        jsonText = jsonText.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

        try {
            return objectMapper.readValue(jsonText, ReviewResultDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", jsonText);
            throw new RuntimeException("AI returned invalid JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null || !response.containsKey("content")) {
            throw new RuntimeException("Empty response body from Anthropic API");
        }
        
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        return content.stream()
            .filter(c -> "text".equals(c.get("type")))
            .map(c -> (String) c.get("text"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No text in AI response"));
    }
}
