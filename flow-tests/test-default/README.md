# test-default

Home for integration tests that need only the **default Flow configuration**:

- React router (default), dev mode, root context path
- Does not use Lumo or Aura to avoid dependencies on those
- Deployed with **Spring Boot** for ease of development
- All ITs run on **JUnit 6 (Jupiter)**

## Adding a test

1. Add a `@Route` view under `src/main/java/com/vaadin/flow/test/...` — Spring
   picks it up automatically, no servlet registration needed.
2. Add an IT under `src/test/java/...` extending
   [`AbstractDefaultIT`](src/test/java/com/vaadin/flow/test/AbstractDefaultIT.java)
   and annotate test methods with `@BrowserTest` (not JUnit's `@Test`). Use
   `@BeforeEach`/`@AfterEach` and `org.junit.jupiter.api.Assertions`.

## What does *not* belong here

Anything needing a non-default build/deploy: production build, custom
context/encoding, a non-default router engine, a custom theme, a custom
frontend directory/package manager, bundle-level feature flags, or plain-servlet
semantics (custom `@WebServlet` / `createServletService` overrides). Those keep
their own specialized modules.
