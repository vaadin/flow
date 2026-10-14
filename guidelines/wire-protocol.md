# Wire Protocol

What a server response actually contains, how the constant pool keeps it
small, and how an `executeJs` call gets from a Java method to a running
JavaScript function. Read this before adding anything new that travels
between the server and the client.

The mechanics here are internal and may change, but the constraints they
impose on new code are real — most of them only surface as a runtime
failure or a memory leak in a long-lived UI. The checkable form of those
constraints is in the Client-Side JavaScript section of
[`CONVENTIONS.md`](../CONVENTIONS.md); this chapter is the reasoning
behind them.

## The UIDL response

`UidlWriter.createUidl` builds one JSON object per server response. The
keys that matter for new code, in the order the client applies them:

| Key         | Contents                                                       |
| ----------- | -------------------------------------------------------------- |
| `constants` | New constant pool entries (see below), as `id` → value.         |
| `changes`   | State tree changes, each encoded by a `NodeChange`.              |
| `execute`   | Pending `executeJs` invocations, one array per invocation.       |

`MessageHandler` applies them in exactly that order. Constants are
imported as soon as a message arrives, before it is even decided whether
the message is handled now or queued — both changes and invocations
refer to constants, and so does the check for a forced reload during
resynchronization. Changes are applied next, and the `execute` list runs
last, behind a doubly nested post-flush listener, so the scripts see the
DOM that the same response's changes produced, including any post-flush
listener added while applying them.

**The server writes them in a different order: `changes`, then
`execute`, then `constants`.** `encodeChanges` runs first; the
`beforeClientResponse` executions that run during change encoding can
queue more JavaScript, so `dumpPendingJavaScriptInvocations` comes after
it. Both steps register constants — a change for the values it refers
to, an invocation for what it runs — so `dumpConstants` comes last: it
clears the new-key set, and a constant registered after it would miss the
message. Anything new that contributes to the response has to be slotted
into that sequence rather than appended at the end.

## Constant pool

The constant pool deduplicates JSON values that would otherwise be sent
again and again: listener settings shared by many state nodes, and what
each `executeJs` invocation runs. The value is sent once per UI under a
short id, and everything that uses it carries only the id.

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
- There are two ways in. A value stored in a state node as a
  `ConstantPoolKey` is replaced by its id when the `NodeChange` encoders
  call `JacksonCodec.encodeWithConstantPool`; every other node value is
  encoded inline. And `UidlWriter` registers what each `executeJs`
  invocation runs directly with the pool while encoding it —
  `UidlRequestHandler` does the same for a reload script it adds to a
  response that is already written, which is why it patches `constants`
  in place.

### What is in the pool today

