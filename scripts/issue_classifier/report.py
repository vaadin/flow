"""Summary reports for issue classification."""

from rich.console import Console
from rich.panel import Panel
from rich.table import Table

from .classify import get_classification_stats
from .fetch import load_issues, load_metadata
from .review import get_review_stats

console = Console()


def print_progress_report() -> None:
    """Print a comprehensive progress report."""
    meta = load_metadata()
    issues = load_issues()

    if not issues:
        console.print("[yellow]No data available. Run fetch first.[/yellow]")
        return

    # Overall progress
    total = len(issues)
    classified = sum(1 for i in issues if i.get("classification") is not None)
    approved = sum(1 for i in issues if i.get("review_status") == "approved")
    applied = sum(1 for i in issues if i.get("review_status") == "applied")

    console.print("\n[bold]Issue Classification Progress Report[/bold]\n")

    # Progress table
    progress_table = Table(title="Pipeline Progress")
    progress_table.add_column("Stage", style="cyan")
    progress_table.add_column("Count", justify="right")
    progress_table.add_column("Percentage", justify="right")

    progress_table.add_row("Total Issues", str(total), "100%")
    progress_table.add_row(
        "Classified",
        str(classified),
        f"{classified * 100 // total}%" if total else "0%",
    )
    progress_table.add_row(
        "Approved",
        str(approved),
        f"{approved * 100 // total}%" if total else "0%",
    )
    progress_table.add_row(
        "Applied",
        str(applied),
        f"{applied * 100 // total}%" if total else "0%",
    )

    console.print(progress_table)

    # Classification stats
    stats = get_classification_stats()

    if stats.get("by_type"):
        console.print("\n[bold]Classification by Type[/bold]")
        type_table = Table()
        type_table.add_column("Type", style="cyan")
        type_table.add_column("Count", justify="right")

        for t, count in sorted(stats["by_type"].items(), key=lambda x: -x[1]):
            type_table.add_row(t, str(count))

        console.print(type_table)

    if stats.get("by_impact"):
        console.print("\n[bold]Impact Distribution (Bugs)[/bold]")
        impact_table = Table()
        impact_table.add_column("Impact", style="cyan")
        impact_table.add_column("Count", justify="right")

        for impact, count in sorted(stats["by_impact"].items()):
            impact_table.add_row(impact, str(count))

        console.print(impact_table)

    if stats.get("by_module"):
        console.print("\n[bold]Top Modules[/bold]")
        module_table = Table()
        module_table.add_column("Module", style="cyan")
        module_table.add_column("Count", justify="right")

        for mod, count in sorted(
            stats["by_module"].items(),
            key=lambda x: -x[1],
        )[:10]:
            module_table.add_row(mod, str(count))

        console.print(module_table)

    if stats.get("good_first_issues"):
        console.print(f"\n[green]Good First Issues: {stats['good_first_issues']}[/green]")

    # Triage stats
    if stats.get("triage"):
        triage = stats["triage"]
        console.print("\n[bold]Triage Analysis (Bugs)[/bold]")
        triage_table = Table()
        triage_table.add_column("Category", style="yellow")
        triage_table.add_column("Count", justify="right")

        triage_table.add_row("Needs Test Case", str(triage.get("needs_test_case", 0)))
        triage_table.add_row("AI Fixable", str(triage.get("ai_fixable", 0)))
        triage_table.add_row("Potentially Fixed", str(triage.get("potentially_fixed", 0)))
        triage_table.add_row("Potentially Outdated", str(triage.get("potentially_outdated", 0)))

        console.print(triage_table)

    # Metadata
    if meta:
        console.print(f"\n[dim]Last fetch: {meta.get('fetched_at', 'unknown')}[/dim]")


