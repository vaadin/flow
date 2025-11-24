# Static VAADIN Folder Mapping Analysis

## The Key Rule

**The VAADIN folder is always relative to the servlet path (URL mapping), not just the context path.**

## Path Construction

| Configuration | Context Path | Servlet Path (urlMapping) | VAADIN URL |
|---------------|--------------|---------------------------|------------|
| Default (`/*`) | `/myapp` | `""` | `/myapp/VAADIN/...` |
| Custom (`/ui/*`) | `/myapp` | `/ui` | `/myapp/ui/VAADIN/...` |
| Spring root mapping | `/myapp` | `/vaadinServlet` (internal) | `/myapp/VAADIN/...` |

## How It Works

### 1. URL Mapping Application

`RequestUtil.applyUrlMapping()` in `vaadin-spring/.../RequestUtil.java:475-488`:

```java
// Strips trailing /* or / from urlMapping, then prepends to path
// e.g., urlMapping="/ui/*", path="/VAADIN/build/..." → "/ui/VAADIN/build/..."
```

### 2. Static File Resolution

`StaticFileServer.getRequestFilename()` at `flow-server/.../StaticFileServer.java:580-602`:

- For requests to `/VAADIN/*`, returns just the `pathInfo` (stripping servlet path)
- This allows the server to find resources regardless of servlet mapping

### 3. Spring Root Mapping Special Case

- When `vaadin.urlMapping=/*` (default), Spring internally uses `/vaadinServlet/*` mapping
- A `DispatcherServlet` at root forwards to VaadinServlet
- Static resources still appear at context root level (`/myapp/VAADIN/...`)

## Resource Locations (Server-Side)

Resources are loaded from these locations (in order):

1. **Dev mode**: `{projectFolder}/{buildFolder}/webapp/VAADIN/...`
2. **Production**: `META-INF/VAADIN/webapp/VAADIN/...` (from classpath/JARs)
3. **ServletContext**: `webapp/VAADIN/...` (traditional webapp resources)

## Security Configuration

`HandlerHelper.java:148-171` defines that `/VAADIN/**` requires security context, and the security matchers apply the URL mapping:

```java
// /VAADIN/** becomes /ui/VAADIN/** when urlMapping="/ui/*"
```

## Summary

**VAADIN folder URL = Context Path + Servlet Path (minus wildcard) + `/VAADIN/...`**

- Context path only: `/context/VAADIN/...`
- With servlet mapping `/ui/*`: `/context/ui/VAADIN/...`
- Spring root mapping: `/context/VAADIN/...` (servlet path hidden internally)
