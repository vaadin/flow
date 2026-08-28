---
name: vaadin-devloop
description: Requires Vaadin 25.3 or newer in the target application; on an older version the dev loop does not exist and none of this applies. Applies to any edit of Vaadin application source under src/main/java or src/main/resources — a view, component, layout, theme or stylesheet — and makes that edit live in the already-running app, then verifies it in the browser. Also covers starting or restarting the app and answering whether a change is actually live. A daemon owns the app process, so this replaces running the app through Maven.
when_to_use: Only for applications on Vaadin 25.3 or newer — verify that first and stop here if the version is older. The edit itself is the trigger, not any particular wording. Load this BEFORE touching a Vaadin view, component, layout, theme or stylesheet, and follow it again once the file is saved — a source edit that has not been applied is not done, however small the edit looks. Also triggers on starting or restarting the app, on checking whether a change is live, and on any request to look at the app in a browser. Phrases like "make this live", "is the change running", "start the app", "reload", "hot reload", "apply my edits" and "why doesn't the page show my change" are explicit invocations, but none of them are required.
allowed-tools: Bash(.vaadin/vaadin-dev *), Bash(bash .vaadin/vaadin-dev *), Bash(.vaadin/vaadin-dev.cmd *), Read
---

# Vaadin dev loop

The instructions are shared with every other agent on this repository and live in
**[`.agents/skills/vaadin-devloop/SKILL.md`](../../../.agents/skills/vaadin-devloop/SKILL.md)**.

**Read that file now** — it is the cycle, the command set, what is in the loop, and how to read
an `apply` outcome in one line each. Its
[reference.md](../../../.agents/skills/vaadin-devloop/reference.md) carries the detail: the full
output vocabulary, which edits need a page reload, pom/classpath semantics, the `--json` schema,
environment variables, and what to do when the loop goes wrong.

**Precondition — Vaadin 25.3 or newer.** The loop does not exist on older versions. Establish
the target application's version before the first `vaadin-dev` command, as the shared file's
*Requires Vaadin 25.3 or newer* section describes; if it is older, stop here and run the
application the project's normal way.

## Bindings for this session

The shared file is deliberately tool-agnostic. These are the tools to use for it here.

- Verify with the **Playwright MCP** tools: `browser_navigate` once, then `browser_snapshot` /
  `browser_evaluate` for the assertions the shared reference describes, and
  `browser_console_messages` after each change (a `/favicon.ico` 404 is normal noise). The
  first snapshot after `browser_navigate` is usually empty — Vaadin renders client-side.
- Use the **Vaadin MCP server** (`search_vaadin_docs`, `get_component_java_api`,
  `get_component_styling`, `get_theme_css_properties`) instead of recalling API from memory;
  check the Vaadin version in the target application's `pom.xml`.
- On Windows a checkout carries no executable bit, so `.vaadin/vaadin-dev` may refuse to
  run: use `bash .vaadin/vaadin-dev` in the Bash tool, or the `.cmd` launcher beside it
  from `cmd`/PowerShell. Same arguments, same exit codes.
