package com.example.BackendApplication.service;

import com.example.BackendApplication.dto.ReviewResultDTO;
import com.example.BackendApplication.dto.WebhookPayloadDTO;
import com.example.BackendApplication.model.*;
import com.example.BackendApplication.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewJobService {

    private static final Logger log = LoggerFactory.getLogger(ReviewJobService.class);

    private final RepositoryRepo repositoryRepo;
    private final PullRequestRepository prRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository commentRepository;
    private final GitHubService githubService;
    private final AIReviewService aiReviewService;
    private final RedisTemplate<String, String> redisTemplate;

    // Explicit constructor injection (No Lombok)
    public ReviewJobService(
            RepositoryRepo repositoryRepo,
            PullRequestRepository prRepository,
            ReviewRepository reviewRepository,
            ReviewCommentRepository commentRepository,
            GitHubService githubService,
            AIReviewService aiReviewService,
            RedisTemplate<String, String> redisTemplate) {
        this.repositoryRepo = repositoryRepo;
        this.prRepository = prRepository;
        this.reviewRepository = reviewRepository;
        this.commentRepository = commentRepository;
        this.githubService = githubService;
        this.aiReviewService = aiReviewService;
        this.redisTemplate = redisTemplate;
    }

    @Async("reviewExecutor")
    @Transactional
    public void enqueueReview(WebhookPayloadDTO payload) {
        String repoFullName = payload.getRepository().getFullName();
        int prNumber = payload.getPullRequest().getNumber();
        String lockKey = "review:lock:" + repoFullName + ":" + prNumber;

        // 1. Redis Deduplication - skip if another review thread is currently analyzing this PR
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "processing", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("Review already in progress for {}/#{}", repoFullName, prNumber);
            return;
        }

        log.info("Starting review flow for {}/#{}", repoFullName, prNumber);
        Review review = null;

        try {
            // 2. Find the tracked repository
            Repository repo = repositoryRepo.findByFullName(repoFullName)
                .orElseThrow(() -> new RuntimeException("Repository is not tracked: " + repoFullName));

            String userToken = repo.getUser().getAccessToken();
            WebhookPayloadDTO.PullRequestData prData = payload.getPullRequest();
            String incomingSha = prData.getHead().getSha();

            // 3. Check duplicate reviews: Fetch the existing PR and check if it already has a successful review for this exact SHA
            Optional<PullRequest> existingPrOpt = prRepository.findByRepositoryIdAndPrNumber(repo.getId(), prNumber);
            if (existingPrOpt.isPresent()) {
                PullRequest existingPr = existingPrOpt.get();
                if (incomingSha.equals(existingPr.getHeadSha())) {
                    Optional<Review> lastReview = reviewRepository
                        .findFirstByPullRequestIdAndStatusOrderByCreatedAtDesc(existingPr.getId(), ReviewStatus.DONE);
                    
                    if (lastReview.isPresent()) {
                        log.info("PR {}/#{} already reviewed for commit SHA: {}. Skipping duplicate review.", 
                            repoFullName, prNumber, incomingSha);
                        return;
                    }
                }
            }

            // 4. Upsert PullRequest record
            PullRequest pr = existingPrOpt.orElseGet(() -> {
                PullRequest newPr = new PullRequest();
                newPr.setRepository(repo);
                newPr.setPrNumber(prNumber);
                newPr.setCreatedAt(Instant.now());
                return newPr;
            });

            pr.setTitle(prData.getTitle());
            pr.setAuthor(prData.getUser().getLogin());
            pr.setBaseBranch(prData.getBase().getRef());
            pr.setHeadBranch(prData.getHead().getRef());
            pr.setHeadSha(incomingSha);
            pr.setState(prData.getState());
            pr.setGithubUrl(prData.getHtmlUrl());
            pr = prRepository.save(pr);

            // 5. Initialize Review record with PROCESSING status
            review = new Review();
            review.setPullRequest(pr);
            review.setStatus(ReviewStatus.PROCESSING);
            review.setModelUsed("claude-3-5-sonnet");
            review.setCreatedAt(Instant.now());
            review = reviewRepository.save(review);

            // 6. Fetch Git diff from GitHub
            String diff = githubService.fetchPrDiff(repoFullName, prNumber, userToken);
            if (diff == null || diff.isBlank()) {
                throw new RuntimeException("Empty git diff received from GitHub");
            }

            // 7. Request review from Claude API
            ReviewResultDTO result = aiReviewService.analyzeCode(diff);

            // 8. Save and post comments if issues were found
            if (result.getIssues() != null && !result.getIssues().isEmpty()) {
                Review finalReview = review;
                List<ReviewComment> comments = result.getIssues().stream()
                    .filter(i -> i.getFile() != null && i.getComment() != null)
                    .map(issue -> {
                        ReviewComment comment = new ReviewComment();
                        comment.setReview(finalReview);
                        comment.setFilePath(issue.getFile());
                        comment.setLineNumber(issue.getLine());
                        comment.setSeverity(issue.getSeverity() != null ? issue.getSeverity() : "INFO");
                        comment.setComment(issue.getComment());
                        comment.setSuggestion(issue.getSuggestion());
                        comment.setCreatedAt(Instant.now());
                        return comment;
                    })
                    .collect(Collectors.toList());
                commentRepository.saveAll(comments);

                // Post inline reviews
                for (ReviewComment c : comments) {
                    if (c.getLineNumber() != null && c.getLineNumber() > 0) {
                        githubService.postReviewComment(
                            repoFullName, prNumber, incomingSha,
                            c.getFilePath(), c.getLineNumber(),
                            formatGitHubComment(c), userToken
                        );
                    }
                }

                // Post overall review summary
                githubService.postReviewSummary(repoFullName, prNumber, formatSummaryComment(result), userToken);
            }

            // 9. Finalize Review record as successful
            review.setStatus(ReviewStatus.DONE);
            review.setReviewSummary(result.getSummary());
            review.setOverallScore(result.getOverallScore());
            review.setIssuesFound(result.getIssues() != null ? result.getIssues().size() : 0);
            review.setPostedToGithub(true);
            review.setCompletedAt(Instant.now());
            reviewRepository.save(review);

            log.info("Review completed successfully for {}/#{} with {} issues.", 
                repoFullName, prNumber, review.getIssuesFound());

        } catch (Exception e) {
            log.error("Review job failed for {}/#{}: {}", repoFullName, prNumber, e.getMessage(), e);
            if (review != null) {
                review.setStatus(ReviewStatus.FAILED);
                review.setErrorMessage(e.getMessage());
                review.setCompletedAt(Instant.now());
                reviewRepository.save(review);
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private String formatGitHubComment(ReviewComment c) {
        String emoji = switch (c.getSeverity()) {
            case "BUG"         -> "🐛";
            case "SECURITY"    -> "🔒";
            case "PERFORMANCE" -> "⚡";
            case "STYLE"       -> "✨";
            default            -> "💡";
        };
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**%s %s**\n\n%s", emoji, c.getSeverity(), c.getComment()));
        if (c.getSuggestion() != null && !c.getSuggestion().isBlank()) {
            sb.append(String.format("\n\n> **Suggestion:** %s", c.getSuggestion()));
        }
        return sb.toString();
    }

    private String formatSummaryComment(ReviewResultDTO result) {
        int score = result.getOverallScore() != null ? result.getOverallScore() : 0;
        int issues = result.getIssues() != null ? result.getIssues().size() : 0;
        return String.format(
            "## 🤖 AI Code Review\n\n**Score:** %d/10 | **Issues found:** %d\n\n%s\n\n" +
            "_Reviewed by [AI Code Reviewer](https://github.com)_",
            score, issues, result.getSummary()
        );
    }
}
