"""Configuration constants for the issue classifier."""

import os
from pathlib import Path

# Repository settings
REPO = "vaadin/flow"

# File paths
PROJECT_ROOT = Path(__file__).parent.parent.parent
DATA_DIR = PROJECT_ROOT / "data" / "issues"
ISSUES_FILE = DATA_DIR / "issues.jsonl"
META_FILE = DATA_DIR / "issues_meta.json"
AUDIT_LOG = DATA_DIR / "audit_log.jsonl"

# API settings
FETCH_PAGE_SIZE = 100
CLASSIFY_CHUNK_SIZE = 20
APPLY_BATCH_SIZE = 30
APPLY_BATCH_DELAY_SECONDS = 60

# Classification labels
TYPE_LABELS = ["bug", "enhancement", "feature request"]
IMPACT_LABELS = ["Impact: High", "Impact: Low"]
SEVERITY_LABELS = ["Severity: Major", "Severity: Minor"]

MODULE_LABELS = {
    "signals": "signals",
    "binder": "flow-data",
    "navigation": "navigation",
    "push": "push",
    "dnd": "dnd",
    "spring": "vaadin-spring",
    "build-tools": "build-tools",
    "flow-server": "flow-server",
    "flow-client": "flow-client",
    "flow-data": "flow-data",
    "security": "security",
    "i18n": "i18n",
    "a11y": "accessibility",
    "pwa": "pwa",
    "testing": "testing",
}

COMMUNITY_LABELS = ["Good First Issue", "Help wanted"]

# Module detection keywords for LLM context
MODULE_KEYWORDS = {
    "signals": [
        "Signal", "reactive", "state management", "NumberSignal",
        "ListSignal", "computed", "effect"
    ],
    "navigation": [
        "Router", "route", "redirect", "BeforeEnter", "BeforeLeave",
        "navigation", "RouteConfiguration", "RouteParameters", "QueryParameters"
    ],
    "push": [
        "WebSocket", "Atmosphere", "push", "real-time", "PushConnection",
        "server push", "@Push"
    ],
    "binder": [
        "Binder", "binding", "validation", "converter", "Validator",
        "field binding", "bean validation"
    ],
    "build-tools": [
        "Maven", "Gradle", "Vite", "frontend build", "npm", "pnpm",
        "webpack", "bundle", "plugin"
    ],
    "flow-server": [
        "Element", "StateNode", "StateTree", "Component", "UI",
        "VaadinSession", "VaadinService"
    ],
    "flow-client": [
        "TypeScript", "JavaScript", "client-side", "Flow.ts",
        "frontend", "lit", "web component"
    ],
    "spring": [
        "Spring", "autowire", "Bean", "SpringUI", "VaadinServlet",
        "Spring Boot", "Spring Security"
    ],
    "security": [
        "security", "authentication", "authorization", "CSRF",
        "XSS", "injection", "vulnerability"
    ],
    "dnd": [
        "drag", "drop", "DnD", "drag and drop", "draggable"
    ],
}

# Good First Issue criteria
GOOD_FIRST_ISSUE_CRITERIA = """
A "Good First Issue" should meet these criteria:
- Clear, well-defined scope with a single specific task
- Documentation or test improvements
- Single-file changes likely
- No deep framework knowledge required
- Has a clear solution path that can be described
- Doesn't require understanding of complex internals
"""

# Triage analysis criteria
TRIAGE_CRITERIA = {
    "needs_test_case": """
A bug "needs a test case" if:
- The reporter hasn't provided steps to reproduce
- The issue description is vague about when/how it occurs
- There's no code sample or minimal reproduction
- The behavior is intermittent or environment-specific
- It's unclear what the expected vs actual behavior is
""",
    "ai_fixable": """
An issue is "likely AI-fixable without further info" if:
- The problem is clearly described with specific error messages
- The fix is localized to a small area of code
- It's a straightforward bug (typo, off-by-one, null check, etc.)
- The expected behavior is unambiguous
- No external dependencies or complex state involved
- Similar patterns exist elsewhere in the codebase
- It doesn't require deep domain knowledge or architectural decisions
""",
    "potentially_fixed": """
An issue is "potentially already fixed" if:
- It's older than 6 months and mentions a specific version
- It describes behavior that seems basic/critical (likely caught)
- The reporter hasn't responded to follow-ups
- Similar issues have been closed as fixed
- It mentions APIs or features that have been reworked
""",
    "potentially_outdated": """
An issue is "potentially outdated/deprecated" if:
- It references old Vaadin versions (< 23)
- It mentions deprecated APIs (Polymer, bower, webjars, etc.)
- It's about features that have been replaced (e.g., old build system)
- The technology stack mentioned is no longer supported
- It references removed or significantly changed APIs
""",
}

# LLM settings
ANTHROPIC_MODEL = "claude-sonnet-4-20250514"
MAX_BODY_PREVIEW_LENGTH = 2000


def get_anthropic_api_key() -> str:
    """Get the Anthropic API key from environment."""
    key = os.environ.get("ANTHROPIC_API_KEY")
    if not key:
        raise ValueError(
            "ANTHROPIC_API_KEY environment variable is not set. "
            "Please set it to use LLM classification."
        )
    return key
