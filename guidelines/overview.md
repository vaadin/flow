# Flow Guidelines

These guidelines describe how features in Vaadin Flow — the Java server-side
framework of Vaadin Platform — should be designed and implemented in the `flow`
repository. Chapters can be read selectively for the topics your work touches.

Treat these as guidelines, not hard rules. They are best practices that should
be followed by default, but can be deviated from when necessary to make
something work.

For the canonical list of checkable conventions see [`CONVENTIONS.md`](../CONVENTIONS.md).
For repository-level commands (build, test, format) see [`CLAUDE.md`](../CLAUDE.md).

## Chapters

| Chapter                                     | Topic                                                                          |
| ------------------------------------------- | ------------------------------------------------------------------------------ |
| [Repository](repository.md)                 | Tech stack, module layout, where things live, Maven and build plugins.         |
| [Architecture](architecture.md)             | State tree, `Element`, the Jackson codec, routing, component development.      |
| [Design](design.md)                         | Shape of new public Java API: facades, signals, sealed types, naming, lifecycle. |
| [Browser Integration](browser-integration.md) | Wrapping browser APIs, `executeJs`, DOM events, bootstrap data, capability detection. |
| [Documenting](documenting.md)               | Javadoc expectations, documenting wrapped browser APIs.                        |
| [Testing](testing.md)                       | Unit tests, integration tests, debugging failures.                             |
