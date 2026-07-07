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
        List<PullRequest> prs = prRepository.findByRepositoryId(repoId);
        populateReviewStatus(prs);
        return ResponseEntity.ok(prs);
    }
    
    @GetMapping("/{prId}")
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
    public ResponseEntity<Map<String, String>> triggerReview(@PathVariable Long prId) {
        reviewJobService.triggerManualReview(prId);
        return ResponseEntity.ok(Map.of("status", "queued"));
    }

    @PostMapping("/repo/{repoId}/sync")
    public ResponseEntity<List<PullRequest>> syncPullRequests(@PathVariable Long repoId) {
        List<PullRequest> prs = reviewJobService.syncPullRequests(repoId);
        populateReviewStatus(prs);
        return ResponseEntity.ok(prs);
    }

    @PostMapping("/repo/{repoId}/auto-review-recent")
    public ResponseEntity<Map<String, String>> autoReviewRecentPrs(@PathVariable Long repoId) {
        // 1. Sync PRs from GitHub first
        reviewJobService.syncPullRequests(repoId);

        // 2. Fetch the top 5 open pull requests sorted by PR number desc
        List<PullRequest> openPrs = prRepository
            .findByRepositoryIdAndStateOrderByPrNumberDesc(repoId, "open");

        int count = 0;
        for (PullRequest pr : openPrs) {
            if (count >= 5) break;
            reviewJobService.triggerManualReview(pr.getId());
            count++;
        }

        return ResponseEntity.ok(Map.of(
            "status", "queued",
            "message", "Queued AI reviews for top " + count + " recent open PRs."
        ));
    }

    private void populateReviewStatus(List<PullRequest> prs) {
        for (PullRequest pr : prs) {
            reviewRepository.findByPullRequestId(pr.getId())
                .ifPresent(review -> pr.setReviewStatus(review.getStatus().name()));
        }
    }
}
