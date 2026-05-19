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

## Recommended pattern: pure `@JsType` native shim

After a migration, the Java public class becomes a *pure* `@JsType(isNative
= true)` declaration that points at the TS module. No body, no fields, no
JVM branch:

```java
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client",
        name = "ExistingElementMap")
public class ExistingElementMap {
    public ExistingElementMap();
    public native Element getElement(int id);
    public native Integer getId(Element element);
    public native void remove(int id);
    public native void add(int id, Element element);
}
```

Java callers keep calling `new ExistingElementMap()` and `map.getElement(id)`
as before; at runtime those resolve to the TS class published at
`window.Vaadin.Flow.internal.client.ExistingElementMap`. There is no
`Native<Name>` sibling class — the public Java class *is* the JsType
binding.

Because the Java class has no JVM-side body, **any JUnit test that
instantiated it must already be deleted in the same commit as the
migration**. See [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md): per-tier ports
of test coverage to mocha happen alongside the class migration.

For static-utility classes (no constructor, no instance state) the same
shape applies, just with `static native` methods:

```java
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client",
        name = "Console")
public final class Console {
    public static native void debug(Object message);
    public static native void log(Object message);
    // ...
}
```

## Transitional pattern: `GWT.isScript()` shim with JVM fallback

The 24 in-flight commits on this branch use a slightly different shape:
the public Java class stays in place as a thin shim that picks between
the TS bridge under GWT and a JVM behaviour under tests:

```java
public static void debug(Object message) {
    if (GWT.isScript()) {
        NativeConsole.debug(message);  // forwards to TS
    } else {
        System.out.println(message);   // JVM fallback for JUnit
    }
}
```

And for stateful classes, a paired `Native<Name>` JsType class plus a
public Java facade that keeps a `HashMap`/`ArrayList` JVM-side copy of
the state.

This pattern was used because at the time the migration started the
project couldn't yet drop JUnit tests one-by-one. **It is now considered
transitional.** The cost is duplicating the implementation on the JVM
side; for trivial classes that's a few lines, for richer ones it's
tedious make-work. Per `MIGRATION_PLAN.md` Tier 7 (and matching tiers
for the communication / bootstrap classes), these in-flight shims get
collapsed to the recommended pattern at the same time their JUnit tests
are dropped.

**Do not add new migrations using the transitional pattern.** Use the
pure `@JsType` native form and port any JUnit coverage to mocha in the
same commit.

### When the recommended pattern needs a `Native<Name>` sibling

The pure form folds the JsType binding into the public Java class. That
works when every public method maps one-to-one to a TS method. If the
Java shim has to do something non-trivial *before* dispatching — most
commonly, bundling Java method references into a callback POJO or
flattening an overloaded API into the JS-friendly form — keep a
`Native<Name>` sibling for the native binding and let the public Java
class hold the small adapter logic. `NativeApplicationConnection`,
`NativeAtmospherePushConnection`, `NativeExecuteJavaScriptProcessor` and
`NativeSimpleElementBindingStrategy` are the canonical examples.

## GwtTest stub for stateful classes

For stateful classes whose `new` form is exercised by a GwtTest that
hasn't yet been ported to mocha, the stub in
`ClientEngineTestBase.installMigratedBridgeStubs()` declares a constructor
function rather than an object literal:

```java
client.ExistingElementMap = function() {
    // …per-instance closure state…
    this.getElement = function(id) { /* ... */ };
};
```

Per the plan, both the GwtTest and the stub disappear once the test
coverage moves to mocha.

## Passing Java callbacks to TS

When a JSNI body invokes a Java method by reference (`runnable.@Runnable::run()()`
or similar), the migrated TS version receives a plain JS function. The Java
shim wraps the Java callable into a `@JsFunction` interface declared on the
sibling `Native<Name>` class. Established shapes:

