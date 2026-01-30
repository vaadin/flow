"""Fetch issues from GitHub using gh CLI."""

import json
import subprocess
from datetime import datetime, timezone
from pathlib import Path

from rich.console import Console
from rich.progress import Progress, SpinnerColumn, TextColumn

from .config import (
    DATA_DIR,
    FETCH_PAGE_SIZE,
    ISSUES_FILE,
    MAX_BODY_PREVIEW_LENGTH,
    META_FILE,
    REPO,
)

console = Console()


def run_gh_command(args: list[str]) -> str:
    """Run a gh CLI command and return the output."""
    cmd = ["gh"] + args
    result = subprocess.run(cmd, capture_output=True, text=True, check=True)
    return result.stdout


def fetch_issues_page(page: int, per_page: int = FETCH_PAGE_SIZE) -> list[dict]:
    """Fetch a single page of issues from GitHub."""
    # Calculate the range for this page
    # gh doesn't support pagination directly, so we use --limit with offset simulation
    # We'll fetch all at once with a high limit since gh handles it efficiently
    args = [
        "issue", "list",
        "--repo", REPO,
        "--state", "open",
        "--json", "number,title,body,labels,createdAt,updatedAt,author,comments",
        "--limit", str(per_page),
    ]

    output = run_gh_command(args)
    return json.loads(output) if output.strip() else []


def fetch_all_issues() -> list[dict]:
    """Fetch all open issues from the repository."""
    console.print(f"[blue]Fetching all open issues from {REPO}...[/blue]")

    # gh CLI handles pagination internally when we use a high limit
    args = [
        "issue", "list",
        "--repo", REPO,
        "--state", "open",
        "--json", "number,title,body,labels,createdAt,updatedAt,author,comments",
        "--limit", "5000",  # High limit to get all issues
    ]

    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        console=console,
    ) as progress:
        task = progress.add_task("Fetching issues from GitHub...", total=None)
        output = run_gh_command(args)
        issues = json.loads(output) if output.strip() else []
        progress.update(task, description=f"Fetched {len(issues)} issues")

    return issues


def transform_issue(issue: dict) -> dict:
    """Transform a raw GitHub issue into our tracking format."""
    now = datetime.now(timezone.utc)
    created_at = datetime.fromisoformat(issue["createdAt"].replace("Z", "+00:00"))
    age_days = (now - created_at).days

    # Extract label names
    existing_labels = [label["name"] for label in issue.get("labels", [])]

    # Truncate body for preview
    body = issue.get("body") or ""
    body_preview = body[:MAX_BODY_PREVIEW_LENGTH]
    if len(body) > MAX_BODY_PREVIEW_LENGTH:
        body_preview += "..."

    return {
        "number": issue["number"],
        "title": issue["title"],
        "body_preview": body_preview,
        "created_at": issue["createdAt"],
        "age_days": age_days,
        "existing_labels": existing_labels,
        "author": issue.get("author", {}).get("login", "unknown"),
        "comment_count": issue.get("comments", 0),
        "classification": None,
        "proposed_labels": None,
        "review_status": "pending",
    }


def save_issues(issues: list[dict], filepath: Path = ISSUES_FILE) -> None:
    """Save issues to JSONL file."""
    filepath.parent.mkdir(parents=True, exist_ok=True)

    with open(filepath, "w") as f:
        for issue in issues:
            f.write(json.dumps(issue) + "\n")

    console.print(f"[green]Saved {len(issues)} issues to {filepath}[/green]")


def save_metadata(total: int, fetched_at: str) -> None:
    """Save metadata about the fetch operation."""
    meta = {
        "total_issues": total,
        "fetched_at": fetched_at,
        "last_processed_index": 0,
        "classified_count": 0,
        "reviewed_count": 0,
        "applied_count": 0,
    }

    with open(META_FILE, "w") as f:
        json.dump(meta, f, indent=2)

    console.print(f"[green]Saved metadata to {META_FILE}[/green]")


def load_issues(filepath: Path = ISSUES_FILE) -> list[dict]:
    """Load issues from JSONL file."""
    if not filepath.exists():
        return []

    issues = []
    with open(filepath) as f:
        for line in f:
            if line.strip():
                issues.append(json.loads(line))

    return issues


def load_metadata() -> dict | None:
    """Load metadata from file."""
    if not META_FILE.exists():
        return None

    with open(META_FILE) as f:
        return json.load(f)


def update_metadata(updates: dict) -> None:
    """Update metadata file with new values."""
    meta = load_metadata() or {}
    meta.update(updates)

    with open(META_FILE, "w") as f:
        json.dump(meta, f, indent=2)


def fetch_and_save() -> int:
    """Main fetch operation: fetch all issues and save to file."""
    # Ensure data directory exists
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    # Fetch all issues
    raw_issues = fetch_all_issues()

    if not raw_issues:
        console.print("[yellow]No issues found![/yellow]")
        return 0

    # Transform to our format
    console.print("[blue]Transforming issues...[/blue]")
    transformed = [transform_issue(issue) for issue in raw_issues]

    # Sort by issue number (oldest first)
    transformed.sort(key=lambda x: x["number"])

    # Save issues
    save_issues(transformed)

    # Save metadata
    fetched_at = datetime.now(timezone.utc).isoformat()
    save_metadata(len(transformed), fetched_at)

    # Print summary
    console.print("\n[bold green]Fetch complete![/bold green]")
    console.print(f"  Total issues: {len(transformed)}")

    # Count issues by label status
    unlabeled = sum(1 for i in transformed if not i["existing_labels"])
    console.print(f"  Unlabeled issues: {unlabeled} ({unlabeled*100//len(transformed)}%)")

    return len(transformed)


def verify_fetch() -> bool:
    """Verify the fetch by comparing with gh issue count."""
    console.print("[blue]Verifying fetch...[/blue]")

    # Get count from gh CLI
    args = [
        "issue", "list",
        "--repo", REPO,
        "--state", "open",
        "--json", "number",
        "--limit", "5000",
    ]
    output = run_gh_command(args)
    gh_count = len(json.loads(output)) if output.strip() else 0

    # Get count from our file
    issues = load_issues()
    file_count = len(issues)

    console.print(f"  GitHub count: {gh_count}")
    console.print(f"  File count: {file_count}")

    if gh_count == file_count:
        console.print("[green]✓ Counts match![/green]")
        return True
    else:
        console.print(f"[yellow]⚠ Counts differ by {abs(gh_count - file_count)}[/yellow]")
        return False
