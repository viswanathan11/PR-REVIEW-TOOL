package com.example.BackendApplication.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayloadDTO {
    private String action;
    private PullRequestData pullRequest;
    private RepositoryData repository;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public PullRequestData getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequestData pullRequest) {
        this.pullRequest = pullRequest;
    }

    public RepositoryData getRepository() {
        return repository;
    }

    public void setRepository(RepositoryData repository) {
        this.repository = repository;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequestData {
        private int number;
        private String title;
        private String state;

        @JsonProperty("html_url")
        private String htmlUrl;
        private UserData user;
        private BranchData base;
        private BranchData head;

        @JsonProperty("created_at")
        private String createdAt;

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
        }

        public UserData getUser() {
            return user;
        }

        public void setUser(UserData user) {
            this.user = user;
        }

        public BranchData getBase() {
            return base;
        }

        public void setBase(BranchData base) {
            this.base = base;
        }

        public BranchData getHead() {
            return head;
        }

        public void setHead(BranchData head) {
            this.head = head;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryData {
        @JsonProperty("full_name")
        private String fullName;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserData {
        private String login;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BranchData {
        private String ref;
        private String sha;

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        public String getSha() {
            return sha;
        }

        public void setSha(String sha) {
            this.sha = sha;
        }
    }
}
