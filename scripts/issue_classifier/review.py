"""Interactive review interface for classified issues."""

import json
from typing import Literal

from rich.console import Console
from rich.panel import Panel
from rich.prompt import Confirm, Prompt
from rich.table import Table
from rich.text import Text

from .config import (
    COMMUNITY_LABELS,
    IMPACT_LABELS,
    ISSUES_FILE,
    SEVERITY_LABELS,
    TYPE_LABELS,
)
from .fetch import load_issues, load_metadata, update_metadata

console = Console()


def display_issue(issue: dict) -> None:
    """Display an issue for review."""
    # Header
    console.print()
    console.print(
        Panel(
            f"[bold]#{issue['number']}[/bold] {issue['title']}",
            title="Issue",
            border_style="blue",
        )
    )

    # Metadata table
    meta_table = Table(show_header=False, box=None, padding=(0, 2))
    meta_table.add_column("Key", style="dim")
    meta_table.add_column("Value")

    meta_table.add_row("Age", f"{issue['age_days']} days")
    meta_table.add_row("Author", issue.get("author", "unknown"))
    meta_table.add_row("Comments", str(issue.get("comment_count", 0)))
    meta_table.add_row(
        "Existing Labels",
        ", ".join(issue["existing_labels"]) if issue["existing_labels"] else "None",
    )

    console.print(meta_table)

    # Body preview
    console.print()
    console.print(Panel(issue["body_preview"] or "[dim]No body[/dim]", title="Body"))

    # Classification
    if issue.get("classification"):
        cls = issue["classification"]

        cls_table = Table(show_header=False, box=None, padding=(0, 2))
        cls_table.add_column("Key", style="cyan")
        cls_table.add_column("Value")

        cls_table.add_row("Type", cls.get("type", "unknown"))
        if cls.get("impact"):
            cls_table.add_row("Impact", cls["impact"])
        if cls.get("severity"):
            cls_table.add_row("Severity", cls["severity"])
        cls_table.add_row("Modules", ", ".join(cls.get("modules", [])) or "None")
        cls_table.add_row(
            "Good First Issue", "Yes" if cls.get("good_first_issue") else "No"
        )
        cls_table.add_row("Confidence", f"{cls.get('confidence', 0):.0%}")
        cls_table.add_row("Reasoning", cls.get("reasoning", ""))

        console.print()
        console.print(Panel(cls_table, title="LLM Classification", border_style="green"))

        # Triage analysis (for bugs)
        triage = cls.get("triage")
        if triage:
            triage_table = Table(show_header=False, box=None, padding=(0, 2))
            triage_table.add_column("Key", style="yellow")
            triage_table.add_column("Value")

            def yes_no_style(val: bool) -> str:
                return "[green]Yes[/green]" if val else "[dim]No[/dim]"

            triage_table.add_row(
                "Needs Test Case",
                yes_no_style(triage.get("needs_test_case", False)),
            )
            triage_table.add_row(
                "AI Fixable",
                yes_no_style(triage.get("ai_fixable", False)),
            )
            triage_table.add_row(
                "Potentially Fixed",
                yes_no_style(triage.get("potentially_fixed", False)),
            )
            triage_table.add_row(
                "Potentially Outdated",
                yes_no_style(triage.get("potentially_outdated", False)),
            )
            if triage.get("triage_notes"):
                triage_table.add_row("Notes", triage["triage_notes"])

            console.print()
            console.print(Panel(triage_table, title="Triage Analysis", border_style="yellow"))

    # Proposed labels
    if issue.get("proposed_labels"):
        props = issue["proposed_labels"]

        if props.get("add"):
            add_text = Text()
            for label in props["add"]:
                add_text.append(f"+ {label}", style="green")
                add_text.append("  ")
            console.print()
            console.print(Panel(add_text, title="Labels to Add", border_style="green"))

        if props.get("remove"):
            remove_text = Text()
            for label in props["remove"]:
                remove_text.append(f"- {label}", style="red")
                remove_text.append("  ")
            console.print()
            console.print(
                Panel(remove_text, title="Labels to Remove", border_style="red")
            )


def get_review_action() -> Literal["approve", "modify", "skip", "quit"]:
    """Get the user's review action."""
    console.print()
    console.print("[dim]Actions: [a]pprove, [m]odify, [s]kip, [q]uit[/dim]")
    action = Prompt.ask(
        "Action",
        choices=["a", "m", "s", "q", "approve", "modify", "skip", "quit"],
        default="a",
    )

    action_map = {
        "a": "approve",
        "m": "modify",
        "s": "skip",
        "q": "quit",
    }
    return action_map.get(action, action)


def modify_classification(issue: dict) -> dict:
    """Allow user to modify the classification."""
    cls = issue.get("classification", {}).copy()

    console.print("\n[yellow]Modify classification (press Enter to keep current):[/yellow]")

    # Type
    current_type = cls.get("type", "")
    type_choice = Prompt.ask(
        f"Type [{'/'.join(TYPE_LABELS)}]",
        default=current_type,
    )
    if type_choice in TYPE_LABELS:
        cls["type"] = type_choice

    # Impact (for bugs)
    if cls.get("type") == "bug":
        current_impact = cls.get("impact", "")
        impact_choice = Prompt.ask(
            "Impact [High/Low/none]",
            default=current_impact or "none",
        )
        if impact_choice in ["High", "Low"]:
            cls["impact"] = impact_choice
        elif impact_choice == "none":
            cls["impact"] = None

        # Severity
        current_severity = cls.get("severity", "")
        severity_choice = Prompt.ask(
            "Severity [Major/Minor/none]",
            default=current_severity or "none",
        )
        if severity_choice in ["Major", "Minor"]:
            cls["severity"] = severity_choice
        elif severity_choice == "none":
            cls["severity"] = None
    else:
        cls["impact"] = None
        cls["severity"] = None

    # Good First Issue
    gfi = Confirm.ask(
        "Good First Issue?",
        default=cls.get("good_first_issue", False),
    )
    cls["good_first_issue"] = gfi

    # Recalculate proposed labels
    proposed = determine_proposed_labels_from_cls(issue, cls)

    return {"classification": cls, "proposed_labels": proposed}


