# Manifest-Based JAR Scanning Implementation Plan

## Overview

This document describes the implementation of manifest-based JAR filtering for Vaadin Flow's annotation scanner. The goal is to significantly improve build performance by only scanning JAR files that are explicitly marked as Vaadin add-ons via the `Vaadin-Package-Version` manifest attribute.

## Problem Statement

Currently, Vaadin scans all dependencies in the classpath to find annotations like `@Route`, `@NpmPackage`, `@JsModule`, etc. This is slow because most dependencies have no relationship with Vaadin. For typical projects with many third-party dependencies, this can add significant overhead to both development mode startup and production builds.

## Solution

Only scan:
1. The application's own classes (output directory) - always
2. Dependencies marked with `Vaadin-Package-Version` in `META-INF/MANIFEST.MF` (when in ADD_ON mode)
3. Dependencies explicitly included via configuration overrides

## Implementation Status

### ✅ Phase 1: Core Manifest Filtering (COMPLETE)

**Commit:** `27215103a0`

#### Components Implemented

1. **JarManifestChecker Utility**
   - File: `flow-plugins/flow-plugin-base/src/main/java/com/vaadin/flow/plugin/base/JarManifestChecker.java`
   - Checks if JAR files contain `Vaadin-Package-Version` manifest attribute
   - Static caching using `ConcurrentHashMap` for performance
   - Thread-safe and efficient

2. **AnnotationScannerMode Enum**
   - Location: `FrontendScannerConfig.java`
   - Values:
     - `ADD_ON`: Only scan JARs with Vaadin manifest (recommended for performance)
     - `FULL`: Scan all JARs (legacy behavior, current default)

3. **FrontendScannerConfig Enhancements**
   - File: `flow-plugins/flow-maven-plugin/src/main/java/com/vaadin/flow/plugin/maven/FrontendScannerConfig.java`
   - Added scanner mode field (defaults to `FULL`)
   - Added getter/setter methods
   - Updated `toString()` to include scanner mode

4. **Reflector Manifest Filtering**
   - File: `flow-plugins/flow-maven-plugin/src/main/java/com/vaadin/flow/plugin/maven/Reflector.java`
   - Integrated manifest checking in `createIsolatedClassLoader()`
   - Combines explicit include/exclude rules WITH manifest filtering
   - Output directory always scanned regardless of manifest
   - Debug logging for transparency

5. **Unit Tests**
   - File: `flow-plugins/flow-plugin-base/src/test/java/com/vaadin/flow/plugin/base/JarManifestCheckerTest.java`
   - 10 comprehensive test cases
   - All tests passing
   - Coverage includes:
     - JARs with/without manifest
     - Cache behavior
     - Edge cases (null, non-existent files, directories)

### ✅ Phase 2: Property Configuration (COMPLETE)

**Commit:** `7c4873d854`

#### Components Implemented

1. **Maven Parameter**
   - File: `flow-plugins/flow-maven-plugin/src/main/java/com/vaadin/flow/plugin/maven/FlowModeAbstractMojo.java`
   - Property: `vaadin.annotationScanner`
   - Default: `FULL` (backward compatible)
   - Comprehensive JavaDoc with usage examples

2. **Scanner Mode Parsing**
   - Implemented in `getOrCreateReflector()` method
   - Case-insensitive parsing
   - Dash-to-underscore conversion (`add-on` → `ADD_ON`)
   - Creates config if not already present
   - Validation with helpful error messages
   - Debug logging

## How to Use

### Command Line

```bash
mvn clean install -Dvaadin.annotationScanner=ADD_ON
```

### In pom.xml Properties

```xml
<properties>
    <vaadin.annotationScanner>ADD_ON</vaadin.annotationScanner>
</properties>
```

### Via Maven Plugin Configuration

```xml
<plugin>
    <groupId>com.vaadin</groupId>
    <artifactId>flow-maven-plugin</artifactId>
    <configuration>
        <annotationScanner>ADD_ON</annotationScanner>
    </configuration>
</plugin>
```

### Accepted Values

- `ADD_ON` (or `add-on`, `Add-On`) - Only scan JARs with Vaadin manifest
- `FULL` (or `full`, `Full`) - Scan all JARs (default)

## Performance Benefits

Expected improvement when using `ADD_ON` mode:
- **50-80% reduction in scan time** for typical projects
- Faster development mode startup
- Faster production builds

