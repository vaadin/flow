"""Batch label updates for GitHub issues."""

import json
import subprocess
import time
from datetime import datetime, timezone

from rich.console import Console
from rich.panel import Panel
from rich.progress import BarColumn, Progress, TaskProgressColumn, TextColumn
from rich.table import Table

from .config import (
    APPLY_BATCH_DELAY_SECONDS,
    APPLY_BATCH_SIZE,
    AUDIT_LOG,
    ISSUES_FILE,
    REPO,
)
from .fetch import load_issues, load_metadata, update_metadata

console = Console()


def run_gh_command(args: list[str], check: bool = True) -> tuple[bool, str]:
    """Run a gh CLI command and return success status and output."""
    cmd = ["gh"] + args
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, check=check)
        return True, result.stdout
    except subprocess.CalledProcessError as e:
        return False, e.stderr


def add_label_to_issue(issue_number: int, label: str) -> bool:
    """Add a label to an issue."""
    success, output = run_gh_command([
        "issue", "edit",
        str(issue_number),
        "--repo", REPO,
        "--add-label", label,
    ], check=False)

    if not success:
        console.print(f"[red]Failed to add '{label}' to #{issue_number}: {output}[/red]")

    return success


def remove_label_from_issue(issue_number: int, label: str) -> bool:
    """Remove a label from an issue."""
    success, output = run_gh_command([
        "issue", "edit",
        str(issue_number),
        "--repo", REPO,
        "--remove-label", label,
    ], check=False)

    if not success:
        console.print(
            f"[red]Failed to remove '{label}' from #{issue_number}: {output}[/red]"
        )

    return success


def log_audit_entry(entry: dict) -> None:
    """Append an entry to the audit log."""
    with open(AUDIT_LOG, "a") as f:
        f.write(json.dumps(entry) + "\n")


def get_approved_issues() -> list[dict]:
    """Get all approved issues that have label changes."""
    issues = load_issues()

    approved = []
    for issue in issues:
        if issue.get("review_status") != "approved":
            continue

        proposed = issue.get("proposed_labels", {})
        if proposed.get("add") or proposed.get("remove"):
            approved.append(issue)

    return approved


def preview_changes() -> None:
    """Show a preview of all changes that would be applied."""
    approved = get_approved_issues()

    if not approved:
        console.print("[yellow]No approved issues with label changes[/yellow]")
        return

    console.print(f"\n[bold]Preview of {len(approved)} issues to update:[/bold]\n")

    table = Table()
    table.add_column("Issue", style="cyan")
    table.add_column("Title", max_width=40)
    table.add_column("Add Labels", style="green")
    table.add_column("Remove Labels", style="red")

    for issue in approved[:50]:  # Show first 50
        proposed = issue.get("proposed_labels", {})
        table.add_row(
            f"#{issue['number']}",
            issue["title"][:40],
            ", ".join(proposed.get("add", [])) or "-",
            ", ".join(proposed.get("remove", [])) or "-",
        )

    console.print(table)

    if len(approved) > 50:
        console.print(f"[dim]... and {len(approved) - 50} more issues[/dim]")

    # Summary
    total_adds = sum(
        len(i.get("proposed_labels", {}).get("add", []))
        for i in approved
    )
    total_removes = sum(
        len(i.get("proposed_labels", {}).get("remove", []))
        for i in approved
    )

    console.print()
    console.print(Panel(
        f"Issues: {len(approved)}\n"
        f"Labels to add: {total_adds}\n"
        f"Labels to remove: {total_removes}",
        title="Summary",
        border_style="blue",
    ))


