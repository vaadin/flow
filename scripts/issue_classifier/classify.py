"""LLM-assisted classification of GitHub issues."""

import json
import time
from typing import Any

from anthropic import Anthropic
from rich.console import Console
from rich.progress import BarColumn, Progress, TaskProgressColumn, TextColumn

from .config import (
    ANTHROPIC_MODEL,
    CLASSIFY_CHUNK_SIZE,
    GOOD_FIRST_ISSUE_CRITERIA,
    ISSUES_FILE,
    MODULE_KEYWORDS,
    TRIAGE_CRITERIA,
    get_anthropic_api_key,
)
from .fetch import load_issues, load_metadata, update_metadata

console = Console()


def build_classification_prompt(issue: dict) -> str:
    """Build the classification prompt for an issue."""
    module_context = "\n".join(
        f"- {module}: {', '.join(keywords)}"
        for module, keywords in MODULE_KEYWORDS.items()
    )

    return f"""Analyze this GitHub issue from the vaadin/flow repository and classify it.

## Issue Details

**Number:** #{issue['number']}
**Title:** {issue['title']}
**Age:** {issue['age_days']} days
**Author:** {issue['author']}
**Comments:** {issue['comment_count']}
**Existing Labels:** {', '.join(issue['existing_labels']) if issue['existing_labels'] else 'None'}

**Body:**
{issue['body_preview']}

## Classification Task

Classify this issue with the following:

1. **Type** (required): One of: bug, enhancement, feature request
   - bug: Something is broken or not working as expected
   - enhancement: Improvement to existing functionality
   - feature request: New functionality that doesn't exist yet

2. **Impact** (for bugs only): High or Low
   - High: Affects many users, blocks workflows, causes data loss, security issue
   - Low: Edge case, minor inconvenience, workaround available

3. **Severity** (for bugs only): Major or Minor
   - Major: Core functionality broken, crashes, security vulnerabilities
   - Minor: Cosmetic issues, non-critical bugs, minor inconveniences

4. **Modules** (if detectable): Which parts of the codebase are affected
   Module keywords for reference:
{module_context}

5. **Good First Issue** (yes/no): Would this be suitable for a new contributor?
{GOOD_FIRST_ISSUE_CRITERIA}

6. **Confidence** (0.0-1.0): How confident are you in this classification?

7. **Reasoning**: Brief explanation of your classification (1-2 sentences)

## Triage Analysis (for bugs only)

For bugs, also assess:

8. **Needs Test Case** (yes/no): Does this bug need a reproducible test case before it can be fixed?
{TRIAGE_CRITERIA['needs_test_case']}

9. **AI Fixable** (yes/no): Could an AI assistant likely fix this bug without further information?
{TRIAGE_CRITERIA['ai_fixable']}

10. **Potentially Fixed** (yes/no): Might this issue have already been fixed?
{TRIAGE_CRITERIA['potentially_fixed']}

11. **Potentially Outdated** (yes/no): Is this issue possibly outdated or about deprecated features?
{TRIAGE_CRITERIA['potentially_outdated']}

## Response Format

Respond with ONLY a JSON object in this exact format:
{{
  "type": "bug" | "enhancement" | "feature request",
  "impact": "High" | "Low" | null,
  "severity": "Major" | "Minor" | null,
  "modules": ["module1", "module2"],
  "good_first_issue": true | false,
  "confidence": 0.85,
  "reasoning": "Brief explanation",
  "triage": {{
    "needs_test_case": true | false,
    "ai_fixable": true | false,
    "potentially_fixed": true | false,
    "potentially_outdated": true | false,
    "triage_notes": "Brief notes on triage assessment"
  }}
}}

Important:
- impact and severity should only be set for bugs, otherwise null
- modules should be an empty array if no specific module is detectable
- Be conservative with good_first_issue - only mark true if clearly suitable
- triage should be null for non-bugs, only populated for bugs
- ai_fixable should be conservative - only true if clearly straightforward
"""


def parse_classification_response(response_text: str) -> dict | None:
    """Parse the LLM response into a classification dict."""
    try:
        # Try to extract JSON from the response
        text = response_text.strip()

        # Handle markdown code blocks
        if "```json" in text:
            start = text.find("```json") + 7
            end = text.find("```", start)
            text = text[start:end].strip()
        elif "```" in text:
            start = text.find("```") + 3
            end = text.find("```", start)
            text = text[start:end].strip()

        return json.loads(text)
    except json.JSONDecodeError as e:
        console.print(f"[red]Failed to parse classification response: {e}[/red]")
        console.print(f"[dim]Response was: {response_text[:500]}...[/dim]")
        return None


def classify_issue(client: Anthropic, issue: dict) -> dict | None:
    """Classify a single issue using the LLM."""
    prompt = build_classification_prompt(issue)

    try:
        response = client.messages.create(
            model=ANTHROPIC_MODEL,
            max_tokens=800,
            messages=[{"role": "user", "content": prompt}],
        )

        response_text = response.content[0].text
        return parse_classification_response(response_text)

    except Exception as e:
        console.print(f"[red]Error classifying issue #{issue['number']}: {e}[/red]")
        return None


def determine_proposed_labels(issue: dict, classification: dict) -> dict[str, list[str]]:
    """Determine which labels to add/remove based on classification."""
    existing = set(issue["existing_labels"])
    to_add = []
    to_remove = []

    # Type label
    type_label = classification.get("type")
    if type_label and type_label not in existing:
        to_add.append(type_label)

    # Impact label (for bugs)
    if classification.get("type") == "bug" and classification.get("impact"):
        impact_label = f"Impact: {classification['impact']}"
        if impact_label not in existing:
            to_add.append(impact_label)

    # Severity label (for bugs)
    if classification.get("type") == "bug" and classification.get("severity"):
        severity_label = f"Severity: {classification['severity']}"
        if severity_label not in existing:
            to_add.append(severity_label)

    # Good First Issue
    if classification.get("good_first_issue") and "Good First Issue" not in existing:
        to_add.append("Good First Issue")

    return {"add": to_add, "remove": to_remove}