## Manifest Filtering in Different Environments

### Maven Build Goals (prepare-frontend, build-frontend)

Manifest filtering is **ACTIVE** in Maven build goals when using `vaadin.annotationScanner=ADD_ON`.

### How prepare-frontend and build-frontend Are Affected

The manifest filtering implementation affects **both** Maven goals since they both use the same `Reflector` infrastructure:

**prepare-frontend (PROCESS_RESOURCES phase):**
- Creates a Reflector with ClassFinder for basic classpath setup
- Does NOT use `FrontendDependenciesScanner` for intensive annotation scanning
- The "Reflections took X ms to scan Y urls" message comes from the Reflections library during ClassFinder initialization
- Still benefits from manifest filtering because fewer JARs are added to the ClassFinder's scan URLs
- The URL count in this message indicates how many JARs are being considered for scanning

**build-frontend (PREPARE_PACKAGE phase):**
- Creates a new Reflector instance (separate from prepare-frontend)
- DOES use `FrontendDependenciesScanner` for intensive bytecode scanning of annotations
- Reports "Scanned X classes in Y ms" which shows the result of annotation scanning
- Benefits significantly from manifest filtering as fewer classes need bytecode analysis

### How Manifest Filtering Helps Both Goals

When `ADD_ON` mode is enabled:
1. Both goals create their Reflector with the same `FrontendScannerConfig` containing the scanner mode
2. Both goals filter artifacts in `Reflector.createIsolatedClassLoader()` using `JarManifestChecker`
3. Both goals create a `ReflectorClassLoader` with a reduced set of `urlsToScan`
4. **prepare-frontend**: Fewer URLs → faster Reflections initialization → reduced "Reflections took X ms" time
5. **build-frontend**: Fewer URLs → fewer classes to scan → reduced "Scanned X classes in Y ms" time

### Expected Output

**Before filtering (FULL mode):**
```
[INFO] --- flow-maven-plugin:prepare-frontend ---
[INFO] Reflections took 1404 ms to scan 152 urls

[INFO] --- flow-maven-plugin:build-frontend ---
[INFO] Scanned 15000 classes in 3000 ms
```

**After filtering (ADD_ON mode):**
```
[INFO] --- flow-maven-plugin:prepare-frontend ---
[INFO] Reflections took 400 ms to scan 25 urls

[INFO] --- flow-maven-plugin:build-frontend ---
[INFO] Scanned 1721 classes in 859 ms
```

### Troubleshooting: High URL Count in prepare-frontend

If you see a high URL count (e.g., 152 URLs) in prepare-frontend even with `ADD_ON` mode enabled, this could indicate:

1. **Filtering not applied**: The property might not be read correctly by the plugin
2. **Many Vaadin add-ons**: Your project might genuinely have many dependencies with Vaadin manifests
3. **Configuration issue**: The prepare-frontend goal might not be reading the scanner configuration

**Verification steps:**

Enable debug logging to see which artifacts are accepted/rejected:
```bash
mvn clean prepare-frontend -Dvaadin.annotationScanner=ADD_ON -X | grep -i "artifact.*accepted\|artifact.*rejected"
```

Compare URL counts between modes:
```bash
# Run with FULL mode (baseline)
mvn clean prepare-frontend -Dvaadin.annotationScanner=FULL | grep "Reflections took"

# Run with ADD_ON mode (should show fewer URLs)
mvn clean prepare-frontend -Dvaadin.annotationScanner=ADD_ON | grep "Reflections took"
```

Check debug logs for manifest filtering messages:
```bash
mvn clean install -Dvaadin.annotationScanner=ADD_ON -X 2>&1 | grep -E "(has Vaadin-Package-Version|no Vaadin-Package-Version)"
```

### Spring Boot Dev Mode

Manifest filtering is **ACTIVE** in Spring Boot dev mode when using `vaadin.annotation-scanner-mode=addon`.

**Configuration in application.properties:**
```properties
vaadin.annotation-scanner-mode=addon
```

**How it works:**
- Spring's `CustomResourceLoader` checks JAR manifests during classpath scanning
- Only JARs with `Vaadin-Package-Version` manifest attribute are scanned
- Works alongside existing `vaadin.allowed-packages` / `vaadin.blocked-packages` filtering
- Manifest checks are cached for performance

