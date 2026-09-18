# Hotswap

How development-mode hotswap keeps generated frontend files in sync with the
Java classes they were generated from.

## The two halves of a generated file

A file such as `layouts.json` or `generated-flow-imports.js` has two producers,
and both have to write the same content:

- **At startup**, `NodeTasks` runs the `Task*` commands in `flow-build-tools`.
  Each writes its files under the frontend generated folder
  (`src/main/frontend/generated`).
- **While the application runs**, a `VaadinHotswapper` in `vaadin-dev-server`
  is notified that classes or resources changed. When a change affects what a
  generated file should contain, the hotswapper rewrites that one file.

The hotswapper's whole job is to make the file on disk match the new class
state. Getting it from disk into the browser is the dev server's job — the
generated folder is part of the Vite module graph, so Vite's watcher sees the
change and reloads the module.

## Call into the task, do not reimplement it

The content generation stays in the task. Expose the single step the hotswapper
needs as a `public static` method on the task, and have it delegate to the same
private method `execute()` uses, so there is exactly one copy of the logic.

`TaskGenerateReactFiles.writeLayouts(Options, Collection<Class<?>>)` is the
precedent: `execute()` and `writeLayouts` both go through the private
`writeLayoutsJson`. `RouteRegistryHotswapper.onClassesChange` calls it after
updating the route registry:

```java
Options options = new Options(
        vaadinService.getContext().getAttribute(Lookup.class), null,
        configuration.getProjectFolder())
        .withFrontendDirectory(configuration.getFrontendFolder());
TaskGenerateReactFiles.writeLayouts(options, appRegistry.getLayouts());
```

Two things to keep in mind when adding such an entry point:

- Keep it narrow — one generated file, not the task's whole `execute()`. A full
  `execute()` at runtime would rewrite files unrelated to the change and redo
  work (template reads, npm state, validation) that only makes sense at
  startup.
- The task API throws the checked `ExecutionFailedException`, which the
  `VaadinHotswapper` callbacks do not declare. Unwrap it into a runtime
  exception in the static method, as `writeLayouts` does, rather than making
  every caller handle it.

## Rebuild a minimal `Options`

A hotswapper has a `VaadinService`, not the build's `Options`. Build a minimal
one from the context and the deployment configuration, as above: `Lookup` from
`VaadinContext`, the project folder and the frontend folder from
`DeploymentConfiguration`.

Pass `null` for the `ClassFinder`. The hotswapper already knows which classes
matter — it has the changed classes from the event and the registries that were
just updated — so the task must not rescan the classpath to find out.

## Write through `writeIfChanged`

Generated files are written with
`AbstractFileGeneratorFallibleCommand.writeIfChanged`, which delegates to
`FileIOUtils.writeIfChanged`. It does two things that matter here:

- It compares the new content against what is on disk and returns without
  touching the file when they match, so an unnecessary rewrite does not trigger
  a frontend recompile.
- It writes atomically (temp file in the same directory, then move), so Vite's
  watcher never observes a truncated or momentarily missing file and then
  fails to resolve imports between generated files.

So never write a generated file with `Files.writeString` or a hand-rolled
writer — that loses both properties. And still gate the call on a cheap check
of whether the relevant state actually changed: `RouteRegistryHotswapper`
collects the registry's layouts before and after applying the class changes and
only calls `writeLayouts` when that set differs.

## Getting the browser to pick it up

Use the weakest mechanism that works:

1. **Nothing.** The rewritten file is in the Vite module graph and the dev
   server reloads it by itself. This is the `layouts.json` case —
   `RouteRegistryHotswapper` writes the file and never calls `triggerUpdate`.
2. **`HotswapEvent.triggerUpdate(UIUpdateStrategy)`** when the server-side UI
   also has to react. `REFRESH` re-renders the affected UIs and keeps state;
   `RELOAD` reloads the page. `RELOAD` has priority and cannot be downgraded
   back to `REFRESH` once requested.
3. **`HotswapEvent.sendHmrEvent(String, JsonNode)` or
   `HotswapEvent.updateClientResource(String, String)`** when client code needs
   a specific signal or content. `DefaultTranslationsHotswapper` sends a
   `translations-update` HMR event; `StyleSheetHotswapper` pushes bundled CSS
   for a URL.

## When rewriting a file cannot work

Some Java changes cannot be repaired by regenerating a file, because the
browser would still load a stale bundle chunk: `@JsModule`, `@JavaScript`,
`@CssImport`, `@NpmPackage` and `@Theme` end up in `generated-flow-imports.js`,
and the client reaches them through a chunk keyed by class name. Updating the
imports file alone leaves the chunk wrong, so the bundle has to be rebuilt.

`DevLoopRedefiner.frontendDependencies(Class)` fingerprints exactly these
annotations before and after a redefine, and the dev loop escalates to a
restart when the fingerprint moves. If a new annotation joins that group, add
it to the fingerprint instead of trying to hot-update the bundle.

## Testing

Test the hotswapper against a real temporary project folder rather than mocking
the writer: point the deployment configuration's project folder at a temp
directory, fire the hotswap event, and assert on the generated file. Cover both
directions — that the file is written when the relevant state changed, and that
it is left alone when it did not. See `RouteRegistryHotswapperTest`.
