package com.example.BackendApplication.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    public RepositoryController(RepositoryRepo repositoryRepo, GitHubService gitHubService){
        this.gitHubService=gitHubService;
        this.repositoryRepo=repositoryRepo;
    }


    @GetMapping
    public ResponseEntity<List<Repository>> listTrackedRepo(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(repositoryRepo.findByUserId(user.getId()));
    }

       @GetMapping("/github")
    public ResponseEntity<List<Map<String, Object>>> listGitHubRepos(
            @AuthenticationPrincipal User user) {
            System.out.println("[DEBUG] Fetching Github repos for user: "+user.getGithubLogin());
            System.out.println("[DEBUG] Access Token Length: " + (user.getAccessToken() != null ? user.getAccessToken().length() : "null"));

            try{

                List<Map<String, Object>> repos = gitHubService.getUserRepositories(user.getAccessToken());
                System.out.println("[DEBUG] GitHub Service returned: " + (repos != null ? "List of size " + repos.size() : "null"));
                  if (repos == null) {
                // If it returned null, send a 500 error instead of a 200 with an empty body
                return ResponseEntity.status(500).build();
            }
                return ResponseEntity.ok(repos);

            }catch(Exception e){
                System.err.println("[DEBUG] Exception caught while fetching repos: " + e.getMessage());
                e.printStackTrace();

                return ResponseEntity.status(500).build();
            }
    }
}
