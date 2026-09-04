# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

Vaadin Flow is the Java framework of Vaadin Platform for building modern web
applications. This is a large, multi-module Maven project that combines
server-side Java components with modern frontend tooling (Vite, TypeScript,
React support).

### Technologies

- Java 21+, Maven
- Jakarta EE (not Java EE), Spring Boot 4 integration
- Jackson for client-server JSON serialization
- Vite for frontend builds, TypeScript for the client engine
- JUnit and Mockito for unit tests, Vaadin TestBench for integration tests

### Key Modules

- `flow-server`: core server-side framework (state tree, `Element`, routing, DI)
- `flow-client`: client-side TypeScript/JavaScript engine
- `flow-data`: data binding and validation
- `flow-html-components`: basic HTML component wrappers
- `flow-plugins`: Maven and Gradle build plugins
- `flow-devloop-daemon`: daemon for the `vaadin-dev` dev loop
- `flow-tests/`: integration test suite
- `vaadin-spring`: Spring Framework integration

See `guidelines/repository.md` for the full module map and
`guidelines/architecture.md` for how the pieces fit together.

## Guidelines & Conventions

Always read `CONVENTIONS.md` in full when **authoring** or **reviewing** code — it is the canonical list of checkable conventions.

Design and implementation guidelines live in `guidelines/`. Read the chapters mapped in `guidelines/overview.md` selectively for the topics your work touches.

## Development Commands

### Building and Testing

```bash
# Build entire project
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Note: To run tests, omit -DskipTests entirely (not -DskipTests=false)

# Build specific module
mvn clean install -pl flow-server -am

# Run tests for specific module
mvn test -pl flow-server

# Run specific test class
mvn test -Dtest=JacksonCodecTest

# Run specific test method
mvn test -Dtest=JacksonCodecTest#testComplexTypeSerialization

# Run tests matching pattern
mvn test -Dtest="*Codec*Test"

# Run integration tests (automatically starts and stops server)
mvn verify -pl flow-tests/test-root-context

# Run single integration test
mvn verify -pl flow-tests/test-root-context -Dit.test=ExecJavaScriptIT
```

### Code Quality

```bash
# Format code, must be run before every commit
mvn spotless:apply

# Check code formatting
mvn spotless:check

# Run checkstyle validation
mvn checkstyle:check
```

### Frontend Development

```bash
# Frontend assets are managed by Maven plugins, and Vite dev mode is started
# automatically during development. Manual frontend build (rare):
cd flow-client && npm install && npm run build
```
