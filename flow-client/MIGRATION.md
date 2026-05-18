# Migrating flow-client from GWT-compiled Java to TypeScript

The flow-client module compiles ~21k LOC of Java to a single GWT bundle that
ships as `FlowClient.js`. This document captures the pattern for replacing
that Java code with TypeScript, one class at a time, so that the GWT bundle
and the new TS code can coexist throughout the migration and every step
remains independently mergeable.

## End state

When every class has been migrated:

- `src/main/java/com/vaadin/client/**` is empty (or contains only generators
  used at server-side build time).
- `src/test-gwt/**`, `src/test/java/com/vaadin/client/**`,
  `flow-client/pom.xml`'s GWT plugin and the `ClientEngineLinker`/`scripts/client.js`
  GWT-bundle wrapper are all deleted.
- `Flow.ts` imports the migrated modules directly; the
  `window.Vaadin.Flow.internal.*` bridge namespace goes away.

## Bridge mechanic

1. **Namespace.** Migrated TS implementations are published at
   `window.Vaadin.Flow.internal.<pkg>.<name>`, where `<pkg>` mirrors the Java
   package below `com.vaadin.client` (`client`, `client.flow.reactive`, ...).
   `internal` keeps the namespace clearly off-limits to applications.
2. **TS side.** Every migrated module is added to `internal/bridge.ts`, which
   exports `installGwtBridge()`. That function calls `registerGwtBridge(pkg,
   name, impl)` for each entry, walking the namespace and writing the
   implementation. The bridge is installed:
   - once at module evaluation (production path), and
   - again from `Flow.ts` before each call to `clientMod.init()`, so tests
     that wipe `window.Vaadin` between runs still see the bridge.
3. **GWT side.** A small native JsType class declares the JS shape:

   ```java
   @JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "Console")
   final class NativeConsole {
       static native void debug(Object message);
       // ...
   }
   ```

   Callers don't reach this class directly — see the next point.

## Java shim with JVM fallback

JVM unit tests under `src/test/java/**` exercise Java production code on the
JVM (not in GWT). Pure `@JsType(isNative=true)` calls throw on the JVM, so the
public Java class stays in place as a thin shim that picks between the bridge
(under GWT) and a JVM behaviour (under tests):

```java
public static void debug(Object message) {
    if (GWT.isScript()) {
        NativeConsole.debug(message);
    } else {
        System.out.println(message);
    }
}
```

This is the standard shape for a migrated class. The migrated TS module owns
all interesting behaviour; the Java shim only routes.

## GwtTest stub

`Gwt*Test` classes don't go through `Flow.ts`, so the bridge isn't published
when they run. `ClientEngineTestBase.gwtSetUp()` installs pass-through stubs
of the `Vaadin.Flow.internal.*` namespace via JSNI — see
`installMigratedBridgeStubs()` in that file. Each new migration adds an entry
to that stub.

## Per-PR checklist

For each Java class being migrated:

1. **Add the TS module** under `src/main/frontend/internal/<pkg>/<Name>.ts`.
   Export the implementation; **do not** call `registerGwtBridge` from the
   module itself — registration lives only in `bridge.ts`.
2. **Add the entry to `internal/bridge.ts`** (import + one
   `registerGwtBridge(...)` line in `installGwtBridge()`).
3. **Replace the Java class body** with a `GWT.isScript()` shim that calls
   into a sibling `Native<Name>` JsType class. Keep the JVM fallback so JUnit
   tests under `src/test/java/**` continue to compile and pass.
4. **Add the `Native<Name>` class** with
   `@JsType(isNative = true, namespace = "Vaadin.Flow.internal.<pkg>", name = "<Name>")`
   and `native` declarations for each method.
5. **Extend the GwtTest stub** in `ClientEngineTestBase.installMigratedBridgeStubs()`
   with a pass-through JS object for the new entry.
6. **Port the Gwt test** (if one exists) to a `*Tests.ts` file under
   `src/test/frontend/` using `web-test-runner` + mocha + sinon + `@open-wc/testing`.
   File name must end in `Tests.ts` (matches the runner config glob).
   Delete the old `Gwt<Name>Test.java` in the same PR.
7. **Verify the build.** Run, in order:
   - `npx tsc --noEmit && npx eslint src/main/frontend src/test/frontend`
   - `npm test` (all TS tests, in Chromium)
   - `mvn -pl . test -Dgwt.test.pattern=none` (JVM JUnit only)
   - `mvn install` (full build, GWT compile + every test)
   - `mvn spotless:check`

   Every step must be green. Each PR leaves the world working.

## PR size and order

- Keep each PR under ~500 LOC of TS and ideally one package.
- Migrate from leaves to roots: pure utilities first, then reactive / DOM /
  node features, then state tree, then binding, then registry / application,
  then communication, finally bootstrap. The collection package
  (`flow.collection.*`) is already `@JsType` native and disappears in the
  final tear-down — do **not** start there.

## Style

Straight translation first, idiomatic refactor later:

- TS file names mirror Java class names.
- Method names mirror Java method names (camelCase carries over).
- Use TS native types (`Map`, `Set`, `Array`, `WeakMap`) directly. The Java
  `JsMap`/`JsArray`/etc. continue to map to these via their existing global
  `@JsType` annotation — leave them alone until tear-down.
- Prefer `const` object literals with methods over classes with only static
  members (the `@typescript-eslint/no-extraneous-class` rule rejects the
  latter). The bridge lookup works identically.

## Worked example: `Console`

See:

- `src/main/frontend/internal/client/Console.ts` (TS implementation)
- `src/main/frontend/internal/bridge.ts` (registration)
- `src/main/frontend/Flow.ts` (eager `installGwtBridge()` call)
- `src/main/java/com/vaadin/client/Console.java` (Java shim)
- `src/main/java/com/vaadin/client/NativeConsole.java` (`@JsType` binding)
- `src/test-gwt/java/com/vaadin/client/ClientEngineTestBase.java`
  (`installMigratedBridgeStubs`)
- `src/test/frontend/ConsoleTests.ts` (mocha tests)
