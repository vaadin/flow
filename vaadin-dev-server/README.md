# vaadin-dev-server

## Development

### Frontend

To install NPM dependencies:
```shell
npm install
```

To format code using Prettier:
```shell
npm run prettier
```

### Using the local dev tool files in a Flow application

In order to work iteratively on the dev tool UI, this project contains a frontend dev server setup that allows to use the local version of the dev tool files within an actual Flow app.
The dev server supports hot module reload (HMR), which allows making changes to the dev tool without requiring to rebuild / restart the Flow app, or having to reload the page in the browser.

First, start the Flow app that you want to develop the dev tool in.
Then patch the app to use the local files using:
```shell
npm run patch-app /path/to/flow-app
```

Then start the frontend dev server:
```shell
npm install
npm start
```

Open the Flow app in the browser and open the dev tool.
Then start making changes to the local dev tools files (e.g. change a CSS style).
Verify the changes are immediately reflected in the dev tool opened in the browser.

## The dev-loop connector

`com.vaadin.base.devserver.devloop` is the in-application half of the
`vaadin-dev` dev loop: it holds the registration connection the daemon drives,
performs one atomic redefine of every loaded copy of a changed class, and pushes
changed stylesheets. The daemon itself is a separate module,
[`flow-devloop-daemon`](../flow-devloop-daemon/README.md), whose README carries
the protocol, the outcome vocabulary and the known limits.

It is not a module of its own because `VaadinHotswapper` — the SPI it observes —
lives here, so putting it here means no new artifact, no BOM entry and no change
to any application's pom: it travels in with `com.vaadin:vaadin-dev`, which every
starter already declares.

**Nothing here is installed unless the daemon launched the JVM.** That is not a
nicety: registering Flow's `Hotswapper` instantiates every `VaadinHotswapper` on
the classpath, and one of them installs an after-navigation listener that walks
the component tree — a cost no application should pay for a loop it is not using.
So `DevLoopInitListener` returns immediately unless `vaadin.devloop.daemonPort`
and `vaadin.devloop.token` are both set, which only the daemon sets, and
`DevLoopHotswapper` never becomes the active instance otherwise. The handshake
properties are the opt-in; there is no separate flag that could disagree with
them.

`DevLoopRegistration.start` calls `Hotswapper.register(service)` itself. Nothing
else in Flow does, and nothing should: `onHotswap` is an instance method, so a
tool that wants to drive it has to obtain the instance, and `register` is the only
way to get one.

Three things outside the package exist for the connector, and all three are
improvements in their own right:

- **`Hotswapper.register` is idempotent, and `Hotswapper.getRegistered` is new.**
  Registration has two callers — this connector, and any hotswap agent that
  injects its own call — and two instances would double every refresh.
  `getRegistered` is how a component that did not do the registering finds the
  result.
- **`PublicResourcesLiveUpdater.suspend(VaadinContext)`** lets a tool that owns
  the edit-to-running-app loop turn the CSS watcher off. Watching on *save* is
  the wrong trigger for such a tool: the loop decides when a change goes live,
  and a second watcher pushing on its own makes "what is the state of my last
  change?" unanswerable.
- **`PublicStyleSheetBundler.toUnixSeparators`** replaced two
  `new File(url).toPath()` calls. A stylesheet URL can carry its scheme
  (`context://styles.css`, `base://css/app.css`), a colon is illegal in a Windows
  path, and `InvalidPathException` there degraded every in-place CSS update to a
  full page reload.