| Java type            | `@JsFunction` interface                    | First used in          |
| -------------------- | ------------------------------------------ | ---------------------- |
| `Runnable`           | `com.vaadin.client.JsRunnable` — `void run()` | shared (top-level) |
| `Supplier<Object>`   | `JsSupplier` — `Object get()`              | `NativeResourceLoader` |
| `Consumer<String>`   | `JsStringConsumer` — `void accept(String)` | `NativeBootstrapper`   |
| `Consumer<JsonArray>`| `JsArgsConsumer` — `void accept(JsonArray)`| `NativeClientJsonCodec`|

Plus per-class function shapes for richer signatures (used by
`NativeApplicationConnection`, `NativeAtmospherePushConnection`,
`NativeExecuteJavaScriptProcessor`, `NativeSimpleElementBindingStrategy`).

Reuse `JsRunnable` from the top-level `com.vaadin.client` package
whenever a `() => void` is needed. Declare new shapes alongside the
consuming native class.

When the original JSNI passed *two* callbacks via a Java listener interface
(for example `ResourceLoadListener` with `onLoad` / `onError`), don't ship
the listener through the bridge. Have the Java shim build two `JsRunnable`
closures that capture the event; the TS side receives just the two
`() => void` callbacks. `ResourceLoader.addOnloadHandler` is the worked
example.

## GwtTest stub

`Gwt*Test` classes don't go through `Flow.ts`, so the bridge isn't published
when they run. `ClientEngineTestBase.gwtSetUp()` installs pass-through stubs
of the `Vaadin.Flow.internal.*` namespace via JSNI — see
`installMigratedBridgeStubs()` in that file. Each migration adds an entry
to that stub *if and only if* a GwtTest that hasn't been ported yet still
references the migrated class.

Per `MIGRATION_PLAN.md`, the goal is to port the GwtTest to mocha in the
same commit as the migration, drop the GwtTest, and remove the stub entry
— so the stub map shrinks over the course of the migration.

## Per-PR checklist

For each Java class being migrated:

1. **Port the relevant test coverage to mocha first.** Identify the
   methods of any `Gwt<Name>Test` and `<Name>Test.java` that prove
   behaviour worth preserving (per the `MIGRATION_PLAN.md` P1 inventory).
   Write equivalent mocha tests under `src/test/frontend/<Name>Tests.ts`.
   These will run against the new TS module once it exists; keep them
   ready in this commit but expect them to fail until step 3 lands.
2. **Add the TS module** under `src/main/frontend/internal/<pkg>/<Name>.ts`.
   Export the implementation. The mocha tests from step 1 should now pass.
3. **Replace the Java class body** with a pure `@JsType(isNative = true)`
   declaration pointing at the TS module — no body, no fields, no JVM
   branch. (Recommended pattern, above.) When `Native<Name>` adapter
   classes are needed for callback bundling, see "When the recommended
   pattern needs a `Native<Name>` sibling" above.
4. **Add the entry to `internal/bridge.ts`** (import + one
   `registerGwtBridge(...)` line in `installGwtBridge()`).
5. **Delete `<Name>Test.java`** if one exists — its coverage now lives
   in mocha.
6. **Delete `Gwt<Name>Test.java`** if it covered only this class. If it
   covers multiple classes that haven't migrated yet, leave it in place
   and add a JSNI pass-through stub for `<Name>` in
   `ClientEngineTestBase.installMigratedBridgeStubs()` so the GwtTest
   keeps working until its other targets migrate too.
7. **Verify the build.** Run, in order:
   - `npx tsc --noEmit && npx eslint src/main/frontend src/test/frontend`
   - `npm test` (all TS tests, in Chromium)
   - `mvn -pl . test -Dgwt.test.pattern=none` (JVM JUnit only — should
     get smaller as classes migrate)
   - `mvn install` (full build, GWT compile + every test)
   - `mvn spotless:check`

   Every step must be green. Each PR leaves the world working.

## PR size and order

- Keep each PR under ~500 LOC of TS and ideally one Java class.
- For the full ordering through to an empty `src/main/java`, see
  [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md). Short version: leaves to
  roots — reactive → nodefeature → DOM → state tree → binding →
  application kernel → communication → bootstrap → tear-down. The
  `flow.collection.*` package is already `@JsType` native and disappears
  in tear-down; do **not** start there.

