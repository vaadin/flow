# GitHub Issue Classification System

Classify and label GitHub issues in the vaadin/flow repository using LLM-assisted analysis.

## Quick Start

```bash
# 1. Set up the environment
cd scripts/issue_classifier
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# 2. Set your Anthropic API key
export ANTHROPIC_API_KEY="your-key-here"

# 3. Ensure gh CLI is authenticated
gh auth login

# 4. Run the workflow
cd scripts  # Must run from scripts/ directory
python -m issue_classifier fetch          # Fetch all issues
python -m issue_classifier classify --all # Classify with LLM
python -m issue_classifier review         # Review classifications
python -m issue_classifier apply --dry-run # Preview changes
python -m issue_classifier apply          # Apply labels
```

## Commands

| Command | Description |
|---------|-------------|
| `fetch` | Fetch all open issues from GitHub |
| `classify` | Classify issues using LLM |
| `resume` | Resume classification from checkpoint |
| `review` | Interactive review of classifications |
| `apply` | Apply approved label changes |
| `report` | Generate summary reports |
| `stats` | Show quick statistics |
| `show <number>` | Show details for a specific issue |

## Workflow

1. **Fetch** - Download all open issues to `data/issues/issues.jsonl`
2. **Classify** - Use Claude to analyze and classify each issue
3. **Review** - Manually approve/modify each classification (required)
4. **Apply** - Apply approved labels to GitHub

## Data Files

- `data/issues/issues.jsonl` - All issues in JSON Lines format
- `data/issues/issues_meta.json` - Progress metadata
- `data/issues/audit_log.jsonl` - Record of all label changes

## Classification Schema

Each issue is classified with:

- **Type**: `bug`, `enhancement`, or `feature request`
- **Impact** (bugs only): `High` or `Low`
- **Severity** (bugs only): `Major` or `Minor`
- **Modules**: Which parts of the codebase are affected
- **Good First Issue**: Whether suitable for new contributors

## Requirements

- Python 3.9+
- `gh` CLI authenticated (`gh auth login`)
- `ANTHROPIC_API_KEY` environment variable
