# Repository

## Technology stack

- **Java 21+**, **Maven** (large multi-module project).
- **Jakarta EE** (not Java EE). Spring Boot 4 integration is available.
- **Jackson** for JSON serialization between Java objects and JavaScript.
- **Vite** for development mode with hot reload, and for production bundling.
- **TypeScript** for the client engine and for generated type definitions.
- **JUnit** and **Mockito** for unit tests, **Vaadin TestBench** for browser
  integration tests.

## Module structure

| Module                     | Contents                                                     |
| -------------------------- | ------------------------------------------------------------ |
| `flow-server`              | Core server-side framework: state tree, DOM abstraction, routing, DI, frontend asset management. |
| `flow-client`              | Client-side TypeScript/JavaScript engine.                    |
| `flow-data`                | Data binding and validation.                                  |
| `flow-html-components`     | Basic HTML component wrappers (`Div`, `Anchor`, `NativeLabel`, …). |
| `flow-react`               | React integration.                                            |
| `flow-push`                | WebSocket-based push.                                         |
| `flow-dnd`                 | Drag and drop.                                                |
| `flow-webpush`             | Web Push notifications.                                       |
| `flow-plugins`             | Maven and Gradle build plugins.                               |
| `flow-devloop-daemon`      | Daemon for the `vaadin-dev` dev loop.                         |
| `flow-tests/`              | Integration test suite.                                       |
| `vaadin-spring`            | Spring Framework integration.                                 |
| `vaadin-dev-server`        | Development tooling served to the browser.                    |

## Build plugins

`flow-plugins` holds the build-time tooling. Anything declared as a dependency
of a plugin module is loaded into the Maven plugin classloader before any goal
runs — in every build, including production ones. Keep dev-runtime artifacts
out of that dependency set and resolve them from the project's own artifacts at
goal execution time instead. See the Build & Dependencies section of
[`CONVENTIONS.md`](../CONVENTIONS.md).

## Code style

Formatting is applied by `mvn spotless:apply` and validated by
`mvn spotless:check` and `mvn checkstyle:check`. Run the formatter before every
commit — the CI validation job fails on unformatted code.

## Blast radius

This is a complex, interconnected system. Changes to core classes such as
`StateNode`, `Element` or the codec layer ripple across the codebase — plan for
full test runs and a longer review cycle. Frontend changes generally require
corresponding server-side changes in the same PR, and vice versa.
