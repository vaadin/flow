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

Renaming an existing public class is a breaking change. Add the new class,
deprecate the old one with a `@deprecated` pointer to the replacement, and
remove it in the next major.

Put a new type in the package that matches its scope, not in the package of its
first caller. A second entry point for the same capability is normal, and
moving a public type afterwards is a breaking change.

Declare a type that only one class uses as a nested type inside that class
instead of giving it a file of its own.

A method that does not touch the state of the class it sits on belongs
elsewhere — do not add it to a central class just because that class is its
first caller. Search for an existing utility before writing a helper, and when
the helper is genuinely new, put it in the util class it belongs to
(`ReflectTools`, `FrontendUtils`, …) instead of keeping it private where it is
needed first.

Keep the surface of an internal class to what its callers need — static helpers
that only sibling classes and tests call are package private.

When API accepts a type the application writes — an annotated interface, a
class following a convention — validate every assumption about it where it is
accepted and throw with the reason. What does not satisfy the contract has to
fail at the call that accepts the type, with a message naming what is wrong,
rather than at some later point of use. See `guidelines/design.md`.

## Naming

Every method name contains a verb that says what the method does. A name that
is only a noun or a category — `header(…)`, `names(…)`, `report(…)` — leaves
the reader to open the method; `writeHeader(…)`, `collectNames(…)`,
`warnAboutMissingFiles(…)` say what happens. This holds for TypeScript as much
as for Java.

Name a type after what it is, not after what the code around it does with it. A
type the application writes is named for what it declares, not for the
machinery that consumes it.

Name a component that wraps an HTML element after the element itself, and add
the `Native` prefix only when the plain name is taken or when it invites a
mistake that goes unnoticed. Do not introduce further `Html…` names. See
`guidelines/design.md`.

Do not expose two names for the same value — a record component and a `get…`
method beside it returning the same thing is one accessor too many.

A second way to do what an existing method already does is an overload of that
method, not a new name. An overload is found by everyone who already calls the
method, while a separate name has to be discovered on its own. Spell out the
difference between the overloads in the Javadoc of both. See
`guidelines/design.md`.

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

Prefer parameters over variants in anything the build generates for the
client: one parameterized entry rather than one entry per case, since every
entry is content the bundle carries and the client has to look up.

Never send a server-side class or method name to the browser. Key what the
client has to look up by a hash of the content it runs, so renaming changes
nothing the browser holds, and keep readable identifiers for development-only
debug output.

Send a payload that repeats through the constant pool the client already
caches, and keep the message shape the same for a new path and the existing one
instead of adding a second cache beside it. Put arriving constants in the pool
before anything resolves a reference to them. See
`guidelines/wire-protocol.md` for what the pool holds today and what it costs.

Keep a wire object down to what the receiver cannot derive: no key it ignores,
no value it can read off the payload it already has.

Report a call the client cannot execute — an unknown function, a payload that
does not match it — through the error channel of the call, so a pending result
that can never run completes instead of hanging.

Implement a `VaadinHotswapper` for everything a class change needs beyond the
changed class itself, such as regenerating a file the build generates from Java
or updating what the browser already holds. Hotswapping through JRebel or
HotswapAgent is the main way a developer sees a change without a restart, and
it replaces only the class — the hotswapper does the rest and pushes it to the
browser with an HMR event.

Keep reading and patching a generated file inside the task that generates it. A
caller that reacts to a change asks the task to bring the file up to date; it
does not parse the format itself.

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

Do not skip a build step whose output the application needs. Reusing output
that predates the change is a broken application — do the work, or fail with a
message that names what is missing.

Follow the existing implementations when you add one to an extension point,
including one that is in review at the same time, instead of introducing a
second shape for the same thing.

## Javadoc

Do not add `@since` tags. What to write in Javadoc, and how to document a
wrapped browser API, is covered by `guidelines/documenting.md`.

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

Static-import the test helpers and call them unqualified —
`assertEquals(…)`, not `Assertions.assertEquals(…)`, and `mock(…)`, not
`Mockito.mock(…)`. This covers the JUnit assertions, Hamcrest `assertThat`
and its matchers, and the Mockito core methods (`mock`, `when`, `verify`,
`spy`, `doReturn`, …) that Sonar flags with `java:S8924`. It applies to new
tests and to the calls you are already changing: do not mass-convert an
unrelated file, and leave the remaining JUnit 4 tests until they are migrated.
Within a file you are changing, convert the rest of its calls too rather than
leaving two styles behind.

Add an integration test view under `flow-tests/test-root-context/` for
browser-facing features, and exercise both the happy path and the error branch.

Debug a failing integration test with Playwright before guessing. Look at what
the browser is actually doing.

Check the cases that already exist — including the ones you added earlier in
the same change — before adding a test, and extend one instead of adding a
near-duplicate. Two cases that differ only in the direction of the same
comparison (one parameter too many, one too few) are one case.

Do not add tests for behavior you did not change. When a change only replaces
the implementation behind an existing API, the tests that already cover it are
what proves it still works.

Test the class the test class is named after. When the assertions are about
what a collaborator does, the case belongs in that collaborator's test class.

Make a value that only has to differ look arbitrary. A byte-exact copy of real
data reads as a contract that the code does not have.

Build the state a case needs directly. Setup that writes a file, deletes it and
writes it again through another path hides what the case is about.

## Code Style

Run `mvn spotless:apply` before every commit.

Names and comments describe how the code works and why, not what changed from a
previous version.

Use Java text blocks for multi-line strings instead of string concatenation.

Delete code that cannot run: no null check for a value that is never null, no
production-mode check in a class that only runs in development, and no special
case for input the general path already handles — compare against empty content
instead of special-casing a missing file.

If a reviewer has to ask why a check is there, it either needs a comment that
says why, or it does not need to exist.

## Commit & PR Hygiene

The commit message format, the shape of a pull request description and what to
check before opening a PR live in
[`.claude/skills/commit-and-pr/SKILL.md`](.claude/skills/commit-and-pr/SKILL.md).
Read it before committing or opening a pull request.

Keep a pull request to one increment. When a follow-up — a second entry point,
converting the existing call sites — is a behavior change of its own, open it
on top of the branch under review instead of growing that branch.