Three kinds of value. Two node features store the settings of a DOM event
listener: `ElementListenerMap` one constant per event type on an element,
`PolymerEventListenerMap` the event data expressions of a Polymer
template listener. The third kind is what an `executeJs` invocation
runs, covered [below](#wire-shape).

An `ElementListenerMap` value is an object keyed by the JavaScript
expressions the client evaluates when the event fires. The value of each
key says whether that expression is also a filter that decides whether
the event is sent at all, and takes one of exactly three forms:

- `false` — a plain expression, evaluated and sent as event data.
- `true` — a filter with no debounce.
- `[[timeout, phase…], …]` — a filter with debounce settings.

```json
"constants": {
  "RBNvo1WzZ4o=": {},
  "J4r/ss0KY+c=": { "event.clientX": false, "event.clientY": false },
  "24b7yiAh6SA=": { "event.button === 0": true },
  "vdAdQQWwVaQ=": { "1": [[250, "trailing"]] },
  "gKEp5ocBgAc=": { "}value": false }
}
```

Where each of those comes from:

- `{}` — a plain listener with no event data: nothing for the client to
  evaluate, it just sends the event.
- `{ "event.clientX": …, "event.clientY": … }` — two `addEventData(…)`
  calls.
- `{ "event.button === 0": true }` — `setFilter("event.button === 0")`.
- `{ "1": [[250, "trailing"]] }` — `debounce(250)`. There is no real
  filter, so the always-true filter expression `1` carries the debounce
  settings. `1` also shows up as `"1": true` when an element has both
  filtered and unfiltered listeners for one event type, so that the
  unfiltered ones are still notified when no filter matches.
- `{ "}value": false }` — `synchronizeProperty("value")`. The `}` prefix
  (`JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN`) marks a property to read
  off the element and send back, rather than an expression to evaluate.

The change that uses one carries only the id — a `put` on the element's
listener feature with `"key": "click"` and `"value": "J4r/ss0KY+c="`.
That is where the saving is: two thousand buttons sharing one click
listener configuration cost one entry instead of two thousand copies of
the same object, which is what `ConstantPoolPerformanceView` in
`flow-tests` makes visible.

The ids above are the real hashes of those values, but the hash is taken
over `json.toString()` — the compact form, without the spaces the
snippet is pretty-printed with. Anything that changes the serialized
form, key order included, changes the id.

### Constraints this puts on new code

- **Nothing is ever evicted, on either side.** The pool grows for the
  lifetime of the UI. A constant pool key is right for a value with few
  distinct shapes repeated over many nodes, and wrong for anything that
  varies per node or per interaction — that just leaks a JSON value per
  interaction into a map that is never cleaned.
- **An id names one value, forever.** The client's
  `ConstantPool.importFromJson` accepts a key it already holds only with
  the value it already holds — the same message can legitimately be read
  twice, when it is queued or re-sent — and fails an assertion for a key
  that arrives with a different value. The TypeScript assertions are
  always on, unlike the GWT ones they were ported from. The server's
  `knownValues` set is what keeps each id to one message, so the two
  sides' bookkeeping must stay in step: do not clear or rebuild one
  without the other. Resynchronization deliberately resets neither —
  `StateTree.prepareForResync` rebuilds the client's state tree, and the
  ids in the replayed changes still have to resolve.
- **Read constants by key on the client, and assert presence**, as
  `SimpleElementBindingStrategy.handleDomEvent` does. A missing key means
  the two pools diverged, and failing loudly at that point is far cheaper
  to debug than the behaviour that follows —
  `ServerEventObject.getEventData` has no such guard, and a divergence
  there surfaces as a null dereference several frames away.

## `executeJs` over the wire

There are two ways to run JavaScript from the server, and they share one
wire shape:

- **An expression** — `Page.executeJs(String, Object...)` and
  `Element.executeJs(String, Object...)`. The JavaScript is a string built
  at the call site and compiled in the browser.
- **Declared JavaScript** — `Page.executeJs(Class)` and
  `Element.executeJs(Class)` with an interface annotated with
  `@JsDefinition`, whose methods carry their JavaScript in
  `@JsExpression`. The build collects every such method into the bundle as
  a function, so nothing is compiled in the browser: this is the path that
  works under a content security policy without `unsafe-eval`.

### Server side

Both build a `JavaScriptInvocation` — an expression plus its parameters,
and for declared JavaScript also the `JsCall` it came from — wrap it in a
`PendingJavaScriptInvocation` owned by a `StateNode`, and queue it on
`UIInternals`.

- Parameters are **dry-run encoded in the constructor** so an unsupported
  parameter type throws at the call site, where the stack trace is
  useful. The encoding that actually ships happens when the response is
  written, which is why an `Element` parameter resolves against its
  attachment state at flush time, not at call time.
- `Element.executeJs(String, …)` appends the element itself as an extra
  trailing parameter and wraps the expression in
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
  is a subscriber, `UidlWriter` appends two return-channel parameters,
  success and then error. For an expression it also rewrites the
  expression into a
  `try { Promise.resolve((async function(){ … })()).then($ok, $err) }
  catch { … }` wrapper, so the expression the client runs is not the
  string that was passed in; for declared JavaScript the client does the
  same wrapping itself. The second channel is the error channel a call
  has to report through: anything the client cannot execute must reach
  it, or the `PendingJavaScriptResult` never completes.

### Wire shape

Each invocation is a JSON array whose **last element is a constant pool
id naming what to run** — not the JavaScript itself. The rest are the
parameters. The constant is one of two things:

```json
"constants": {
  "7ItLxuJbO4w=": "return (async function() { this.focus()}).apply($0)",
  "9SyADOkTfys=": { "f": "ef73ee9ae94cdca543ea12f4263d0fcaeec3bdd7f22ce6b1eef43c177cce865e" }
},
"execute": [
  [{ "@v-node": 5 }, "7ItLxuJbO4w="],
  [120, { "@v-node": 5 }, "9SyADOkTfys="]
]
```

- **A string** is an expression: here `element.executeJs("this.focus()")`,
  with the element as its one parameter. The client names the parameters
  `$0`, `$1`, … and compiles the string with them.
- **An object** is declared JavaScript. `f` (`UIDL_KEY_JS_FUNCTION`) is
  the function id — a hex SHA-256 of the method's parameter count, whether
  it is variadic, and its `@JsExpression`, see `JsCall.functionId` — so
  no Java class or method name reaches the browser. Here it is a one-argument method declaring
  `this.scrollTop = $0`. `n` (`UIDL_KEY_JS_ARGUMENT_COUNT`) is added only
  for a variadic method, whose rest parameter the client cannot count.
  The parameters are `[arguments…, element, success?, error?]`, with
  `null` as the element for `Page.executeJs(Class)`.

Because an invocation carries only the id, an expression that runs again
costs a reference, not its text. Declared JavaScript goes one step
further and sends no JavaScript at all.

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

`ExecuteJavaScriptProcessor` decodes the parameters and looks up the
constant the invocation names. A missing constant is logged and the
invocation dropped.

- For a string it builds `new Function('$0', …, expression)` and applies
  it with a context object as `this`. That context carries `getNode`,
  `$appId`, the registry and the framework callbacks
  (`attachExistingElement`, `registerInitializer`, `disposeInitializer`,
  `stopApplication`, …) that framework-authored expressions rely on.
- For an object it looks the function up in
  `window.Vaadin.Flow.jsDefinitions`, which the generated bundle fills,
  and applies it to the element with the arguments. There is no context
  object on this path. A function missing from the bundle, or a
  parameter count that does not match it, is reported through the error
  channel rather than run.

**An invocation referencing a node that is not bound yet is deferred, not
dropped**: the processor registers a DOM-node listener and retries the
whole invocation afterwards. An invocation can therefore run after
invocations from a later message, so do not rely on ordering between
`executeJs` calls targeting different elements.

Exceptions thrown by the JavaScript are caught and reported. Outside
production mode the failing expression is logged too; a declared function
is named by what the developer wrote when the development bundle carries
it, and by its function id otherwise.

### Constraints this puts on new code

- **Prefer declared JavaScript for new framework code.** It works without
  `unsafe-eval`, sends no JavaScript, and keeps Java names off the wire.
- **Never concatenate values into an expression** — pass them as
  parameters. See [Browser Integration](browser-integration.md) for the
  full set of rules around calling into the browser. On the wire this is
  also a leak: every distinct expression string becomes a pool entry
  that is never evicted, so an expression with a value baked in adds one
  per call for the lifetime of the UI.
- **Keep what an invocation runs stable.** For the same reason, do not
  generate expression variants per case — one expression with parameters
  is one constant, sent once.
- **Subscribe before it is sent.** Attach `then(...)` on the same server
  visit that created the invocation.
