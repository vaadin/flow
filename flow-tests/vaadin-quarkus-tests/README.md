# Vaadin Quarkus tests

Integration tests for `vaadin-quarkus`. Each module boots a Quarkus
application of its own instead of deploying to a shared servlet container, so
the tests are run one module at a time:

```sh
mvn verify -f flow-tests/vaadin-quarkus-tests/pom.xml -pl production
```

The modules are:

- `common-test-code` — the views and the IT classes that `production` and
  `development` both run; on its own it only checks that they compile
- `production` / `development` — the same application in production and in
  development mode
- `embedded-plugin` — the same application built by the Vaadin plugin the
  extension embeds rather than by `flow-maven-plugin`; this is also the module
  the native image build uses
- `push-dispatch-it` — a deadlock regression test for the Push dispatch
  default the extension sets
- `codestarts` — generates a project from the extension's codestart and
  compares it against the snapshots in `src/test/resources/__snapshots__`;
  drives no browser

`reusable-theme`, `custom-websocket-dependency` and `test-addons/*` are
fixtures the application modules depend on, not test modules.

Everything but `codestarts` drives a real Chrome through TestBench, so a
`~/.vaadin/proKey` is required and browser windows open while they run.

Validation runs `embedded-plugin`, `production` and `codestarts` for every
change, and `development` and `push-dispatch-it` when the Quarkus sources
themselves change; see the `quarkus-tests` job in
`.github/workflows/validation.yml`. The native image build needs GraalVM and
takes about half an hour, so it runs nightly instead — see
`.github/workflows/quarkus-native.yml`.

The tests tagged `slow` are excluded by default:

```sh
mvn verify -f flow-tests/vaadin-quarkus-tests/pom.xml -pl production -Pslow-tests
```
