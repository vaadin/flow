# Copilot Instructions for Vaadin Flow

This file provides guidance to GitHub Copilot when working with code in this repository.

## Repository Overview

Vaadin Flow is the Java framework of Vaadin Platform for building modern web applications. This is a large, multi-module Maven project that combines server-side Java components with modern frontend tooling (Vite, TypeScript, React support).

**Technology Stack:**
- Java 21+ (required for development)
- Jakarta EE 11 (not Java EE)
- Spring Boot 4 integration
- Maven build system
- Vite for frontend development
- TypeScript, React, and Lit templates

## Project Structure

This is a multi-module Maven project with the following key modules:

- `flow-server`: Core server-side framework with component system, state management, and routing
- `flow-client`: Client-side TypeScript/JavaScript code
- `flow-data`: Data binding and validation
- `flow-router`: Navigation and routing
- `flow-html-components`: Basic HTML component wrappers
- `flow-tests`: Extensive integration test suite
- `vaadin-spring`: Spring Framework integration
- `flow-push`: WebSocket-based push communication
- `flow-react`: React integration
- `flow-lit-template`: Lit template support

## Build and Test Commands

### Building
```bash
# Build entire project (use this for initial setup)
mvn clean install

# Build without tests (faster for quick iterations)
mvn clean install -DskipTests

# Build specific module
cd flow-server && mvn clean install
```

### Testing
```bash
# Run tests for specific module
cd flow-server && mvn test

# Run specific test class
mvn test -Dtest=JacksonCodecTest

# Run specific test method
mvn test -Dtest=JacksonCodecTest#testComplexTypeSerialization

# Run tests matching pattern
mvn test -Dtest="*Codec*Test"

# Run integration tests (automatically starts and stops server)
cd flow-tests/test-root-context && mvn verify

# Run single integration test
mvn verify -Dit.test=ExecJavaScriptIT
```

**Important:** To run tests, omit `-DskipTests` entirely (not `-DskipTests=false`)

### Code Quality
```bash
# Format code (always run before committing)
mvn spotless:apply

# Check code formatting
mvn spotless:check

# Run checkstyle validation
mvn checkstyle:check
```

## Coding Conventions

### General Guidelines
- Use triple quotes (`"""`) for multi-line string blocks in Java text blocks
- When tests fail or code doesn't compile: **Always analyze why first, do not start rewriting code**
- Names and comments should describe how the code works and why, not what has changed from previous versions
- Commit messages capture change information, not the code itself
- **Always create proper tests for what should work first**. If tests expose problems, fix the implementation after creating tests

### Testing Philosophy
- Verify actual behavior rather than just "not null" checks
- Test JSON structure and content for serialization
- Add comprehensive edge case coverage
- Integration tests should use TestBench for browser automation
- When an integration test fails, understand what's happening in the UI before making changes

### Git Conventions
- When creating a commit that will resolve an issue in the same repository, add "Fixes #issuenumber" to the commit message
- Always disable pagers in git commands (e.g., `git --no-pager status`)

## Key Architecture Components

### Core Server Framework (`flow-server/`)
- Component system with server-side state management (`StateNode`, `StateTree`)
- DOM abstraction layer (`Element`, `Node`) that syncs with client-side
- JavaScript execution bridge (`JacksonCodec`, `JsonCodec`) for seamless client-server communication
- Routing system (`Router`, `RouteConfiguration`) with navigation lifecycle
- Dependency injection and instantiation (`Instantiator`, `Lookup`)
- Frontend asset management and bundling

### Client-Server Communication
- Uses Jackson for JSON serialization/deserialization between Java objects and JavaScript
- `executeJs()` methods allow calling JavaScript from server with automatic parameter serialization
- **When sending data to executeJs, always pass it as parameters using $0, $1, etc. and never concatenate strings**
- Return values from JavaScript can be automatically deserialized into Java beans
- WebSocket-based push communication (`PushConnection`, `AtmospherePushConnection`)

### State Management
Flow uses a tree-based state management system:
- `StateNode`: Represents component state on server
- `StateTree`: Manages entire application state tree
- `NodeFeature`: Different aspects of node state (properties, children, etc.)
- Changes are automatically synchronized to client

### Component Development
Components extend `Component` and use:
- `Element`: Low-level DOM manipulation
- Property synchronization via `@Synchronize`
- Event handling with `@DomEvent`
- Client-side callbacks with `@ClientCallable`

### JavaScript Execution and JSON Codec
The `JacksonCodec` and `JsonCodec` classes handle serialization between Java and JavaScript:
- Parameters: Java objects → JSON → JavaScript variables (`$0`, `$1`, etc.)
- Return values: JavaScript objects → JSON → Java beans
- Special handling for `Element` instances (sent as DOM references or null)
- Support for arbitrary objects via Jackson serialization
- Use Jackson-compatible types for seamless serialization

## Testing Structure

**Unit Tests**: Located in `src/test/java/` in each module
- Use JUnit 4/5
- Heavy use of Mockito for mocking
- Focus on individual class behavior

**Integration Tests**: Located in `flow-tests/`
- Use TestBench for browser automation
- Test full client-server interaction
- Require running application server

## Frontend Development

```bash
# Frontend assets are managed by Maven plugins
# Vite dev mode is automatically started for development
# Manual frontend build (rare, usually automatic):
cd flow-client && npm install && npm run build
```

**Frontend Build System:**
- Vite-based development mode with hot reload
- Production bundling with webpack plugins
- TypeScript support with generated type definitions
- React and Lit template support
- Theme system with CSS custom properties

## Important Patterns and Gotchas

### Architecture Changes
This is a complex, interconnected system:
- Changes to core classes like `StateNode` or `Element` have wide impact
- Frontend changes require corresponding server-side updates
- Always run relevant test suites after modifications

### Common Pitfalls to Avoid
- Don't concatenate strings when passing data to `executeJs()` - use parameters
- Element parameters in `executeJs()` become DOM references or null
- Return values can be deserialized to Java beans automatically
- Always verify JSON structure and content in tests

## Development Environment

- Hot reload available in development mode
- Extensive CI/CD pipeline with multiple test configurations
- Requires Java 21+ for development
- Uses Jakarta EE (not Java EE)
- Spring Boot 4 integration available

## Common Task Types

### Best for Copilot
- Bug fixes in specific modules
- Test coverage improvements
- Documentation updates
- Refactoring within established patterns
- Adding new component features following existing patterns

### Requires Extra Caution
- Changes to core state management classes
- Modifications to the JavaScript bridge
- Updates to the routing system
- Frontend build system changes

## Additional Resources

- [Vaadin Flow Documentation](https://vaadin.com/docs/latest/flow)
- [Contributing Guide](https://vaadin.com/docs/latest/contributing/pr)
- [Forum](https://vaadin.com/forum/c/flow/8)
- [Discord](https://discord.com/channels/732335336448852018/1009201939092865084)