def print_label_summary() -> None:
    """Print a summary of proposed label changes."""
    issues = load_issues()

    if not issues:
        console.print("[yellow]No data available. Run fetch first.[/yellow]")
        return

    # Count proposed labels
    add_counts: dict[str, int] = {}
    remove_counts: dict[str, int] = {}

    for issue in issues:
        if issue.get("review_status") not in ("approved", "applied"):
            continue

        proposed = issue.get("proposed_labels", {})

        for label in proposed.get("add", []):
            add_counts[label] = add_counts.get(label, 0) + 1

        for label in proposed.get("remove", []):
            remove_counts[label] = remove_counts.get(label, 0) + 1

    console.print("\n[bold]Label Changes Summary[/bold]\n")

    if add_counts:
        console.print("[green]Labels to Add:[/green]")
        add_table = Table()
        add_table.add_column("Label", style="green")
        add_table.add_column("Count", justify="right")

        for label, count in sorted(add_counts.items(), key=lambda x: -x[1]):
            add_table.add_row(label, str(count))

        console.print(add_table)

    if remove_counts:
        console.print("\n[red]Labels to Remove:[/red]")
        remove_table = Table()
        remove_table.add_column("Label", style="red")
        remove_table.add_column("Count", justify="right")

        for label, count in sorted(remove_counts.items(), key=lambda x: -x[1]):
            remove_table.add_row(label, str(count))

        console.print(remove_table)

    total_add = sum(add_counts.values())
    total_remove = sum(remove_counts.values())

    console.print(f"\n[bold]Total: +{total_add} / -{total_remove} label operations[/bold]")


def print_unlabeled_report() -> None:
    """Print a report on unlabeled issues."""
    issues = load_issues()

    if not issues:
        console.print("[yellow]No data available. Run fetch first.[/yellow]")
        return

    unlabeled = [i for i in issues if not i.get("existing_labels")]

    console.print(f"\n[bold]Unlabeled Issues: {len(unlabeled)}[/bold]\n")

    if not unlabeled:
        console.print("[green]All issues have labels![/green]")
        return

    # Group by classification status
    classified = [i for i in unlabeled if i.get("classification")]
    unclassified = [i for i in unlabeled if not i.get("classification")]

    console.print(f"  Classified: {len(classified)}")
    console.print(f"  Unclassified: {len(unclassified)}")

    # Show oldest unlabeled
    console.print("\n[bold]Oldest Unlabeled Issues:[/bold]")

    table = Table()
    table.add_column("Issue", style="cyan")
    table.add_column("Age (days)", justify="right")
    table.add_column("Title", max_width=50)

    oldest = sorted(unlabeled, key=lambda x: -x.get("age_days", 0))[:10]

    for issue in oldest:
        table.add_row(
            f"#{issue['number']}",
            str(issue.get("age_days", 0)),
            issue["title"][:50],
        )

    console.print(table)


def export_good_first_issues() -> list[dict]:
    """Export list of Good First Issue candidates."""
    issues = load_issues()

    candidates = []
    for issue in issues:
        cls = issue.get("classification", {})
        if cls.get("good_first_issue"):
            candidates.append({
                "number": issue["number"],
                "title": issue["title"],
                "url": f"https://github.com/vaadin/flow/issues/{issue['number']}",
                "type": cls.get("type"),
                "confidence": cls.get("confidence", 0),
                "reasoning": cls.get("reasoning", ""),
            })

    return sorted(candidates, key=lambda x: -x.get("confidence", 0))


def print_good_first_issues() -> None:
    """Print Good First Issue candidates."""
    candidates = export_good_first_issues()

    if not candidates:
        console.print("[yellow]No Good First Issue candidates found.[/yellow]")
        return

    console.print(f"\n[bold]Good First Issue Candidates: {len(candidates)}[/bold]\n")

    table = Table()
    table.add_column("Issue", style="cyan")
    table.add_column("Type", style="dim")
    table.add_column("Confidence", justify="right")
    table.add_column("Title", max_width=50)

    for c in candidates[:20]:
        table.add_row(
            f"#{c['number']}",
            c.get("type", ""),
            f"{c.get('confidence', 0):.0%}",
            c["title"][:50],
        )

    console.print(table)

    if len(candidates) > 20:
        console.print(f"[dim]... and {len(candidates) - 20} more[/dim]")