def save_issue_update(issues: list[dict], index: int, issue: dict) -> None:
    """Save an updated issue back to the JSONL file."""
    issues[index] = issue

    with open(ISSUES_FILE, "w") as f:
        for iss in issues:
            f.write(json.dumps(iss) + "\n")


def classify_chunk(
    start_index: int,
    chunk_size: int = CLASSIFY_CHUNK_SIZE,
    rate_limit_delay: float = 0.5,
) -> int:
    """Classify a chunk of issues starting from the given index."""
    issues = load_issues()
    meta = load_metadata()

    if not issues:
        console.print("[yellow]No issues to classify. Run fetch first.[/yellow]")
        return 0

    total = len(issues)
    end_index = min(start_index + chunk_size, total)

    console.print(
        f"[blue]Classifying issues {start_index + 1} to {end_index} of {total}[/blue]"
    )

    # Initialize Anthropic client
    api_key = get_anthropic_api_key()
    client = Anthropic(api_key=api_key)

    classified_count = 0

    with Progress(
        TextColumn("[progress.description]{task.description}"),
        BarColumn(),
        TaskProgressColumn(),
        console=console,
    ) as progress:
        task = progress.add_task(
            "Classifying...",
            total=end_index - start_index,
        )

        for i in range(start_index, end_index):
            issue = issues[i]

            # Skip already classified issues
            if issue.get("classification") is not None:
                progress.update(task, advance=1)
                continue

            # Classify the issue
            classification = classify_issue(client, issue)

            if classification:
                issue["classification"] = classification
                issue["proposed_labels"] = determine_proposed_labels(
                    issue, classification
                )
                classified_count += 1

                # Save immediately after each classification
                save_issue_update(issues, i, issue)

            progress.update(
                task,
                advance=1,
                description=f"Classified #{issue['number']}",
            )

            # Rate limiting
            if i < end_index - 1:
                time.sleep(rate_limit_delay)

    # Update metadata
    update_metadata({
        "last_processed_index": end_index,
        "classified_count": meta.get("classified_count", 0) + classified_count,
    })

    console.print(f"[green]Classified {classified_count} issues in this chunk[/green]")
    return classified_count


def classify_all(
    chunk_size: int = CLASSIFY_CHUNK_SIZE,
    rate_limit_delay: float = 0.5,
) -> int:
    """Classify all unclassified issues."""
    issues = load_issues()

    if not issues:
        console.print("[yellow]No issues to classify. Run fetch first.[/yellow]")
        return 0

    total = len(issues)
    total_classified = 0

    # Find first unclassified issue
    start_index = 0
    for i, issue in enumerate(issues):
        if issue.get("classification") is None:
            start_index = i
            break
    else:
        console.print("[green]All issues are already classified![/green]")
        return 0

    console.print(f"[blue]Starting classification from issue {start_index + 1}[/blue]")

    while start_index < total:
        classified = classify_chunk(start_index, chunk_size, rate_limit_delay)
        total_classified += classified
        start_index += chunk_size

        if start_index < total:
            console.print(
                f"[dim]Progress: {start_index}/{total} "
                f"({start_index * 100 // total}%)[/dim]"
            )

    console.print(f"\n[bold green]Classification complete![/bold green]")
    console.print(f"  Total classified: {total_classified}")

    return total_classified


def resume_classification(
    chunk_size: int = CLASSIFY_CHUNK_SIZE,
    rate_limit_delay: float = 0.5,
) -> int:
    """Resume classification from where we left off."""
    meta = load_metadata()

    if not meta:
        console.print("[yellow]No metadata found. Run fetch first.[/yellow]")
        return 0

    start_index = meta.get("last_processed_index", 0)
    console.print(f"[blue]Resuming from index {start_index}[/blue]")

    return classify_all(chunk_size, rate_limit_delay)


def get_classification_stats() -> dict[str, Any]:
    """Get statistics about classification progress."""
    issues = load_issues()

    if not issues:
        return {"total": 0, "classified": 0, "pending": 0}

    classified = sum(1 for i in issues if i.get("classification") is not None)
    pending = len(issues) - classified

    # Count by type
    type_counts: dict[str, int] = {}
    impact_counts: dict[str, int] = {}
    module_counts: dict[str, int] = {}
    good_first_count = 0

    # Triage counts
    triage_counts = {
        "needs_test_case": 0,
        "ai_fixable": 0,
        "potentially_fixed": 0,
        "potentially_outdated": 0,
    }

    for issue in issues:
        if issue.get("classification"):
            cls = issue["classification"]

            # Type
            t = cls.get("type", "unknown")
            type_counts[t] = type_counts.get(t, 0) + 1

            # Impact
            if cls.get("impact"):
                impact_counts[cls["impact"]] = impact_counts.get(cls["impact"], 0) + 1

            # Modules
            for mod in cls.get("modules", []):
                module_counts[mod] = module_counts.get(mod, 0) + 1

            # Good First Issue
            if cls.get("good_first_issue"):
                good_first_count += 1

            # Triage stats (for bugs)
            triage = cls.get("triage")
            if triage:
                for key in triage_counts:
                    if triage.get(key):
                        triage_counts[key] += 1

    return {
        "total": len(issues),
        "classified": classified,
        "pending": pending,
        "by_type": type_counts,
        "by_impact": impact_counts,
        "by_module": module_counts,
        "good_first_issues": good_first_count,
        "triage": triage_counts,
    }
