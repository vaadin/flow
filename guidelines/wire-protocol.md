# Wire Protocol

What a server response actually contains, how the constant pool keeps it
small, and how an `executeJs` call gets from a Java method to a running
JavaScript function. Read this before adding anything new that travels
between the server and the client.

The mechanics here are internal and may change, but the constraints they
impose on new code are real — most of them only surface as a runtime
failure or a memory leak in a long-lived UI.

## The UIDL response

`UidlWriter.createUidl` builds one JSON object per server response. The
keys that matter for new code, in the order the client applies them:

| Key         | Contents                                                       |
| ----------- | -------------------------------------------------------------- |
| `constants` | New constant pool entries (see below), as `id` → value.         |
| `changes`   | State tree changes, each encoded by a `NodeChange`.              |
| `execute`   | Pending `executeJs` invocations, one array per invocation.       |

`MessageHandler` applies them in exactly that order: constants are
imported first (so a change can reference one), changes are applied next,
and the `execute` list runs last — behind a doubly nested post-flush
listener, so the scripts see the DOM that the same response's changes
produced, including any post-flush listener added while applying them.

**On the server the write order is the reverse of the read order, and it
has to be.** `encodeChanges` runs first because encoding a change is what
registers new constants; `dumpConstants` is called after it and clears the
new-key set; `dumpPendingJavaScriptInvocations` is called after that
because `beforeClientResponse` executions run during change encoding and
can queue more JavaScript. Anything new that contributes to the response
has to be slotted into that sequence rather than appended at the end.

## Constant pool

The constant pool deduplicates JSON values that would otherwise be
repeated in the response for many state nodes. The value is sent once
under a short id, and each node's change carries only the id.

- `ConstantPoolKey` wraps the `JsonNode`. Its id is the Base64 encoding
  of the first 64 bits of the SHA-256 digest of `json.toString()`, so
  **identity is by value**: two structurally equal values collapse into
  one entry, and the id is stable across UIs, sessions and server
  restarts.
- The id is computed lazily and cached. **Never mutate the JSON after
  building the key** — the id would then no longer describe the value,
  and the client would resolve the id to whatever was sent first.
- `ConstantPool` lives in `UIInternals`, one per UI. It keeps
  `knownValues` (every id ever sent to this client) and `newKeys` (the
  ids first seen since the last response). `getConstantId` registers,
  `dumpConstants` emits the new ones and clears the set.
- A value only becomes a constant by being stored in a state node as a
  `ConstantPoolKey`. The `NodeChange` encoders call
  `JacksonCodec.encodeWithConstantPool`, which replaces the key with its
  id string; every other value is encoded inline.

### Constraints this puts on new code

- **Nothing is ever evicted, on either side.** The pool grows for the
  lifetime of the UI. A constant pool key is right for a value with few
  distinct shapes repeated over many nodes, and wrong for anything that
  varies per node or per interaction — that just leaks a JSON value per
  interaction into a map that is never cleaned.
- **Sending the same id twice is a client-side error, not a no-op.** The
  client's `ConstantPool.importFromJson` asserts that the key is not
  already present, and the TypeScript assertions are always on (unlike
  the GWT ones they were ported from, which production stripped). The
  server's `knownValues` set is the only thing preventing that, so the
  two sides' bookkeeping must stay in step: do not clear or rebuild one
  without the other. Resynchronization deliberately resets neither —
  `StateTree.prepareForResync` rebuilds the client's state tree, and the
  ids in the replayed changes still have to resolve.
- **Read constants by key on the client, and assert presence**, as
  `SimpleElementBindingStrategy.handleDomEvent` and
  `ServerEventObject.getEventData` do. A missing key means the two pools
  diverged, and failing loudly at that point is far cheaper to debug than
  the behaviour that follows.

The canonical producer is `ElementListenerMap`, which stores each event
type's expression settings (event data expressions, filters, debounce
phases) as a constant: two thousand buttons sharing one click listener
configuration cost one constant pool entry instead of two thousand copies
of the same object. `ConstantPoolPerformanceView` in `flow-tests` exists
to make that difference visible. `PolymerEventListenerMap` does the same
for its event data expressions.

