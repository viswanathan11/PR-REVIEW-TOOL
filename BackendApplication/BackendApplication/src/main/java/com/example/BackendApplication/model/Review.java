package com.example.BackendApplication.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many reviews belong to One pull request
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "review_status")
    private ReviewStatus status;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "review_summary")
    private String reviewSummary;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "issues_found")
    private Integer issuesFound;

    // Hibernate 6 handles JSONB mapping perfectly with this annotation
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response")
    private String rawResponse;

    @Column(name = "posted_to_github")
    private Boolean postedToGithub;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // 1. Required No-Args Constructor
    public Review() {
    }

    // 2. All-Args Constructor
    public Review(Long id, PullRequest pullRequest, ReviewStatus status, String modelUsed, String reviewSummary,
            Integer overallScore, Integer issuesFound, String rawResponse, Boolean postedToGithub, String errorMessage,
            Instant createdAt, Instant completedAt) {
        this.id = id;
        this.pullRequest = pullRequest;
        this.status = status;
        this.modelUsed = modelUsed;
        this.reviewSummary = reviewSummary;
        this.overallScore = overallScore;
        this.issuesFound = issuesFound;
        this.rawResponse = rawResponse;
        this.postedToGithub = postedToGithub;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    // 3. Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public String getReviewSummary() {
        return reviewSummary;
    }

    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public Integer getIssuesFound() {
        return issuesFound;
    }

    public void setIssuesFound(Integer issuesFound) {
        this.issuesFound = issuesFound;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public Boolean getPostedToGithub() {
        return postedToGithub;
    }

    public void setPostedToGithub(Boolean postedToGithub) {
        this.postedToGithub = postedToGithub;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
