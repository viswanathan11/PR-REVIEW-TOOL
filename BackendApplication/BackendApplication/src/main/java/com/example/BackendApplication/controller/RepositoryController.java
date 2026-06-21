package com.example.BackendApplication.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.BackendApplication.model.Repository;
import com.example.BackendApplication.model.User;
import com.example.BackendApplication.repository.RepositoryRepo;
import com.example.BackendApplication.service.GitHubService;

@RestController
@RequestMapping("/api/repos")
public class RepositoryController {

    private final RepositoryRepo repositoryRepo;
    private final GitHubService gitHubService;

    // Explicit constructor injection (No Lombok)
    public RepositoryController(RepositoryRepo repositoryRepo, GitHubService gitHubService) {
        this.gitHubService = gitHubService;
        this.repositoryRepo = repositoryRepo;
    }

    @GetMapping
    public ResponseEntity<List<Repository>> listTrackedRepos(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(repositoryRepo.findByUserId(user.getId()));
    }

    @GetMapping("/github")
    public ResponseEntity<List<Map<String, Object>>> listGitHubRepos(
            @AuthenticationPrincipal User user) {
        System.out.println("[DEBUG] Fetching GitHub repos for user: " + user.getGithubLogin());
        System.out.println("[DEBUG] Access Token Length: " + (user.getAccessToken() != null ? user.getAccessToken().length() : "null"));

        try {
            List<Map<String, Object>> repos = gitHubService.getUserRepositories(user.getAccessToken());
            System.out.println("[DEBUG] GitHub Service returned: " + (repos != null ? "List of size " + repos.size() : "null"));
            if (repos == null) {
                return ResponseEntity.status(500).build();
            }
            return ResponseEntity.ok(repos);
        } catch (Exception e) {
            System.err.println("[DEBUG] Exception caught while fetching repos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping
    public ResponseEntity<Repository> addRepo(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        String githubRepoId = String.valueOf(body.get("githubRepoId"));
        String fullName     = String.valueOf(body.get("fullName"));
        String description  = (String) body.get("description");

               // Declare as final and assign exactly once across all conditional paths
        final Boolean isPrivate;
        Object privateObj = body.get("private");
        if (privateObj instanceof Boolean) {
            isPrivate = (Boolean) privateObj;
        } else if (privateObj instanceof String) {
            isPrivate = Boolean.parseBoolean((String) privateObj);
        } else {
            isPrivate = false; // Fallback branch
        }


        System.out.println("[DEBUG] Tracking repo: " + fullName);

        Repository repo = repositoryRepo
            .findByUserIdAndGithubRepoId(user.getId(), githubRepoId)
            .orElseGet(() -> {
                Repository newRepo = new Repository();
                newRepo.setUser(user);
                newRepo.setGithubRepoId(githubRepoId);
                newRepo.setFullName(fullName);
                newRepo.setDescription(description);
                newRepo.setIsPrivate(isPrivate);
                newRepo.setWebhookActive(false);
                newRepo.setCreatedAt(java.time.Instant.now());
                return newRepo;
            });

        return ResponseEntity.ok(repositoryRepo.save(repo));
    }

    @PostMapping("/{id}/webhook")
    public ResponseEntity<Repository> enableWebhook(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Repository repo = repositoryRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Repo not found"));

        if (!repo.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to repository");
        }

        System.out.println("[DEBUG] Registering GitHub Webhook for " + repo.getFullName());
        String webhookId = gitHubService.registerWebhook(
            repo.getFullName(), user.getAccessToken());
        repo.setWebhookId(webhookId);
        repo.setWebhookActive(true);
        return ResponseEntity.ok(repositoryRepo.save(repo));
    }

    @DeleteMapping("/{id}/webhook")
    public ResponseEntity<Repository> disableWebhook(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Repository repo = repositoryRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Repo not found"));

        if (!repo.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to repository");
        }

        System.out.println("[DEBUG] Deleting GitHub Webhook for " + repo.getFullName());
        if (repo.getWebhookId() != null) {
            gitHubService.deleteWebhook(repo.getFullName(),
                repo.getWebhookId(), user.getAccessToken());
        }
        repo.setWebhookId(null);
        repo.setWebhookActive(false);
        return ResponseEntity.ok(repositoryRepo.save(repo));
    }
}
