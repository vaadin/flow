# Flow Guidelines

These guidelines describe how features in Vaadin Flow — the Java server-side
framework of Vaadin Platform — should be designed and implemented in the `flow`
repository. Chapters can be read selectively for the topics your work touches.

Treat these as guidelines, not hard rules. They are best practices that should
be followed by default, but can be deviated from when necessary to make
something work.

The chapters explain; [`CONVENTIONS.md`](../CONVENTIONS.md) decides. A rule
that every change in its scope has to follow is written in `CONVENTIONS.md`, and a chapter
links to it instead of restating it — see its Conventions and Guidelines
section for the full division.
For repository-level commands (build, test, format) see [`CLAUDE.md`](../CLAUDE.md).
Architecture notes on individual features live in `ARCHITECTURE.md` files next
to their code and are listed at the top of [Architecture](architecture.md).

## Chapters

| Chapter                                     | Topic                                                                          |
| ------------------------------------------- | ------------------------------------------------------------------------------ |
| [Repository](repository.md)                 | Tech stack, module layout, where things live, Maven and build plugins.         |
| [Architecture](architecture.md)             | State tree, `Element`, the Jackson codec, routing, component development.      |
| [Design](design.md)                         | Shape of new public Java API: facades, signals, sealed types, naming, lifecycle. |
| [Browser Integration](browser-integration.md) | Wrapping browser APIs, `executeJs`, DOM events, bootstrap data, capability detection. |
| [Wire Protocol](wire-protocol.md)           | The UIDL response, the constant pool, how `executeJs` is encoded and executed. |
| [Frontend Hotswap](frontend-hotswap.md)     | Keeping generated frontend files in sync with Java classes in development mode. |
| [Documenting](documenting.md)               | Javadoc expectations, documenting wrapped browser APIs.                        |
| [Testing](testing.md)                       | Where tests live, testing a browser-API wrapper.                             |
