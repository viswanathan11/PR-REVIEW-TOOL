package com.example.BackendApplication.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewResultDTO {
    private String summary;
    private List<IssueDTO> issues;

    @JsonProperty("overall_score")
    private Integer overallScore;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<IssueDTO> getIssues() {
        return issues;
    }

    public void setIssues(List<IssueDTO> issues) {
        this.issues = issues;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueDTO {
        private String file;
        private Integer line;
        private String severity;
        private String comment;
        private String suggestion;

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public Integer getLine() {
            return line;
        }

        public void setLine(Integer line) {
            this.line = line;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }
}
