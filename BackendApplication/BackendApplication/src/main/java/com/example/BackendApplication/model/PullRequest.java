package com.example.BackendApplication.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pull_requests")
public class PullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many pull requests belong to one Repository
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "base_branch", nullable = false, length = 100)
    private String baseBranch;

    @Column(name = "head_branch", nullable = false, length = 100)
    private String headBranch;

    @Column(name = "head_sha", nullable = false, length = 100)
    private String headSha;

    @Column(name = "state", nullable = false, length = 20)
    private String state;

    @Column(name = "github_url", nullable = false)
    private String githubUrl;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PullRequest() {

    }

    public PullRequest(Long id, Repository repository, Integer prNumber, String title, String author, String baseBranch,
            String headBranch, String headSha, String state, String githubUrl, Instant openedAt, Instant createdAt) {
        this.id = id;
        this.repository = repository;
        this.prNumber = prNumber;
        this.title = title;
        this.author = author;
        this.baseBranch = baseBranch;
        this.headBranch = headBranch;
        this.headSha = headSha;
        this.state = state;
        this.githubUrl = githubUrl;
        this.openedAt = openedAt;
        this.createdAt = createdAt;
    }

    // 3. Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Integer getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(Integer prNumber) {
        this.prNumber = prNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBaseBranch() {
        return baseBranch;
    }

    public void setBaseBranch(String baseBranch) {
        this.baseBranch = baseBranch;
    }

    public String getHeadBranch() {
        return headBranch;
    }

    public void setHeadBranch(String headBranch) {
        this.headBranch = headBranch;
    }

    public String getHeadSha() {
        return headSha;
    }

    public void setHeadSha(String headSha) {
        this.headSha = headSha;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}