**Expected behavior:**
- Startup log message: "Manifest-based filtering enabled (vaadin.annotation-scanner-mode=addon): only JARs with Vaadin-Package-Version will be scanned"
- Debug logs show: "JAR {name} will not be scanned: no Vaadin-Package-Version manifest"
- Faster dev mode startup with fewer JARs scanned

**Note:** This only affects Spring Boot applications. Servlet container dev mode (without Spring) uses `@HandlesTypes` and is not affected by this property.

### Servlet Container Dev Mode (Non-Spring)

Manifest filtering is **NOT ACTIVE** in servlet container dev mode.

**How it works:**
- Uses `@HandlesTypes` annotation on `DevModeStartupListener`
- Servlet container scans all JARs automatically
- No filtering mechanism available
- All classes matching the annotation types are passed to Vaadin

**Workaround:**
For better performance in servlet container deployments, consider using Spring Boot for development.

## Technical Details

### Manifest Attribute

Vaadin add-ons must include the following in their `META-INF/MANIFEST.MF`:

```
Vaadin-Package-Version: 1
```

This attribute is already required by Vaadin Directory for published add-ons.

### Filtering Logic

```
if (scannerMode == ADD_ON) {
    scan if:
      - JAR has Vaadin-Package-Version manifest, OR
      - Explicitly included via frontendScanner configuration, OR
      - Is the output directory
} else {
    scan everything (except default exclusions)
}
```

### Caching

The manifest checker uses a static `ConcurrentHashMap` to cache results:
- Key: `File` object (jar file)
- Value: `Boolean` (has Vaadin manifest)
- Thread-safe
- Persists across artifact checks in same JVM

### Default Exclusions

These Vaadin artifacts are excluded by default even in FULL mode:
- `com.vaadin.external.gw:*`
- `com.vaadin.servletdetector:*`
- `com.vaadin:open`
- `com.vaadin:license-checker`
- `com.vaadin:vaadin-dev`
- `com.vaadin:vaadin-dev-server`
- `com.vaadin:vaadin-dev-bundle`
- `com.vaadin:copilot`
- `com.vaadin:flow-archive-extractor`
- `com.vaadin:ui-tests`
- `com.vaadin.external:gentyref`
- `com.vaadin.external.atmosphere:atmosphere-runtime`

## Pending Work

### Phase 3: Logging and Guidance (TODO)

**Goal:** Provide helpful feedback to guide users toward ADD_ON mode.

#### Planned Logging

1. **In FULL mode:**
   - INFO: Log if all detected annotations would have been found in ADD_ON mode
     - Message: "Consider using ADD_ON mode for better performance"
   - WARN: Log if some annotations would have been missed in ADD_ON mode
     - List affected JAR files
     - Explain how to fix (add manifest or use explicit includes)

2. **In ADD_ON mode:**
   - INFO: List JARs that were scanned due to manifest
   - DEBUG: List JARs that were excluded due to missing manifest

#### Implementation Plan

1. Track scanned classes in `FrontendDependencies`
2. After scanning, analyze which classes would have been scanned in each mode
3. Generate appropriate log messages based on mode and results
4. Add configuration option to suppress informational logging

### Phase 4: Runtime Validation (FUTURE)

**Goal:** Safety net to catch annotations that weren't detected during scanning.

**Approach:**
- Hook into component attachment lifecycle
- Check if component class has Vaadin annotations
- Cross-reference with scanned classes list
- Log error/warning if annotation was missed

**Complexity:** Requires significant integration work, deferred to future release.

### Phase 5: @Uses Annotation Extension (FUTURE)

**Goal:** Allow explicit declaration of dependencies for multi-module projects.

**Approach:**
- Move `@Uses` annotation to new package (not component-specific)
- Extend scanner to process `@Uses` for all classes (not just components)
- When class with `@Uses` is scanned, recursively scan referenced classes

**Complexity:** Requires careful design for backward compatibility, deferred to future release.

## Migration Path

### Vaadin 24.x (Current)
- Default: `FULL` mode (existing behavior)
- `ADD_ON` mode available as opt-in
- Both modes fully supported

### Vaadin 25.x (Next Major)
- Default: `ADD_ON` mode (new behavior)
- `FULL` mode still available for compatibility
- Enhanced logging to guide migration

### Vaadin 26.x or later
- Remove `FULL` mode entirely
- Only `ADD_ON` mode supported
- Remove configuration option

## For Add-on Developers

### Adding the Manifest Attribute