def apply_changes(
    dry_run: bool = False,
    batch_size: int = APPLY_BATCH_SIZE,
    batch_delay: int = APPLY_BATCH_DELAY_SECONDS,
) -> dict:
    """Apply label changes to approved issues."""
    approved = get_approved_issues()

    if not approved:
        console.print("[yellow]No approved issues with label changes[/yellow]")
        return {"applied": 0, "failed": 0}

    if dry_run:
        console.print("[yellow]DRY RUN - No changes will be made[/yellow]")
        preview_changes()
        return {"applied": 0, "failed": 0, "dry_run": True}

    console.print(f"[blue]Applying changes to {len(approved)} issues[/blue]")

    issues = load_issues()
    issue_map = {i["number"]: idx for idx, i in enumerate(issues)}

    applied = 0
    failed = 0
    batch_count = 0

    with Progress(
        TextColumn("[progress.description]{task.description}"),
        BarColumn(),
        TaskProgressColumn(),
        console=console,
    ) as progress:
        task = progress.add_task("Applying labels...", total=len(approved))

        for i, issue in enumerate(approved):
            # Batch delay
            if i > 0 and i % batch_size == 0:
                batch_count += 1
                progress.update(
                    task,
                    description=f"Batch {batch_count} complete, waiting {batch_delay}s...",
                )
                time.sleep(batch_delay)

            proposed = issue.get("proposed_labels", {})
            issue_applied = True

            # Add labels
            for label in proposed.get("add", []):
                if not add_label_to_issue(issue["number"], label):
                    issue_applied = False

            # Remove labels
            for label in proposed.get("remove", []):
                if not remove_label_from_issue(issue["number"], label):
                    issue_applied = False

            if issue_applied:
                applied += 1

                # Log to audit
                log_audit_entry({
                    "timestamp": datetime.now(timezone.utc).isoformat(),
                    "issue_number": issue["number"],
                    "labels_added": proposed.get("add", []),
                    "labels_removed": proposed.get("remove", []),
                    "status": "success",
                })

                # Update issue status
                idx = issue_map[issue["number"]]
                issues[idx]["review_status"] = "applied"
                issues[idx]["applied_at"] = datetime.now(timezone.utc).isoformat()

            else:
                failed += 1
                log_audit_entry({
                    "timestamp": datetime.now(timezone.utc).isoformat(),
                    "issue_number": issue["number"],
                    "labels_added": proposed.get("add", []),
                    "labels_removed": proposed.get("remove", []),
                    "status": "failed",
                })

            progress.update(
                task,
                advance=1,
                description=f"Updated #{issue['number']}",
            )

    # Save updated issues
    with open(ISSUES_FILE, "w") as f:
        for issue in issues:
            f.write(json.dumps(issue) + "\n")

    # Update metadata
    meta = load_metadata() or {}
    update_metadata({"applied_count": meta.get("applied_count", 0) + applied})

    # Summary
    console.print()
    console.print(Panel(
        f"Applied: {applied}\nFailed: {failed}",
        title="Apply Summary",
        border_style="green" if failed == 0 else "yellow",
    ))

    return {"applied": applied, "failed": failed}


def verify_applied(sample_size: int = 10) -> None:
    """Verify labels were applied correctly by checking a sample."""
    issues = load_issues()

    # Get recently applied issues
    applied = [i for i in issues if i.get("review_status") == "applied"]

    if not applied:
        console.print("[yellow]No applied issues to verify[/yellow]")
        return

    # Sample
    import random
    sample = random.sample(applied, min(sample_size, len(applied)))

    console.print(f"[blue]Verifying {len(sample)} issues...[/blue]\n")

    verified = 0
    mismatched = 0

    for issue in sample:
        # Fetch current labels from GitHub
        success, output = run_gh_command([
            "issue", "view",
            str(issue["number"]),
            "--repo", REPO,
            "--json", "labels",
        ])

        if not success:
            console.print(f"[red]Failed to fetch #{issue['number']}[/red]")
            continue

        current_labels = set(
            label["name"]
            for label in json.loads(output).get("labels", [])
        )

        # Check if expected labels are present
        expected_adds = set(issue.get("proposed_labels", {}).get("add", []))
        expected_removes = set(issue.get("proposed_labels", {}).get("remove", []))

        adds_present = expected_adds.issubset(current_labels)
        removes_absent = expected_removes.isdisjoint(current_labels)

        if adds_present and removes_absent:
            console.print(f"[green]✓ #{issue['number']}[/green]")
            verified += 1
        else:
            console.print(f"[red]✗ #{issue['number']}[/red]")
            if not adds_present:
                missing = expected_adds - current_labels
                console.print(f"  [dim]Missing: {missing}[/dim]")
            if not removes_absent:
                still_present = expected_removes & current_labels
                console.print(f"  [dim]Still present: {still_present}[/dim]")
            mismatched += 1

    console.print()
    console.print(Panel(
        f"Verified: {verified}\nMismatched: {mismatched}",
        title="Verification Summary",
        border_style="green" if mismatched == 0 else "red",
    ))
