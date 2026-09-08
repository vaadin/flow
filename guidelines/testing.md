# Testing

## Where tests live

**Unit tests** live in `src/test/java/` in each module. They use JUnit and make
heavy use of Mockito, and they focus on the behavior of individual classes.

**Integration tests** live in `flow-tests/`. They use TestBench for browser
automation, exercise the full client-server interaction, and require a running
application server (the Maven build starts and stops it).

## Writing tests

- **Write the tests that should pass first.** If they expose problems in
  the implementation, fix the implementation afterwards — don't rewrite
  the tests to match a broken implementation.
- Keep the unit test count minimal — only the essential cases. More tests
  are not better; focused tests are.
- For browser-facing features, add an IT view under
  `flow-tests/test-root-context/` that mocks the relevant browser API
  and exercises both happy-path and error branches. Use an option
  value to trigger errors deterministically (for geolocation,
  `maximumAge === -1` works). If the API streams updates (e.g.
  `watchPosition`), use `setInterval` to simulate them and verify
  that the matching cancel call (e.g. `clearWatch`) actually stops
  them.
- ITs assert concrete outputs, not just "not null". If floating-point
  arithmetic would make assertions brittle, simplify the mock to emit
  stable values (different timestamps suffice for uniqueness).
- Add a short settle pause before snapshotting counts after a
  stop-like action — an in-flight event can still land right after the
  stop marker appears in the DOM.
- When improving existing tests, verify actual behavior rather than just
  "not null" — assert the JSON structure and content for serialization, and
  cover the edge cases the change actually introduces.

## Debugging failures

- Analyze *why* a test fails, code does not compile, or a build breaks, before
  changing anything. Do not start rewriting code.
- When an integration test fails, use Playwright to see what the browser is
  actually doing in the UI, rather than guessing at the cause.