### Status

**Done** — every JSNI body, plus two stateful classes:

- `client.{Console, LitUtils, ReactUtils, ConnectionIndicator, ElementUtil,
  WidgetUtil, PolymerUtils, BrowserInfo, ResourceLoader, SystemErrorHandler,
  ExecuteJavaScriptElementUtils, ApplicationConnection, Profiler}`
- `client.bootstrap.Bootstrapper`
- `client.communication.{AtmospherePushConnection, MessageHandler,
  MessageSender, XhrConnection}`
- `client.flow.ExecuteJavaScriptProcessor`
- `client.flow.binding.SimpleElementBindingStrategy`
- `client.flow.util.ClientJsonCodec`
- `client.ExistingElementMap` (stateful pattern)
- `client.flow.model.UpdatableModelProperties` (stateful pattern)

**Deleted** — no callers: `LocationParser`, `StorageUtil`.

**Remaining JSNI in `src/main/java`** — these are *not* migrable to a
separate TS module; they go away in the tear-down phase when their consumers
are translated to TS and the GWT compiler is dropped.

| Class | Why it stays |
|---|---|
| `ValueMap`, `bootstrap.ErrorMessage`, `bootstrap.JsoConfiguration` | `JavaScriptObject` subclass — methods *are* property accessors on a JS instance (`return this[key]`); there is no static "module" to publish |
| `Profiler.GwtStatsEvent` (inner JSO) | Same — JSO accessors on a JS instance |
| `AtmospherePushConnection.AtmosphereConfiguration` / `AtmosphereResponse` (inner JSOs) | Same |
| `ApplicationConnection.Styles` (inner JSO) | Same |
| `flow.binding.ServerEventObject` | JSO subclass whose methods sit on the JS instance |
| `flow.collection.{JsArray, JsMap, JsSet, JsWeakMap, JsCollections}` | Already `@JsType(isNative=true)` aliases of global `Array`/`Map`/`Set`/`WeakMap` plus `@JsOverlay` GWT conveniences that disappear when consumers use native JS types directly |
| `gwt/elemental/js/util/Xhr` | Vendored GWT helper (`com.vaadin.client.gwt.**`, excluded from spotless) |

**No JSNI; stays Java for now** — purely pure-Java logic that runs fine on
both GWT and JVM: all listener / event types, `StateNode`, `StateTree`,
`NodeMap`, `NodeList`, `MapProperty`, `Reactive`, `Computation`,
`DependencyLoader`, `Registry`, `DefaultRegistry`,
`ApplicationConfiguration`, `UILifecycle`, `ExistingElementMap`,
`flow.binding.{Binder, BindingStrategy, TextBindingStrategy,
ServerEventHandlerBinder}`, `flow.dom.{DomApi, DomApiImpl, DomElement,
DomNode}`, `flow.nodefeature.*` event/listener helpers, `Heartbeat`,
`PollConfigurator`, `Poller`, `RequestResponseTracker`,
`ReconnectConfiguration`, `DefaultConnectionStateHandler`,
`LoadingIndicatorConfigurator`, `LoadingIndicatorStateHandler`,
`ServerConnector`, `URIResolver`, `flow.ConstantPool`,
`flow.TreeChangeProcessor`, `flow.model.UpdatableModelProperties`,
`flow.util.{JsObject, NativeFunction}` (already `@JsType`-native).
These migrate in the final tear-down phase, by then their consumers are all
in TS so the migration is just `mv Foo.java Foo.ts` plus translation.

### Tear-down phase

Once the Java logic has been ported to TS (so nothing instantiates the JSO
subclasses, calls `JsArray.length()`, or extends `JavaScriptObject` anymore):

1. Drop `src/main/java`, `src/test`, `src/test-gwt`, the GWT plugin in
   `pom.xml`, `ClientEngineLinker`, `scripts/client.js`, the
   `installMigratedBridgeStubs` hook, and the `internal/bridge.ts` registry.
2. `Flow.ts` imports each migrated module directly instead of through
   `window.Vaadin.Flow.internal.*`.
3. Module becomes pure TS.

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
