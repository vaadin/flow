# Testing

The rules for writing and debugging tests are in the Testing section of
[`CONVENTIONS.md`](../CONVENTIONS.md#testing). This chapter covers where the
tests live and how to build an integration test for a feature that wraps a
browser API.

## Where tests live

**Unit tests** live in `src/test/java/` in each module. They use JUnit and make
heavy use of Mockito, and they focus on the behavior of individual classes.

**Integration tests** live in `flow-tests/`. They use TestBench for browser
automation, exercise the full client-server interaction, and require a running
application server (the Maven build starts and stops it). See
[flow-tests/README.md](../flow-tests/README.md) for which module a new IT
belongs in.

**Client-side tests** live in `flow-client/src/test/frontend/`. They are
TypeScript tests run with Web Test Runner in a headless browser; run them with
`npm test` in `flow-client`.

## Testing a browser-API wrapper

A browser-facing feature needs an IT, and a real browser API is rarely
deterministic enough to test against — it asks for permission, depends on the
device, or never fails on demand. Mock the browser API in the test view
instead:

- Trigger the error branch with an option value rather than with timing. For
  geolocation, `maximumAge === -1` works.
- If the API streams updates (e.g. `watchPosition`), simulate them with
  `setInterval`, and verify that the matching cancel call (e.g. `clearWatch`)
  actually stops them.
- If floating-point arithmetic would make the assertions brittle, simplify the
  mock to emit stable values — different timestamps are enough to tell updates
  apart.
- Add a short settle pause before counting updates after a stop-like action.
  An event already in flight can still land right after the stop marker
  appears in the DOM.

## Waiting for a state

When an assertion becomes a wait, a bare `TimeoutException` does not say what
went wrong. `verifyTitle` in
[PageIT](../flow-tests/test-default/src/test/java/com/vaadin/flow/test/dom/PageIT.java)
shows how to keep the failure message, with the expected and the actual value.

Calling `checkLogsForErrors()` after each step rather than once at the end
means a failure points at the step that caused it.