def print_triage_report() -> None:
    """Print detailed triage analysis for bugs."""
    issues = load_issues()

    if not issues:
        console.print("[yellow]No data available. Run fetch first.[/yellow]")
        return

    # Collect bugs with triage info
    ai_fixable = []
    needs_test_case = []
    potentially_fixed = []
    potentially_outdated = []

    for issue in issues:
        cls = issue.get("classification", {})
        if cls.get("type") != "bug":
            continue

        triage = cls.get("triage", {})
        if not triage:
            continue

        issue_info = {
            "number": issue["number"],
            "title": issue["title"],
            "age_days": issue.get("age_days", 0),
            "confidence": cls.get("confidence", 0),
            "notes": triage.get("triage_notes", ""),
        }

        if triage.get("ai_fixable"):
            ai_fixable.append(issue_info)
        if triage.get("needs_test_case"):
            needs_test_case.append(issue_info)
        if triage.get("potentially_fixed"):
            potentially_fixed.append(issue_info)
        if triage.get("potentially_outdated"):
            potentially_outdated.append(issue_info)

    console.print("\n[bold]Triage Report[/bold]\n")

    # AI Fixable - most actionable
    console.print(f"[bold green]AI Fixable Issues: {len(ai_fixable)}[/bold green]")
    console.print("[dim]These bugs may be fixable by AI without additional information[/dim]\n")

    if ai_fixable:
        table = Table()
        table.add_column("Issue", style="cyan")
        table.add_column("Age", justify="right")
        table.add_column("Title", max_width=50)

        for i in sorted(ai_fixable, key=lambda x: -x["confidence"])[:15]:
            table.add_row(
                f"#{i['number']}",
                f"{i['age_days']}d",
                i["title"][:50],
            )

        console.print(table)
        if len(ai_fixable) > 15:
            console.print(f"[dim]... and {len(ai_fixable) - 15} more[/dim]")

    # Potentially outdated - cleanup candidates
    console.print(f"\n[bold yellow]Potentially Outdated: {len(potentially_outdated)}[/bold yellow]")
    console.print("[dim]May be related to deprecated features or old versions[/dim]\n")

    if potentially_outdated:
        table = Table()
        table.add_column("Issue", style="cyan")
        table.add_column("Age", justify="right")
        table.add_column("Title", max_width=50)

        for i in sorted(potentially_outdated, key=lambda x: -x["age_days"])[:15]:
            table.add_row(
                f"#{i['number']}",
                f"{i['age_days']}d",
                i["title"][:50],
            )

        console.print(table)
        if len(potentially_outdated) > 15:
            console.print(f"[dim]... and {len(potentially_outdated) - 15} more[/dim]")

    # Potentially fixed - verification candidates
    console.print(f"\n[bold blue]Potentially Fixed: {len(potentially_fixed)}[/bold blue]")
    console.print("[dim]May have been fixed already - needs verification[/dim]\n")

    if potentially_fixed:
        table = Table()
        table.add_column("Issue", style="cyan")
        table.add_column("Age", justify="right")
        table.add_column("Title", max_width=50)

        for i in sorted(potentially_fixed, key=lambda x: -x["age_days"])[:15]:
            table.add_row(
                f"#{i['number']}",
                f"{i['age_days']}d",
                i["title"][:50],
            )

        console.print(table)
        if len(potentially_fixed) > 15:
            console.print(f"[dim]... and {len(potentially_fixed) - 15} more[/dim]")

    # Needs test case - waiting for info
    console.print(f"\n[bold red]Needs Test Case: {len(needs_test_case)}[/bold red]")
    console.print("[dim]Need reproducible test case before they can be addressed[/dim]\n")

    if needs_test_case:
        table = Table()
        table.add_column("Issue", style="cyan")
        table.add_column("Age", justify="right")
        table.add_column("Title", max_width=50)

        for i in sorted(needs_test_case, key=lambda x: -x["age_days"])[:15]:
            table.add_row(
                f"#{i['number']}",
                f"{i['age_days']}d",
                i["title"][:50],
            )

        console.print(table)
        if len(needs_test_case) > 15:
            console.print(f"[dim]... and {len(needs_test_case) - 15} more[/dim]")
