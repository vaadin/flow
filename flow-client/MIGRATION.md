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

## Stateful classes (instances, not just static utilities)

For Java classes that callers instantiate with `new` and that hold internal
state, the bridge pattern extends in two ways. Worked examples:
`ExistingElementMap` and `flow.model.UpdatableModelProperties`.

1. **TS class with the real state and logic** under
   `src/main/frontend/internal/client/<pkg>/<Name>.ts`.
2. **`Native<Name>` JsType class** declares the constructor and instance
   methods (no static methods), e.g.

   ```java
   @JsType(isNative = true, namespace = "Vaadin.Flow.internal.client",
           name = "ExistingElementMap")
   final class NativeExistingElementMap {
       NativeExistingElementMap();
       native Element getElement(int id);
       // ...
   }
   ```

3. **Java public class becomes a thin facade** that picks between a
   GWT-side `Native<Name>` instance and a JVM-side fallback (typically
   `HashMap`/`ArrayList`/`HashSet`) so existing JUnit tests that
   `new` the class keep working unchanged:

   ```java
   public class ExistingElementMap {
       private final NativeExistingElementMap delegate;
       private final Map<Element, Integer> jvmElementToId;
       // ...
       public ExistingElementMap() {
           if (GWT.isScript()) {
               delegate = new NativeExistingElementMap();
               jvmElementToId = null;
               // ...
           } else {
               delegate = null;
               jvmElementToId = new HashMap<>();
               // ...
           }
       }
       public Element getElement(int id) {
           if (delegate != null) return delegate.getElement(id);
           // JVM fallback
       }
   }
   ```

4. **GwtTest stub** declares a constructor function rather than an object
   literal so `new client.ExistingElementMap()` works:

   ```java
   client.ExistingElementMap = function() {
       // …per-instance closure state…
       this.getElement = function(id) { /* ... */ };
   };
   ```

The cost of this pattern is duplicating the implementation on the JVM side.
For trivial classes that's a few lines; for richer ones it's tedious but
mechanical. Once a class is in TS, the JVM fallback can also be removed in
the tear-down phase together with all the JUnit tests.

## Passing Java callbacks to TS

When a JSNI body invokes a Java method by reference (`runnable.@Runnable::run()()`
or similar), the migrated TS version receives a plain JS function. The Java
shim wraps the Java callable into a `@JsFunction` interface declared on the
sibling `Native<Name>` class. Established shapes:

| Java type            | `@JsFunction` interface                    | First used in          |
| -------------------- | ------------------------------------------ | ---------------------- |
| `Runnable`           | `JsRunnable` — `void run()`                | `NativeLitUtils`       |
| `Supplier<Object>`   | `JsSupplier` — `Object get()`              | `NativeResourceLoader` |
| `Consumer<String>`   | `JsStringConsumer` — `void accept(String)` | `NativeBootstrapper`   |
| `Consumer<JsonArray>`| `JsArgsConsumer` — `void accept(JsonArray)`| `NativeClientJsonCodec`|

Reuse the existing interface where possible (`JsRunnable` lives in
`NativeLitUtils` and is imported by other native classes). Declare new
shapes alongside the consuming native class.

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

- Keep each PR under ~500 LOC of TS and ideally one Java class.
- Migrate from leaves to roots: pure utilities first, then reactive / DOM /
  node features, then state tree, then binding, then registry / application,
  then communication, finally bootstrap. The collection package
  (`flow.collection.*`) is already `@JsType` native and disappears in the
  final tear-down — do **not** start there.

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
