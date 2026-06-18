# Project Notes & Optimizations

## 🚨 High-Priority Optimization: Skip Duplicate Reviews on Reopened PRs

### The Problem
When a developer closes and reopens a Pull Request without making any new code changes, GitHub sends a `pull_request` event with the action `reopened`. 

Under the default template plan, this triggers a brand new AI review. This is redundant because the code hasn't changed, leading to:
1. Wasted Anthropic Claude API credits.
2. Duplicate comments posted on the GitHub PR.

### The Solution
Implement a check in `ReviewJobService.java` to check the database before executing the review:

1. Query the last successful `Review` record for the given Pull Request.
2. Check if the current `head_sha` of the PR matches the `head_sha` of the last successful review.
3. If they match, log a message (e.g. `PR already reviewed for commit SHA: [sha]. Skipping.`) and return early.
