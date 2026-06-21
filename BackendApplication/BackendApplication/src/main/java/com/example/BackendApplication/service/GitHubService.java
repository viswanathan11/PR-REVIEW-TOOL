package com.example.BackendApplication.service;

/*
This provides method to communicate directly with github using the users's personal 
OAuth2 aacess_token
1.Fetching the users'available repositories.
2.Registering a webhook on a repository so Github notifies our app when PRs are opend
3.Deleting a webhook when tracking is disabled.
4.Fetcing raw pull request diffs for AI analysis
5.Posting individual lines comments and review summarize back to GIthub PR.

Why doing this?
Structuring all GItHub communcation inside a dedicated service prevents HTTP
header setup,authentication and enpoint construction logic from cluttering our controllers.
*/

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.BackendApplication.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final RestTemplate restTemplate;
    private final AppProperties props;
    private final ObjectMapper objectMapper;

    // Explicit constructor injection

    public GitHubService(RestTemplate restTemplate, AppProperties props, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Accept", "application/vnd.github.v3+json");
         h.set("User-Agent", "AI-PR-Reviewer-App");
        return h;

    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUserRepositories(String token) {
        ResponseEntity<List> response = restTemplate.exchange(
                "http://api.github.com/user/repos?per_page=100&sort=updated", HttpMethod.GET,
                new HttpEntity<>(headers(token)),
                List.class);

        return (List<Map<String, Object>>) response.getBody();
    }

    @SuppressWarnings("unchecked")
    public String registerWebhook(String repoFullName, String token) {
        String webhookUrl = props.getBaseUrl() + "/api/webhooks/github";
        Map<String, Object> body = Map.of(
                "name", "web",
                "active", true,
                "events", List.of("pull_request"),
                "config", Map.of(
                        "url", webhookUrl,
                        "content_type", "json",
                        "secret", props.getWebhook().getSecret()));

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api/github.com/repos/" + repoFullName + "/hooks",
                new HttpEntity<>(headers(token)),
                Map.class);

        return response.getBody().get("id").toString();
    }

    public void delteWebhook(String repoFullName, String webhookId, String token) {
        restTemplate.exchange("https://api.github.com/repos/" + repoFullName + "/hooks/" + webhookId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers(token)),
                void.class);
    }

    public String fetchPrDiff(String repoFullName, int prNumber, String token) {
        HttpHeaders h = headers(token);

        h.set("Accept", "application/vnd.github.v3.diff");
        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.github.com/repos/" + repoFullName + "/pulls/" + prNumber,
                HttpMethod.GET,
                new HttpEntity<>(h),
                String.class);

        String diff = response.getBody();
        if (diff != null && diff.length() > props.getAi().getMaxDiffChars()) {
            diff = diff.substring(0, (int) props.getAi().getMaxDiffChars()) + "\n...[diff truncated]";
        }
        return diff;
    }

    public void postReviewComment(String repoFUllName, int prNumber, String commitSha, String filePath, int line,
            String body, String token) {
        Map<String, Object> payload = Map.of(
                "body", body,
                "commit_id", commitSha,
                "path", filePath,
                "line", line,
                "side", "RIGHT");

        try {
            restTemplate.postForEntity(
                    "https://api.github.com/repos/" + repoFUllName + "/pulls" + prNumber + "/comments",
                    new HttpEntity<>(payload, headers(token)),
                    Void.class);
        } catch (Exception e) {

            log.warn("Faild to post review comment to GitHub: {}", e.getMessage());
        }
    };

    public void postReviewSUmmary(String repoFUllName,int prNumber,String body,String token){
        Map<String,Object> payload = Map.of("body",body);
        restTemplate.postForEntity(
            "https://api.github.com/repos/"+repoFUllName+"/issues/"+prNumber+"/comments",
             new HttpEntity<>(payload,headers(token)), 
            Void.class
        );
    }

}