def determine_proposed_labels_from_cls(issue: dict, cls: dict) -> dict[str, list[str]]:
    """Determine proposed labels from a classification."""
    existing = set(issue["existing_labels"])
    to_add = []

    # Type label
    type_label = cls.get("type")
    if type_label and type_label not in existing:
        to_add.append(type_label)

    # Impact label (for bugs)
    if cls.get("type") == "bug" and cls.get("impact"):
        impact_label = f"Impact: {cls['impact']}"
        if impact_label not in existing:
            to_add.append(impact_label)

    # Severity label (for bugs)
    if cls.get("type") == "bug" and cls.get("severity"):
        severity_label = f"Severity: {cls['severity']}"
        if severity_label not in existing:
            to_add.append(severity_label)

    # Good First Issue
    if cls.get("good_first_issue") and "Good First Issue" not in existing:
        to_add.append("Good First Issue")

    return {"add": to_add, "remove": []}


def save_issues(issues: list[dict]) -> None:
    """Save all issues back to the JSONL file."""
    with open(ISSUES_FILE, "w") as f:
        for issue in issues:
            f.write(json.dumps(issue) + "\n")


def review_issues(start_from: int = 0, filter_status: str | None = None) -> None:
    """Interactive review of classified issues."""
    issues = load_issues()
    meta = load_metadata()

    if not issues:
        console.print("[yellow]No issues to review. Run fetch first.[/yellow]")
        return

    # Filter to classified, unreviewed issues
    review_queue = []
    for i, issue in enumerate(issues):
        if issue.get("classification") is None:
            continue
        if filter_status and issue.get("review_status") != filter_status:
            continue
        if issue.get("review_status") == "approved":
            continue
        review_queue.append((i, issue))

    if not review_queue:
        console.print("[green]No issues to review![/green]")
        return

    # Skip to start position
    review_queue = [(i, iss) for i, iss in review_queue if i >= start_from]

    console.print(
        f"[blue]Starting review of {len(review_queue)} issues[/blue]"
    )
    console.print("[dim]Tip: Issues must be approved before labels can be applied[/dim]")

    reviewed_count = 0
    approved_count = 0

    for idx, (i, issue) in enumerate(review_queue):
        console.print(f"\n[dim]─── Issue {idx + 1} of {len(review_queue)} ───[/dim]")

        display_issue(issue)

        action = get_review_action()

        if action == "quit":
            console.print("[yellow]Exiting review...[/yellow]")
            break

        elif action == "skip":
            console.print("[dim]Skipped[/dim]")
            continue

        elif action == "modify":
            modifications = modify_classification(issue)
            issue["classification"] = modifications["classification"]
            issue["proposed_labels"] = modifications["proposed_labels"]
            issue["review_status"] = "approved"
            issues[i] = issue
            save_issues(issues)
            console.print("[green]✓ Modified and approved[/green]")
            approved_count += 1

        elif action == "approve":
            issue["review_status"] = "approved"
            issues[i] = issue
            save_issues(issues)
            console.print("[green]✓ Approved[/green]")
            approved_count += 1

        reviewed_count += 1

    # Update metadata
    current_reviewed = meta.get("reviewed_count", 0) if meta else 0
    update_metadata({"reviewed_count": current_reviewed + approved_count})

    # Summary
    console.print()
    console.print(Panel(
        f"Reviewed: {reviewed_count}\nApproved: {approved_count}",
        title="Review Summary",
        border_style="green",
    ))


def get_review_stats() -> dict:
    """Get statistics about review progress."""
    issues = load_issues()

    if not issues:
        return {"total": 0, "classified": 0, "pending_review": 0, "approved": 0}

    classified = sum(1 for i in issues if i.get("classification") is not None)
    approved = sum(1 for i in issues if i.get("review_status") == "approved")
    pending_review = classified - approved

    return {
        "total": len(issues),
        "classified": classified,
        "pending_review": pending_review,
        "approved": approved,
    }


def review_single(issue_number: int) -> None:
    """Review a single issue by number."""
    issues = load_issues()

    for i, issue in enumerate(issues):
        if issue["number"] == issue_number:
            if not issue.get("classification"):
                console.print(
                    f"[yellow]Issue #{issue_number} has not been classified yet[/yellow]"
                )
                return

            display_issue(issue)
            action = get_review_action()

            if action == "approve":
                issue["review_status"] = "approved"
                issues[i] = issue
                save_issues(issues)
                console.print("[green]✓ Approved[/green]")

            elif action == "modify":
                modifications = modify_classification(issue)
                issue["classification"] = modifications["classification"]
                issue["proposed_labels"] = modifications["proposed_labels"]
                issue["review_status"] = "approved"
                issues[i] = issue
                save_issues(issues)
                console.print("[green]✓ Modified and approved[/green]")

            return

    console.print(f"[red]Issue #{issue_number} not found[/red]")