## `executeJs` over the wire

### Server side

`Page.executeJs` and `Element.executeJs` build a `JavaScriptInvocation`
(the expression plus its parameters), wrap it in a
`PendingJavaScriptInvocation` owned by a `StateNode`, and queue it on
`UIInternals`.

- Parameters are **dry-run encoded in the constructor** so an unsupported
  parameter type throws at the call site, where the stack trace is
  useful. The encoding that actually ships happens when the response is
  written, which is why an `Element` parameter resolves against its
  attachment state at flush time, not at call time.
- `Element.executeJs` appends the element itself as an extra trailing
  parameter and wraps the expression in
  `return (async function() { … }).apply($n)`, which is what makes `this`
  the element. A parameter index in user code therefore does not have to
  be the last index on the wire.
- An invocation is sent only once its owner node is attached and visible.
  `dumpPendingJavaScriptInvocations` partitions the queue on visibility
  and retains the rest; detaching the owner discards them. **New code
  must not assume an `executeJs` issued during a request is sent in that
  request's response.**
- `then(...)` subscribes to the return value and throws
  `IllegalStateException` once the invocation has been sent. When there
  is a subscriber, `UidlWriter` rewrites the expression into a
  `try { Promise.resolve((async function(){ … })()).then($ok, $err) }
  catch { … }` wrapper and appends two extra return-channel parameters.
  The expression the client runs is therefore not the string that was
  passed in.

### Wire shape

Each invocation is a JSON array of `[param0, param1, …, expression]` —
**the expression is the last element**, not the first. The client reads
the last entry as the code and names the remaining ones `$0`, `$1`, ….

Parameters are encoded by `JacksonCodec.encodeWithTypeInfo`. Native JSON
types travel as themselves; everything JSON has no representation for
travels as a single-key wrapper object whose key is reserved:

- `{"@v-node": <nodeId>}` — an `Element` or `Component`, decoded to its
  DOM node.
- `{"@v-return": [<nodeId>, <channelId>]}` — a return channel, decoded to
  a callback that sends a `channel` RPC back to the server.
- `{"@v-fn": {"body": …, "captures": […], "args": […]}}` — a `JsFunction`,
  manifested on the client as a real function with its captures pre-bound.
  This is how `Element.addJsInitializer` ships a user expression as a
  parameter of a fixed framework expression.

`ClientJsonCodec.decodeWithTypeInfo` decodes these **recursively through
objects and arrays**. The whole `@v-` prefix is consequently reserved on
the wire: a bean that serializes to an object carrying one of the keys
above is reinterpreted as a reference, and one carrying any other `@v-`
key is rejected outright. A new wrapper type has to be added to both
codecs at once, under a new `@v-` key.

### Client side

`ExecuteJavaScriptProcessor` decodes the parameters, builds
`new Function('$0', …, expression)` and applies it with a context object
as `this`. That context carries `getNode`, `$appId`, the registry and the
framework callbacks (`attachExistingElement`, `registerInitializer`,
`disposeInitializer`, `stopApplication`, …) that framework-authored
expressions rely on.

**An invocation referencing a node that is not bound yet is deferred, not
dropped**: the processor registers a DOM-node listener and retries the
whole invocation afterwards. An invocation can therefore run after
invocations from a later message, so do not rely on ordering between
`executeJs` calls targeting different elements.

Exceptions thrown by the expression are caught and reported; outside
production mode the failing code is logged as well.

### Constraints this puts on new code

- **Never concatenate values into the expression string** — pass them as
  parameters. See [Browser Integration](browser-integration.md) for the
  full set of rules around calling into the browser.
- **The expression string is not deduplicated.** Unlike event settings, it
  is sent verbatim on every invocation and never goes through the constant
  pool. A long expression called on every interaction, or once per element
  in a large component, pays for its full text every time. Put the body in
  a module (`@JsModule`, or `window.Vaadin.Flow.*`) and send a short call
  instead — this is the wire-level reason behind the "non-trivial JS goes
  in its own file" rule.
- **Subscribe before it is sent.** Attach `then(...)` on the same server
  visit that created the invocation.
