# Clipboard browserless-testing seam — flow-side spec

Design for the **flow-side SPI seam** that issue
[#64](https://github.com/vaadin/browserless-test/issues/64) needs before the
`browserless-test` simulator can be built. Modeled on the Geolocation seam
(`GeolocationClient` / `GeolocationClientFactory`, flow PR #24259) that already
ships in `flow-server`.

> **Status:** the seam does **not** exist yet. This spec is the prerequisite
> flow PR. Once it lands in a `25.x-SNAPSHOT`, the browserless side (§4) follows
> the Geolocation pattern mechanically.

---

## 1. Why the original issue can't be implemented as written

Issue #64 is a **stale proposal**. Two facts block a literal implementation:

1. **No SPI seam exists.** `flow-server:25.2-SNAPSHOT` has the clipboard feature
   classes but **no `ClipboardClient`, no `ClipboardClientFactory`**, and
   `UIInternals` carries **no clipboard hooks** (it has the geolocation ones:
   `getGeolocationClient` / `setGeolocationClient` /
   `getGeolocationAvailabilitySignal`). The `Clipboard` facade resolves nothing
   from `Lookup` — it talks straight to the DOM via triggers and listeners, so
   there is nothing for a test client to replace.

2. **The proposed API never shipped.** The issue describes `copyOnClick(...)`,
   `ClipboardCopy`, `registerCopyText`, `PasteState`, `ClipboardAvailability`.
   The API that actually shipped is different:

   ```java
   // com.vaadin.flow.component.clipboard.Clipboard  (25.2-SNAPSHOT)
   static <T extends Component & ClickNotifier<?>> ClipboardBinding onClick(T trigger);
   static Registration onPaste(Component target, SerializableConsumer<PasteEvent> listener);
   static Registration onPaste(Component target, PasteOptions options,
                               SerializableConsumer<PasteEvent> listener);
   static Registration onFilePaste(Component target, UploadHandler uploadHandler);
   ```

   `ClipboardBinding` (returned by `onClick`) is where copy/read live:

   ```java
   void writeText(String);                                  // + (value, onSuccess, onError) overloads
   <C extends Component & HasValue<?,String>> void writeText(C source);
   void writeHtml(String);
   void writeImage(Component);                               // + DownloadHandler overload
   void write(ClipboardContent);
   void read(SerializableConsumer<ClipboardPayload>, SerializableConsumer<PromiseAction.Error>);
   void readText(SerializableConsumer<String>, SerializableConsumer<PromiseAction.Error>);
   void readHtml(SerializableConsumer<String>, SerializableConsumer<PromiseAction.Error>);
   ```

So this spec **re-derives the seam against the real API**, keeping the
Geolocation resolution mechanics 1:1.

### Dropped from the original issue

- **Affordance #3 "query/override availability."** There is no
  `ClipboardAvailability` type and no clipboard availability signal in
  `UIInternals`. Clipboard isn't a permissioned async sensor like geolocation;
  its reads/writes are click-gated `Promise`s whose rejection (e.g. denied
  permission, insecure context) surfaces through the existing `onError`
  callback. **No availability API is added.** If a future PRD wants a coarse
  "is the Clipboard API present" hint, it should be a separate proposal.

---

## 2. The real wire behavior the port must abstract

Lifted from the current `Clipboard` / `ClipboardBinding` implementation, so the
port covers every production path with no behavior change:

| Facade entry | Today's wire mechanism | What the port must expose |
|---|---|---|
| `onPaste(target, [opts], listener)` | `element.addEventListener("paste", …)` with event data `event.clipboardData?.getData('text/plain')` / `'text/html'`, `mapEventTargetElement()`, and (when `opts` excludes input fields) a `composedPath()` filter | register a paste listener keyed by element + options; deliver a `PasteEvent` |
| `onFilePaste(target, uploadHandler)` | DOM paste listener + file upload over HTTP carrying `Clipboard.PASTE_ID_HEADER` / `PASTE_FILE_COUNT_HEADER`; bytes flow through the `UploadHandler` | register the upload handler keyed by element; feed it synthetic files |
| `ClipboardBinding.writeText/writeHtml/writeImage/write` | `ClickTrigger` + `WriteToClipboardAction` (a `PromiseAction<String>`) bound via `trigger.bind(action)`; success/error delivered through a `ReturnChannelRegistration` | bind a write action to a trigger element; carry the value source + `onSuccess`/`onError`; allow the test to resolve the promise |
| `ClipboardBinding.read/readText/readHtml` | `ClickTrigger` + `ReadFromClipboardAction` (a `PromiseAction<ClipboardPayload>`) | bind a read action to a trigger element; carry `onSuccess`/`onError`; allow the test to resolve with a payload/error |

`PromiseAction.Error(String name, String message)` lives in
`com.vaadin.flow.component.trigger.internal` but is already part of the public
clipboard surface (it appears in `ClipboardBinding`'s signatures), so the port
may reference it.

---

## 3. Flow changes

Mirrors the Geolocation split: **PR-A** extracts the port + default impl +
facade refactor; **PR-B** adds the `Lookup` factory. They can ship as one PR —
Geolocation only split because of late classloader fallout, which we avoid by
designing the seam public from the start.

### 3.1 `ClipboardClient` — the port (`com.vaadin.flow.component.clipboard`)

Public interface, the analogue of `GeolocationClient`. One instance per UI.

```java
public interface ClipboardClient extends Serializable {

    // --- write / read, bound to a click trigger on `trigger` ---
    WriteHandle registerWrite(Component trigger, ClipboardWrite write,
            @Nullable SerializableConsumer<String> onSuccess,
            @Nullable SerializableConsumer<PromiseAction.Error> onError);

    ReadHandle registerRead(Component trigger, ClipboardReadKind kind,
            SerializableConsumer<?> onSuccess,                 // String or ClipboardPayload
            SerializableConsumer<PromiseAction.Error> onError);

    // --- paste ---
    Registration registerPaste(Component target, PasteOptions options,
            SerializableConsumer<PasteEvent> listener);

    Registration registerFilePaste(Component target, UploadHandler uploadHandler);

    // --- lifecycle (parity with GeolocationClient.close()) ---
    void close();

    /** Inspect/resolve handle for a bound write action. */
    interface WriteHandle extends Registration {
        Element trigger();
        ClipboardWrite write();              // text/html/image/payload descriptor
        boolean hasSuccessCallback();
        boolean hasErrorCallback();
    }

    /** Inspect/resolve handle for a bound read action. */
    interface ReadHandle extends Registration {
        Element trigger();
        ClipboardReadKind kind();            // READ, READ_TEXT, READ_HTML
    }
}
```

`ClipboardWrite` is a small public descriptor capturing what
`ClipboardBinding` builds today (a literal string, a bound `HasValue` source,
an image component/`DownloadHandler`, or a composed `ClipboardContent`). It
replaces the per-call `WriteToClipboardAction` construction so the value is
inspectable without a DOM. `ClipboardReadKind` is a 3-value enum.

> **Note on `ClipboardBinding`:** it becomes a thin facade over the client,
> exactly like `Clipboard` itself. Each `writeText(...)` / `read(...)` call
> delegates to `client.registerWrite(...)` / `client.registerRead(...)` instead
> of constructing a `WriteToClipboardAction` and calling `trigger.bind(...)`.
> The `WriteToClipboardAction` / `ReadFromClipboardAction` / return-channel code
> moves into `BrowserClipboardClient` (§3.2).

### 3.2 `BrowserClipboardClient` — production default (package-private)

Lifts the current trigger/`PromiseAction`/`ReturnChannelRegistration` and
`addEventListener("paste", …)` / `UploadHandler` plumbing out of `Clipboard` and
`ClipboardBinding` and puts it behind `ClipboardClient`. **Production wire
behavior is byte-for-byte unchanged**; `Clipboard` and `ClipboardBinding` end up
as thin facades. This is the analogue of `BrowserGeolocationClient`.

### 3.3 `ClipboardClientFactory` — the SPI (PR-B)

```java
public interface ClipboardClientFactory extends Serializable {
    ClipboardClient create(UI ui);
}
```

### 3.4 `Clipboard` / `ClipboardBinding` facade refactor

Resolution mirrors `Geolocation.resolveClient` / `lookupFactory` exactly:

```java
static ClipboardClient client(UI ui) {
    ClipboardClient existing = ui.getInternals().getClipboardClient();
    if (existing != null) {
        return existing;
    }
    ClipboardClientFactory factory = lookupFactory(ui);   // VaadinService → Lookup
    ClipboardClient client = (factory != null)
            ? factory.create(ui)
            : new BrowserClipboardClient(ui);
    ui.getInternals().setClipboardClient(client);
    return client;
}

private static ClipboardClientFactory lookupFactory(UI ui) {
    VaadinService service = ui.getSession().getService();
    Lookup lookup = service.getContext().getAttribute(Lookup.class);
    return lookup == null ? null : lookup.lookup(ClipboardClientFactory.class);
}
```

Resolution happens **at first use, not at UI construction**, so a factory
registered after UI init still takes effect (same constraint Geolocation has).
Public facade signatures stay byte-for-byte stable.

### 3.5 `UIInternals` additions

Add the clipboard equivalents of the existing geolocation fields (the cache, not
a signal — there is no availability signal):

```java
private ClipboardClient clipboardClient;
public ClipboardClient getClipboardClient();
public void setClipboardClient(ClipboardClient client);
```

### 3.6 In-JAR tests

`ClipboardClientSeamTest` (parallel to `GeolocationClientSeamTest`):

- `Clipboard` / `ClipboardBinding` resolve the client via `Lookup`.
- A registered `ClipboardClientFactory` overrides the default.
- Without a factory the fallback is `BrowserClipboardClient`.
- Existing `ClipboardTest` keeps its wire-protocol assertions to pin production
  behavior unchanged.

---

## 4. Downstream: `browserless-test` (after the seam lands)

Drops into `shared/src/main/java/com/vaadin/flow/component/clipboard/`, 1:1 with
the existing `…/geolocation/` package.

- **`BrowserlessClipboardClient`** (package-private) — in-memory
  `ClipboardClient`, no DOM. Registries: `List<WriteHandle>`, `List<ReadHandle>`
  keyed by trigger element; `Map<Element, PasteRegistration>`;
  `Map<Element, UploadHandler>`. Files arrive as `byte[]` and are pushed through
  the `UploadHandler` with a synthetic `UploadEvent` (see open question).
- **`BrowserlessClipboardClientFactory`** — publishes a `ClipboardSimulator` on
  the UI via `ComponentUtil.setData(...)`, exactly like
  `BrowserlessGeolocationClientFactory`.
- **`MockVaadinHelper.BrowserlessLookupInitializer.additionalServices`** gains
  one line next to the geolocation entry:

  ```kotlin
  ClipboardClientFactory::class to BrowserlessClipboardClientFactory::class
  ```

- **`ClipboardSimulator`** (public test API), obtained via `current()` /
  `forUI(UI)`:

  ```java
  // copy / write (PRD affordance #1)
  List<CopyHandleInspection> activeWriteHandles();
  void simulateClick(Component trigger);          // fires the bound write/read promise
  void simulateWriteSuccess(Component trigger);
  void simulateWriteError(Component trigger, String name, String message);

  // read
  void simulateReadResult(Component trigger, String text, @Nullable String html);
  void simulateReadError(Component trigger, String name, String message);

  // paste (PRD affordance #2)
  List<PasteSessionInspection> activePasteSessions();
  PasteBuilder pasteInto(Component target);       // .text(..).html(..).file(name,mime,bytes).simulate()
  ```

  `PasteBuilder.simulate()` runs the production dispatch order: deliver the
  `PasteEvent` to `onPaste` listeners, then run each file through the
  `onFilePaste` `UploadHandler`, emitting the `PasteFile` records the real
  handler would see (`PasteFileHandler.perFile` / `.batch`).

- **Tests:** `ClipboardSimulatorTest` (direct controller behavior) +
  `ClipboardFacadeIntegrationTest` (through the real `Clipboard` /
  `ClipboardBinding` facade), parallel to the geolocation test pair.

---

## 5. Open questions for the flow author

1. **`UploadEvent` synthesis.** Feeding `UploadHandler.handleUploadRequest`
   outside an HTTP request needs a prototype. Cleanest fallback is a
   package-private hook on `InMemoryUploadHandler` / `TemporaryFileUploadHandler`
   that bypasses request plumbing — same shape as keeping
   `Geolocation.setClient` package-private. Worth a spike before locking
   `PasteBuilder.file(...)`.

2. **`ClipboardWrite` descriptor shape.** Does the port carry a live reference to
   a bound `HasValue` source (so `simulateClick` reads its current value, like
   `readSourceText` does today), or snapshot at registration time? Recommend
   **live** to match production. Decision belongs to PR-A.

3. **Per-trigger action stacking.** A single `onClick` binding can register
   multiple write/read actions over its lifetime. The simulator assumes a
   resolvable handle per trigger; confirm whether the client keeps a stack or
   the latest wins. Decision belongs to PR-A.

4. **`PromiseAction.Error` package.** It currently lives in
   `…trigger.internal`. It's already exposed through `ClipboardBinding`'s public
   signatures, so the port can reference it — but the flow team may prefer to
   promote it to a public clipboard type.

---

## 6. PR sequence

1. **flow PR-A** — `ClipboardClient` port, `BrowserClipboardClient` default,
   `Clipboard` + `ClipboardBinding` facade refactor, `UIInternals` cache. No
   behavior change.
2. **flow PR-B** — `ClipboardClientFactory` + `Lookup` resolution (mergeable
   into PR-A).
3. **browserless-test PR** (§4) — depends on a `25.x-SNAPSHOT` carrying PR-A+B.
</content>
</invoke>
