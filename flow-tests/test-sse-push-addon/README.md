# SSE push transport add-on (experimental example)

A push transport for Vaadin Flow that streams messages from the server with
[server-sent events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events),
without Atmosphere. Messages to the server are regular requests, as with the
default `WEBSOCKET_XHR` transport.

The add-on is built only against the public push transport API of Flow
(`PushTransport`, `ServiceInitEvent.addPushTransport`, `@Push(transportName)`),
to show that the API is enough for a transport. It lives in the Flow test
suite and is not published, but it is written like a real add-on, and the
instructions below are the ones an add-on like this would give.

## What you get

- No Atmosphere: the application leaves out `flow-push`, about 850 kB of jars,
  and the browser does not load the Atmosphere script.
- Plain HTTP: the event stream needs no `Upgrade` handling from reverse
  proxies, and it tells nginx not to buffer it.
- Robustness: keep-alive events every 30 seconds, a client that reopens a
  stream that has gone silent, writes that never block a session on a slow
  client, and streams ended without holding the session lock.

## Using it with Spring Boot

Leave Atmosphere out of the Vaadin starter, and add the add-on:

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-spring-boot-starter</artifactId>
    <exclusions>
        <!-- Atmosphere, needed only by the built-in push transports -->
        <exclusion>
            <groupId>com.vaadin</groupId>
            <artifactId>flow-push</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>flow-test-sse-push-addon</artifactId>
    <version>${vaadin.version}</version>
</dependency>
```

A Maven exclusion applies to everything the starter brings, so this one
exclusion removes `flow-push` also from `vaadin-core` and `vaadin-spring`,
which depend on it. The starter also brings `spring-boot-starter-websocket`,
which the add-on does not need either; you can exclude it the same way.

Enable push as usual:

```java
@Push
public class Application implements AppShellConfigurator {
}
```

Push uses the add-on. Nothing else is needed:

- The add-on registers itself through a `VaadinServiceInitListener` found with
  the service loader, so there is no Spring configuration to add.
- `VaadinSecurityConfigurer` already lets requests to `/VAADIN/**` through, so
  the event stream passes Spring Security, also with `vaadin.url-mapping`.

## Using it with Quarkus

Leave Atmosphere out of the Vaadin extension, and add the add-on:

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-quarkus-extension</artifactId>
    <exclusions>
        <!-- Atmosphere, needed only by the built-in push transports -->
        <exclusion>
            <groupId>com.vaadin</groupId>
            <artifactId>flow-push</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>flow-test-sse-push-addon</artifactId>
    <version>${vaadin.version}</version>
</dependency>
```

Enable push with `@Push` on the `AppShellConfigurator`, as above. The add-on
is found with the service loader, so it needs no CDI beans or Quarkus
configuration.

## Using it with Gradle

```groovy
configurations.all {
    exclude group: 'com.vaadin', module: 'flow-push'
}

dependencies {
    implementation 'com.vaadin:flow-test-sse-push-addon:<vaadin version>'
}
```

## Development mode

The development tools of Vaadin connect to the server through Atmosphere, so
`vaadin-dev` keeps `flow-push` in development also when the application
leaves it out. The push of the application uses the add-on anyway, so it
behaves the same in development and in production.

## Choosing a transport

With the add-on on the classpath, push uses it unless the application chooses
a transport. To use a built-in transport next to the add-on, name it:

```java
@Push(transportName = "websocket-xhr")
public class Application implements AppShellConfigurator {
}
```

The built-in transports need Atmosphere, so without it push uses the add-on
whatever transport is chosen.

## Configuration

| Property | Default | |
|---|---|---|
| `vaadin.pushSseKeepAliveInterval` | `30` | Seconds between keep-alive events, `0` disables them. The client reopens a stream when nothing arrives for two intervals. |

Pass it as a system property, for example
`-Dvaadin.pushSseKeepAliveInterval=15`, or as a servlet init parameter
`pushSseKeepAliveInterval`. Spring Boot passes only the `vaadin.*` properties
of Flow itself from `application.properties` to Vaadin, so with Spring Boot
it has to be a system property.

## Reverse proxies

The add-on is tested through nginx and Caddy terminating HTTPS, with a
configuration tuned for Vaadin and with the defaults of the proxy. It works
with both, including nginx with its defaults, which breaks websocket push.

A proxy that limits the total time of a response, such as Envoy with its
15 second default, ends every stream at that limit. The client opens a new
stream right away, but give `VAADIN/push` a longer limit.

Over HTTP/1.1, each open stream takes one of the six connections a browser
opens to a server, so a seventh tab to the application stalls. HTTP/2 and
HTTP/3 have no such limit.

## How it works

- `SsePushServiceInitListener` adds the `SsePushTransport` and the
  `SseRequestHandler` to every Vaadin service.
- The client engine loads `VAADIN/static/sse-push/sse-push.js` from the jar,
  which registers the client side of the transport in
  `window.Vaadin.Flow.pushTransports`.
- The client opens an `EventSource` to `VAADIN/push?v-r=sse`, and
  `SseRequestHandler` connects it to the `SsePushConnection` of the UI.
- `SsePushConnection` writes each message as an event. Writes are queued and
  carried out on the executor of the service, so a push never waits for a
  slow client.

## Tests

- Spring Boot with embedded Tomcat and Spring Security, Vaadin mapped to
  `/ui/*`: `flow-tests/vaadin-spring-tests/test-spring-boot-sse-addon`
- Quarkus: `flow-tests/vaadin-quarkus-tests/sse-addon-it`
- Spring Boot in development mode, with the dev tools and their Atmosphere:
  `flow-tests/vaadin-spring-tests/test-spring-boot-sse-addon-dev`

The first two run in production mode with `flow-push` excluded, also through the
reverse proxies with `-Dreverse.proxy=<nginx|caddy>
-Dreverse.proxy.config=<recommended|default>`. They cover pushing, client
requests while pushing, a large multibyte message, a refresh, a stream lost
silently and a client that stops reading.

The exclusions above are tested with the Flow artifacts these platform
artifacts depend on, as the test suite builds Flow, not the platform.
