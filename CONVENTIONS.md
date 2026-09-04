# Conventions

The canonical list of checkable conventions for this repository. Read it in
full when authoring or reviewing code. Design-level reasoning behind several of
these rules lives in `guidelines/` — see `guidelines/overview.md`.

## Public API

Find the existing precedent before designing new API. Flow already has patterns
for per-UI facades, reactive signals, sealed result hierarchies, Jackson wire
records and browser-API wrappers — match the existing shape instead of
inventing a new one.

Expose observable state as a `Signal<T>` rather than as a listener API, and
name the accessor with a `Signal` suffix (`localeSignal()`,
`availabilitySignal()`).

Cache the read-only wrapper of a signal in a field instead of calling
`asReadonly()` per access. Every call allocates a fresh instance, so the
identity of the returned signal would otherwise be unstable.

Seed a signal with a meaningful default rather than `null`. When the initial
state is "no data yet", use a sentinel enum constant (e.g. `UNKNOWN`) or a
dedicated record (e.g. `Pending`) so callers can pattern match without a
`case null` arm.

Keep framework-only mutators off user-facing classes. The read surface belongs
on the user-facing class or facade, the write surface on `UIInternals` or an
equivalent internal-only class — a setter annotated "for framework use only" on
a class applications read from is a DX hazard.

Make the constructor of a stateful handle that an API hands out (e.g.
`GeolocationWatcher`) package-private, so application code can not bypass the
entry point that creates it.

Prefer an immutable record with a builder over a long parameter list for
tunable options, and validate in the compact constructor.

Do not introduce an interface and an abstract class as a pair speculatively.
Ship a single class named `Xyz` unless you can demonstrate today a useful
implementation that does not extend the abstract class. See
`guidelines/design.md` for the full rule set.

Use a sealed interface with record subtypes for values that are "one of N
things", and design for exhaustive `switch` expressions — do not add `default:`
arms over a sealed set.

Tie resources that outlive a single request (watches, DOM listeners, timers,
client-side subscriptions) to a component's lifecycle by accepting a
`Component owner` and registering a `DetachListener`. Expose an explicit,
idempotent `stop()` for mid-view cancellation.

Name a component that wraps an HTML element after the element itself, and add
the `Native` prefix only when the plain name is taken or when it invites a
mistake that goes unnoticed. Do not introduce further `Html…` names. See
`guidelines/design.md`.

Renaming an existing public class is a breaking change. Add the new class,
deprecate the old one with a `@deprecated` pointer to the replacement, and
remove it in the next major.

## Nullability

Apply `@NullMarked` (JSpecify) at the package level and annotate only what
genuinely may be null with `@Nullable`.

Prefer a sentinel value over a nullable return in the public API. Jackson wire
records are the legitimate exception, because the wire format permits
omissions — keep the wire record private and translate to a non-null public
shape at the boundary.

Put `@Nullable` on the declared type (`ValueSignal<@Nullable X>`). NullAway
infers it for the constructor, so no repeated type argument or type witness is
needed.

## Client-Side JavaScript

Never concatenate values into an `executeJs` expression string. Pass them as
parameters and reference them positionally (`$0`, `$1`, …).

Never build JSON by string concatenation. Use Jackson for construction.

Log client-side `executeJs` errors at `DEBUG`, not at `WARN` or `ERROR`. A
failed JS call usually means the feature is unavailable, not that there is a
server bug.

Put non-trivial JavaScript in its own file rather than inlining it in a Java
string, and prefer TypeScript for new files. See
`guidelines/browser-integration.md` for which of the two homes to use.

Keep client-side global state and helper functions under `window.Vaadin.Flow`.

Make `init(element)` installers idempotent. Track installations per element
with a `WeakMap` and dispose the previous listeners before attaching new ones.

Prefix custom DOM events with `vaadin-` (e.g. `vaadin-geolocation-position`).

Only write client code targeting the supported browsers listed in
`guidelines/browser-integration.md`. No fallbacks and no polyfills for anything
else.

Probe for feature availability without calling the feature itself, since
calling it usually triggers a permission prompt.

Update both sides in the same PR when a change touches the client-server
protocol or a DOM event contract.

## Build & Dependencies

Do not add a dev-runtime artifact as a `compile` or `runtime` scope dependency
of a build plugin module (`flow-plugins/flow-plugin-base`,
`flow-maven-plugin`, …). Plugin dependencies are resolved into the plugin
classloader before any goal runs, so they are downloaded by every build
including `-Pproduction`, they break offline and air-gapped builds, they show
up in SBOM audits, and every module that extends the plugin base inherits them.
When a goal needs a dev-only jar, resolve it from the project's own artifacts
at runtime and invoke it reflectively through a throwaway `URLClassLoader`.

Do not use `provided` scope as a workaround for that: Maven only loads
`compile` and `runtime` dependencies into the plugin realm, so `provided` turns
the problem into a `NoClassDefFoundError` at goal execution time.

Derive the version of a provisioned tool from the project's own dependency tree
instead of pinning it in the plugin, otherwise the pre-provisioned artifact
does not match what the running process expects and the network is hit anyway.

Extract a shared utility instead of copying a class or method between modules.
When two modules need the same logic, move it to the module they both depend
on.

## Javadoc

Javadoc explains the public contract, the why and the caveats — not the
internal implementation, and not what changed from a previous version. Change
history belongs in commit messages.

Do not add `@since` tags.

Java API that wraps a browser or JavaScript API must be written for Java
developers who do not know the underlying JS API. Document what the method does
in Java terms, when to call it, threading and lifecycle expectations, and
browser-specific caveats.

State reliability concerns prominently. If a value is best-effort, say so and
enumerate the browsers where it degrades.

## Testing

Write the tests that should pass first. If they expose problems in the
implementation, fix the implementation — do not rewrite the tests to match a
broken implementation.

Analyze why a test fails, code does not compile, or a build breaks, before
changing anything. Do not start rewriting code.

Keep the unit test count minimal — add only the essential cases.

Assert concrete outputs, not just "not null". Verify JSON structure and content
for serialization, and cover the edge cases that the change actually
introduces.

Add an integration test view under `flow-tests/test-root-context/` for
browser-facing features, and exercise both the happy path and the error branch.

Debug a failing integration test with Playwright before guessing. Look at what
the browser is actually doing.

## Code Style

Run `mvn spotless:apply` before every commit.

Names and comments describe how the code works and why, not what changed from a
previous version.

Use Java text blocks for multi-line strings instead of string concatenation.

## Commit & PR Hygiene

Prefix commit messages with the type: `feat:`, `fix:`, `test:`, `refactor:`,
`chore:`, `docs:`, `ci:`. Use `test:` when the change only touches tests, and
add `!` after the prefix for a breaking change (`chore!:`).

Add the affected module or area as a scope when it narrows the message
usefully — `fix(flow-client):`, `feat(hilla):`, `chore(deps):`. The scope is
optional; a bare `fix:` is fine for changes that span modules.

Add `Fixes #issuenumber` to the commit message when the commit resolves an
issue in this repository.

Open pull requests as drafts, and remind the author to self-review before
marking them ready.
