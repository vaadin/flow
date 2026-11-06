# SSE push transport add-on (experimental example)

A push transport for Vaadin Flow that streams messages from the server with
[server-sent events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events),
without Atmosphere. Messages to the server are regular requests, as with the
default `WEBSOCKET_XHR` transport.

The add-on is built only against the public push transport API of Flow
(`PushTransport`, `ServiceInitEvent.addPushTransport`, `@Push(transportName)`),
to show that the API is enough for a transport. It lives in the Flow test
suite and is not published, but it is written like a real add-on, and the
instructions below are the ones an add-on like this would give. When
published, its coordinates would change from `com.vaadin:flow-test-sse-push-addon`.

## What you get

- No Atmosphere: the application leaves out `flow-push`, about 850 kB of jars,
  and the browser does not load the Atmosphere script.
- Plain HTTP: the event stream needs no `Upgrade` handling from reverse
  proxies, and it tells nginx not to buffer it.
- Robustness: keep-alive events every 30 seconds, a client that reopens a
  stream that has gone silent, writes that never block a session on a slow
  client, and streams ended without holding the session lock.

## Using it with Spring Boot

An application created at [start.vaadin.com](https://start.vaadin.com), or
from the [Spring Boot starter](https://github.com/vaadin/skeleton-starter-flow-spring),
depends on `vaadin`, `vaadin-spring-boot-starter` and, for development,
`vaadin-dev`. Exclude Atmosphere from the first two, and add the add-on:

```xml
<dependencies>
    <dependency>
        <groupId>com.vaadin</groupId>
        <artifactId>vaadin</artifactId>
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
        <artifactId>vaadin-spring-boot-starter</artifactId>
        <exclusions>
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

    <!-- Unchanged: the dev tools connect through Atmosphere, so they keep it -->
    <dependency>
        <groupId>com.vaadin</groupId>
        <artifactId>vaadin-dev</artifactId>
        <optional>true</optional>
    </dependency>
    ...
</dependencies>
```

Both exclusions are needed, as both bring `flow-push`: `vaadin` through
`vaadin-core`, and the starter through `vaadin-spring`. A Maven exclusion
applies to everything the dependency brings, so one per dependency is enough.
Use `vaadin-core` instead of `vaadin` in the same way.

`vaadin-dev` is left as it is. It is optional, so `spring-boot:run` has it,
but the Spring Boot plugin leaves it out of the production jar, and with it
Atmosphere. Keep `spring-boot-starter-websocket`, which the starter brings:
the dev tools use websockets in development.

Enable push as usual:

```java
@Push
public class Application implements AppShellConfigurator {
}
```

Push uses the add-on, in development and in production. Nothing else is
needed:

- The add-on registers itself through a `VaadinServiceInitListener` found with
  the service loader, so there is no Spring configuration to add.
- `VaadinSecurityConfigurer` already lets requests to `/VAADIN/**` through, so
  the event stream passes Spring Security, also with `vaadin.url-mapping`.

To check that Atmosphere is gone, list what still brings it:

```
mvn dependency:tree -Dincludes=com.vaadin:flow-push
```

The only path left should go through `vaadin-dev`.

## Using it with Quarkus

An application created from the
[Quarkus starter](https://github.com/vaadin/base-starter-flow-quarkus) depends
on `vaadin-quarkus-extension`, `vaadin` and `vaadin-jandex`. Exclude Atmosphere
from the first two, and add the add-on:

```xml
<dependencies>
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
        <artifactId>vaadin</artifactId>
        <exclusions>
            <exclusion>
                <groupId>com.vaadin</groupId>
                <artifactId>flow-push</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>com.vaadin</groupId>
        <artifactId>vaadin-jandex</artifactId>
    </dependency>
    <dependency>
        <groupId>com.vaadin</groupId>
        <artifactId>flow-test-sse-push-addon</artifactId>
        <version>${vaadin.version}</version>
    </dependency>
</dependencies>
```

Enable push with `@Push` on the `AppShellConfigurator`, as above. The add-on
is found with the service loader, so it needs no CDI beans or Quarkus
configuration.

Not checked yet: whether the dev tools keep their Atmosphere connection in
`quarkus:dev` with these exclusions. If they show their connection as
unavailable, move the exclusions to a production profile.

## Using it with Gradle

Exclude Atmosphere per dependency, not from all configurations, so that the
dev tools keep it:

```groovy
dependencies {
    implementation('com.vaadin:vaadin') {
        exclude group: 'com.vaadin', module: 'flow-push'
    }
    implementation('com.vaadin:vaadin-spring-boot-starter') {
        exclude group: 'com.vaadin', module: 'flow-push'
    }
    implementation 'com.vaadin:flow-test-sse-push-addon:<vaadin version>'
    developmentOnly 'com.vaadin:vaadin-dev'
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
- The client side of the transport, `sse-push.js` in
  `META-INF/resources/frontend`, is in the frontend bundle of the application,
  as `SsePushServiceInitListener` has a `@JsModule` for it. It registers the
  transport in `window.Vaadin.Flow.pushTransports` when the application
  loads, so it needs no request of its own.
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
