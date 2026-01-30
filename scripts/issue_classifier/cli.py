"""CLI entry point for the issue classifier."""

import click
from rich.console import Console

console = Console()


@click.group()
@click.version_option(version="0.1.0")
def cli() -> None:
    """GitHub Issue Classification System for vaadin/flow.

    This tool helps classify and label GitHub issues using LLM assistance.
    All classifications require manual review before labels are applied.
    """
    pass


@cli.command()
@click.option("--verify", is_flag=True, help="Verify fetch against GitHub count")
def fetch(verify: bool) -> None:
    """Fetch all open issues from GitHub.

    Uses the gh CLI to fetch issues and stores them in data/issues/issues.jsonl.
    Requires gh CLI to be authenticated (gh auth login).
    """
    from .fetch import fetch_and_save, verify_fetch

    count = fetch_and_save()

    if verify and count > 0:
        verify_fetch()


@cli.command()
@click.option(
    "--chunk-size",
    default=20,
    help="Number of issues to classify per chunk",
)
@click.option(
    "--rate-limit",
    default=0.5,
    help="Delay between API calls in seconds",
)
@click.option(
    "--all",
    "classify_all_flag",
    is_flag=True,
    help="Classify all unclassified issues",
)
def classify(chunk_size: int, rate_limit: float, classify_all_flag: bool) -> None:
    """Classify issues using LLM.

    Requires ANTHROPIC_API_KEY environment variable to be set.
    Classifications are saved incrementally and can be resumed.
    """
    from .classify import classify_all, classify_chunk, get_classification_stats
    from .fetch import load_metadata

    meta = load_metadata()
    if not meta:
        console.print(
            "[red]No issues found. Run 'fetch' first.[/red]"
        )
        return

    if classify_all_flag:
        classify_all(chunk_size, rate_limit)
    else:
        start_index = meta.get("last_processed_index", 0)
        classify_chunk(start_index, chunk_size, rate_limit)

    # Show stats
    stats = get_classification_stats()
    console.print(
        f"\n[dim]Progress: {stats['classified']}/{stats['total']} classified[/dim]"
    )


@cli.command()
@click.option(
    "--chunk-size",
    default=20,
    help="Number of issues to classify per chunk",
)
@click.option(
    "--rate-limit",
    default=0.5,
    help="Delay between API calls in seconds",
)
def resume(chunk_size: int, rate_limit: float) -> None:
    """Resume classification from last checkpoint."""
    from .classify import resume_classification

    resume_classification(chunk_size, rate_limit)


@cli.command()
@click.option(
    "--start-from",
    default=0,
    help="Start review from this index",
)
@click.option(
    "--issue",
    "issue_number",
    type=int,
    help="Review a single issue by number",
)
def review(start_from: int, issue_number: int | None) -> None:
    """Interactive review of classified issues.

    All classifications must be approved before labels can be applied.
    Use arrow keys or type the action letter to select an option.
    """
    from .review import get_review_stats, review_issues, review_single

    if issue_number:
        review_single(issue_number)
    else:
        # Show stats first
        stats = get_review_stats()
        console.print(
            f"[dim]Pending review: {stats['pending_review']} | "
            f"Approved: {stats['approved']}[/dim]\n"
        )
        review_issues(start_from)


@cli.command()
@click.option("--dry-run", is_flag=True, help="Preview changes without applying")
@click.option(
    "--batch-size",
    default=30,
    help="Number of issues per batch",
)
@click.option(
    "--batch-delay",
    default=60,
    help="Delay between batches in seconds",
)
@click.option("--verify", is_flag=True, help="Verify applied labels")
def apply(dry_run: bool, batch_size: int, batch_delay: int, verify: bool) -> None:
    """Apply approved label changes to GitHub.

    Only issues with review_status='approved' will be updated.
    Uses the gh CLI to apply labels.
    """
    from .apply import apply_changes, preview_changes, verify_applied

    if verify:
        verify_applied()
        return

    if dry_run:
        preview_changes()
    else:
        result = apply_changes(
            dry_run=False,
            batch_size=batch_size,
            batch_delay=batch_delay,
        )

        if result.get("applied", 0) > 0:
            console.print(
                "\n[dim]Run 'apply --verify' to verify the changes[/dim]"
            )


@cli.command()
@click.option(
    "--type",
    "report_type",
    type=click.Choice(["progress", "labels", "unlabeled", "good-first", "triage"]),
    default="progress",
    help="Type of report to generate",
)
def report(report_type: str) -> None:
    """Generate summary reports.

    Available reports:
    - progress: Overall classification progress
    - labels: Summary of proposed label changes
    - unlabeled: Report on unlabeled issues
    - good-first: Good First Issue candidates
    - triage: Triage analysis (AI fixable, outdated, etc.)
    """
    from .report import (
        print_good_first_issues,
        print_label_summary,
        print_progress_report,
        print_triage_report,
        print_unlabeled_report,
    )

    if report_type == "progress":
        print_progress_report()
    elif report_type == "labels":
        print_label_summary()
    elif report_type == "unlabeled":
        print_unlabeled_report()
    elif report_type == "good-first":
        print_good_first_issues()
    elif report_type == "triage":
        print_triage_report()


@cli.command()
def stats() -> None:
    """Show quick statistics."""
    from .classify import get_classification_stats
    from .fetch import load_issues, load_metadata
    from .review import get_review_stats

    meta = load_metadata()
    issues = load_issues()

    if not issues:
        console.print("[yellow]No data available. Run fetch first.[/yellow]")
        return

    review_stats = get_review_stats()
    class_stats = get_classification_stats()

    console.print("\n[bold]Quick Stats[/bold]\n")
    console.print(f"  Total issues: {len(issues)}")
    console.print(f"  Classified: {class_stats['classified']}")
    console.print(f"  Pending review: {review_stats['pending_review']}")
    console.print(f"  Approved: {review_stats['approved']}")

    if meta:
        console.print(f"\n  [dim]Last fetch: {meta.get('fetched_at', 'unknown')}[/dim]")


@cli.command()
@click.argument("issue_number", type=int)
def show(issue_number: int) -> None:
    """Show details for a specific issue."""
    from .fetch import load_issues
    from .review import display_issue

    issues = load_issues()

    for issue in issues:
        if issue["number"] == issue_number:
            display_issue(issue)
            return

    console.print(f"[red]Issue #{issue_number} not found[/red]")


def main() -> None:
    """Main entry point."""
    cli()


if __name__ == "__main__":
    main()
