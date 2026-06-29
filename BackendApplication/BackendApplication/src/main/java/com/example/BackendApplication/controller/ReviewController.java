package com.example.BackendApplication.controller;

import com.example.BackendApplication.model.*;
import com.example.BackendApplication.repository.*;
import com.example.BackendApplication.service.ReviewJobService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository commentRepository;
    private final ReviewJobService reviewJobService;
    private final PullRequestRepository prRepository;

    // Explicit constructor injection (No Lombok)
    public ReviewController(ReviewRepository reviewRepository, 
                            ReviewCommentRepository commentRepository, 
                            ReviewJobService reviewJobService, 
                            PullRequestRepository prRepository) {
        this.reviewRepository = reviewRepository;
        this.commentRepository = commentRepository;
        this.reviewJobService = reviewJobService;
        this.prRepository = prRepository;
    }

    @GetMapping("/repo/{repoId}")
    public ResponseEntity<List<PullRequest>> getPullRequestsForRepo(@PathVariable Long repoId) {
        return ResponseEntity.ok(prRepository.findByRepositoryId(repoId));
    }
    
    @GetMapping("/{prId}")
    @Cacheable(value = "pr-reviews", key = "#prId")
    public ResponseEntity<Review> getReview(@PathVariable Long prId) {
        return reviewRepository.findByPullRequestId(prId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{prId}/comments")
    public ResponseEntity<List<ReviewComment>> getComments(@PathVariable Long prId) {
        return reviewRepository.findByPullRequestId(prId)
            .map(r -> ResponseEntity.ok(commentRepository.findByReviewId(r.getId())))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{prId}/trigger")
    public ResponseEntity<Map<String, String>> triggerReview(
            @AuthenticationPrincipal User user,
            @PathVariable Long prId) {
        // Build a minimal payload and re-enqueue — implementation left as exercise
        return ResponseEntity.ok(Map.of("status", "queued"));
    }
}