#### Maven Configuration

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <archive>
            <manifestEntries>
                <Vaadin-Package-Version>1</Vaadin-Package-Version>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```

#### Gradle Configuration

```gradle
jar {
    manifest {
        attributes(
            'Vaadin-Package-Version': '1'
        )
    }
}
```

### Testing

After adding the manifest:

1. Build your add-on JAR
2. Check the manifest:
   ```bash
   jar -xf your-addon.jar META-INF/MANIFEST.MF
   cat META-INF/MANIFEST.MF
   ```
3. Verify `Vaadin-Package-Version: 1` is present

## Troubleshooting

### Components Not Working After Enabling ADD_ON Mode

**Problem:** Components from a JAR are not being detected.

**Solution:**
1. Check if the JAR has `Vaadin-Package-Version` in its manifest
2. If not, either:
   - Add the manifest attribute to the JAR (preferred)
   - Explicitly include the JAR in scanner configuration:
     ```xml
     <frontendScanner>
         <includes>
             <include>
                 <groupId>com.example</groupId>
                 <artifactId>my-addon</artifactId>
             </include>
         </includes>
     </frontendScanner>
     ```

### Checking Which JARs Are Being Scanned

Enable debug logging:
```bash
mvn clean install -Dvaadin.annotationScanner=ADD_ON -X
```

Look for log lines like:
- `Artifact X accepted: has Vaadin-Package-Version`
- `Artifact Y rejected: no Vaadin-Package-Version manifest in ADD_ON mode`

### Viewing List of Visited Classes

To see the complete list of classes that were scanned, enable debug logging with `-X`:

```bash
mvn clean install -Dvaadin.annotationScanner=ADD_ON -X
```

After the "Visited X classes. Took Y ms." message, you'll see:
```
[DEBUG] Visited classes: com.example.MyView, com.example.MyComponent, com.vaadin.flow.component.button.Button, ...
```

The classes are listed in alphabetical order, separated by commas. This helps you verify:
- Which application classes were scanned
- Which Vaadin component classes were included
- Whether unexpected third-party classes are being scanned

## Testing

### Unit Tests

Located in:
- `flow-plugins/flow-plugin-base/src/test/java/com/vaadin/flow/plugin/base/JarManifestCheckerTest.java`

Run with:
```bash
mvn test -pl flow-plugins/flow-plugin-base -Dtest=JarManifestCheckerTest
```

### Integration Testing

To test in a real application:

1. Create a test project with both Vaadin add-ons and regular dependencies
2. Build with FULL mode (default)
3. Build with ADD_ON mode (`-Dvaadin.annotationScanner=ADD_ON`)
4. Verify:
   - Build completes successfully
   - Application functions correctly
   - Build time is significantly reduced in ADD_ON mode

## References

- Original RFC: https://github.com/vaadin/flow/pull/22849
- Issue: Performance regression in scanning jars during production build
- Related: Vaadin Directory add-on manifest requirements

## Files Modified/Created

### Created
- `flow-plugins/flow-plugin-base/src/main/java/com/vaadin/flow/plugin/base/JarManifestChecker.java`
- `flow-plugins/flow-plugin-base/src/test/java/com/vaadin/flow/plugin/base/JarManifestCheckerTest.java`
- `flow-server/src/main/java/com/vaadin/flow/server/scanner/JarManifestChecker.java` (runtime copy for Spring Boot)

### Modified
- `flow-plugins/flow-maven-plugin/src/main/java/com/vaadin/flow/plugin/maven/FrontendScannerConfig.java`
- `flow-plugins/flow-maven-plugin/src/main/java/com/vaadin/flow/plugin/maven/Reflector.java`
- `flow-plugins/flow-maven-plugin/src/main/java/com/vaadin/flow/plugin/maven/FlowModeAbstractMojo.java`
- `vaadin-spring/src/main/java/com/vaadin/flow/spring/VaadinServletContextInitializer.java` (Spring Boot dev mode support)

## Status Summary

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 1 | ✅ Complete | Core manifest filtering infrastructure |
| Phase 2 | ✅ Complete | Maven property configuration |
| Phase 2.5 | ✅ Complete | Spring Boot dev mode support |
| Phase 3 | ⏳ Pending | Logging and user guidance |
| Phase 4 | 📅 Future | Runtime validation |
| Phase 5 | 📅 Future | @Uses annotation extension |

**Last Updated:** 2025-12-04
