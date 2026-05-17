# Commit and PR rules

## Commit message prefix for test-only fixes

Use `test:` instead of `fix:` when fixing only tests.

## Reference resolved issues in commits

When creating a commit that resolves an issue in the same repository, add
`Fixes #issuenumber` to the commit message so GitHub closes the issue
automatically on merge.

## PRs land as drafts

When creating a PR, mark it as a draft on GitHub and remind the user to
review the code themselves and mark the PR ready when satisfied.
