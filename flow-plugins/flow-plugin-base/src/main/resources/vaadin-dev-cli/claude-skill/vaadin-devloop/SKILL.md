---
name: vaadin-devloop
{{shared-description}}
when_to_use: Load this BEFORE the first edit to a Vaadin view, component, layout, theme, stylesheet or frontend file, and follow it again once the file is saved — the edit is the trigger, no particular wording is required. Use it to run the app instead of `mvn spring-boot:run` or an IDE run configuration, and to find out whether an edit works instead of `mvn compile` or `mvn test`. Also triggers on starting or restarting the app, on checking whether a change is live, and on any request to look at the app in a browser. Requires Vaadin 25.3 or newer in the target application (`vaadin.version` in its pom.xml); stop here and run the app the project's normal way if it is older.
---

{{shared-instructions}}

## Tooling notes for Claude Code

The instructions above are shared with every other agent on this repository and are deliberately
tool-agnostic. These are the tools to reach for here.

Every tool named below is a **preference with a fallback beside it, never a requirement**. The
loop runs on the `vaadin-dev` CLI and nothing else. A missing MCP server changes which fallback
you take — it never means the work cannot be done, and it is never a reason to stop, to skip
verification silently, or to build tooling of your own. Take the fallback and say which one you
took.

- **Editing.** This skill restricts no tools on purpose. You are expected to Edit and Write
  application source while it is loaded — batching edits and then applying them *is* the cycle,
  so stay in the skill rather than loading it once the work is already finished.
- **Browser verification** — the cycle's step 5, for a change with a visual surface.
  *Preferred:* a Playwright or browser MCP server. `browser_navigate` once, before the first
  `apply` (a CSS push needs a page already connected), then `browser_snapshot` /
  `browser_evaluate` for the assertions the shared reference describes, and
  `browser_console_messages` after each change (a `/favicon.ico` 404 is normal noise). The first
  snapshot after navigating is usually empty — Vaadin renders client-side, so wait for a known
  element or re-snapshot before asserting. *Without one:* extend the browser tests the project
  already has — a `*BrowserTest` or `*IT` class and the element API it already depends on — and
  say you verified there. Do not write a new browser harness for one change: that costs more
  than the change, and *Verifying in the browser* in
  [reference.md](../../../.agents/skills/vaadin-devloop/reference.md) says what a run with no
  browser at all can and cannot claim.
- **Vaadin API and docs.** *Preferred:* the Vaadin MCP server (`search_vaadin_docs`,
  `get_component_java_api`, `get_component_styling`, `get_theme_css_properties`), against the
  version in the application's `pom.xml`. *Without it:* vaadin.com/docs for that version, or
  `./mvnw -q dependency:sources` once and read the sources. Reading `javap` output out of
  `~/.m2` is the last resort — one method name per call, it spends minutes on what either of the
  other two answers at once.
- **Windows.** A checkout carries no executable bit, so `.vaadin/vaadin-dev` may refuse to run:
  use `bash .vaadin/vaadin-dev` in the Bash tool, or the `.cmd` launcher beside it from
  `cmd`/PowerShell. Same arguments, same exit codes.
