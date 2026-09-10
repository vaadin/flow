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

Every top-level Maven module of the repository:

| Module                           | Contents                                                                       |
| -------------------------------- | ------------------------------------------------------------------------------ |
| `flow-server`                    | Core server-side framework: state tree, DOM abstraction, routing, DI, frontend asset management. |
| `flow-client`                    | Client-side TypeScript/JavaScript engine.                                       |
| `flow-data`                      | Data binding and validation.                                                    |
| `flow-html-components`           | Basic HTML component wrappers (`Div`, `Anchor`, `NativeLabel`, …).              |
| `flow-html-components-testbench` | TestBench elements for the HTML components.                                     |
| `flow-react`                     | React integration.                                                              |
| `flow-push`                      | WebSocket-based push.                                                           |
| `flow-webpush`                   | Web Push notifications.                                                         |
| `flow-dnd`                       | Drag and drop.                                                                  |
| `flow-lit-template`              | Lit template support.                                                           |
| `flow-polymer-template`          | Polymer template support (legacy).                                              |
| `flow-polymer2lit`               | Polymer to Lit converter.                                                       |
| `flow-plugins`                   | Build plugins: `flow-plugin-base`, `flow-maven-plugin`, `flow-gradle-plugin`, `flow-dev-bundle-plugin`. |
| `flow-build-tools`               | Frontend build tooling shared by the plugins and the dev server.                |
| `flow-devloop-daemon`            | Daemon for the `vaadin-dev` dev loop.                                           |
| `vaadin-dev-server`              | Development tooling served to the browser.                                      |
| `vaadin-spring`                  | Spring Framework integration.                                                   |
| `flow-server-production-mode`    | Wrapper artifact whose `web-fragment.xml` turns on production mode.             |
| `flow-jandex`                    | Jandex index of the Flow packages, for use outside Vaadin Platform.             |
| `flow`                           | Aggregate POM that pulls in the modules an application needs.                   |
| `flow-bom`                       | Bill of materials.                                                              |
| `flow-test-util`                 | Test utilities (TestBench base classes, IT helpers).                            |
| `flow-test-generic`              | Generic test utilities shared by the modules.                                   |
| `flow-tests/`                    | Integration test suite.                                                         |

Routing lives in `flow-server` — there is no separate router module.

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
