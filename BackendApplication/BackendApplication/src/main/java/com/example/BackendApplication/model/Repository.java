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
@Table(name = "repository")
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many repositories belong to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_repo_id", nullable = false)
    private String githubRepoId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "description")
    private String description;

    @Column(name = "private")
    private Boolean isPrivate;

    @Column(name = "webhook_id")
    private String webhookId;

    @Column(name = "webhook_active")
    private Boolean webhookActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Repository() {
    }

    public Repository(Long id, User user, String githubRepoId, String fullName, String desctiption, Boolean isPrivate,
            String webhookId, Boolean webhookActive, Instant createdAt) {
        this.id = id;
        this.user = user;
        this.githubRepoId = githubRepoId;
        this.fullName = fullName;
        this.description = desctiption;
        this.isPrivate = isPrivate;
        this.webhookId = webhookId;
        this.webhookActive = webhookActive;
        this.createdAt = createdAt;
    }

    // 3. Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getGithubRepoId() {
        return githubRepoId;
    }

    public void setGithubRepoId(String githubRepoId) {
        this.githubRepoId = githubRepoId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    public Boolean getWebhookActive() {
        return webhookActive;
    }

    public void setWebhookActive(Boolean webhookActive) {
        this.webhookActive = webhookActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}