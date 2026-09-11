/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.DEV_BUNDLE_JAR_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleBuildUtilsTest {

    @TempDir
    File temporaryFolder;

    private static final String CUSTOM_REGISTRY_URL = "https://nexus.example.com/repository/npm/";

    private static FrontendTools mockTools(boolean customRegistry) {
        return mockTools(
                customRegistry ? Set.of(CUSTOM_REGISTRY_URL) : Set.of());
    }

    private static FrontendTools mockTools(Set<String> customRegistries) {
        FrontendTools tools = Mockito.mock(FrontendTools.class);
        Mockito.when(tools.getCustomNpmRegistries(Mockito.any()))
                .thenReturn(customRegistries);
        return tools;
    }

    @Test
    void packageLockExists_nothingIsCopied() throws IOException {
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Mockito.anyString())).thenReturn(null);
        Options options = new Options(Mockito.mock(Lookup.class), finder,
                temporaryFolder).withBuildDirectory("target");

        File packageLockFile = new File(temporaryFolder,
                Constants.PACKAGE_LOCK_JSON);
        packageLockFile.createNewFile();
        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_JSON);

        final String existingLockFile = "{ \"existing\" }";
        FileUtils.write(packageLockFile, existingLockFile);

        FileUtils.write(devPackageLockJson, "{ \"bundleFile\"}");

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(false));

        final String packageLockContents = FileUtils
                .readFileToString(packageLockFile, StandardCharsets.UTF_8);

        assertEquals(existingLockFile, packageLockContents,
                "Existing file should not be overwritten");
    }

    @Test
    void noPackageLockExists_devBundleLockIsCopied_notJarLock()
            throws IOException {
        Options options = new MockOptions(temporaryFolder)
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());

        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_JSON);

        final String packageLockContent = "{ \"bundleFile\"}";
        FileUtils.write(devPackageLockJson, packageLockContent);

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(false));

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        assertEquals(packageLockContent, packageLockContents,
                "dev-bundle file should be used");
    }

    @Test
    void noPackageLockExists_jarDevBundleLockIsCopied()
            throws IOException, ClassNotFoundException {
        Options options = new MockOptions(temporaryFolder)
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        File jarHybridPackageLock = new File(options.getNpmFolder(),
                "hybrid-temp.json");
        final String jarHybridPackageLockContent = "{ \"hybridJarData\"}";
        FileUtils.write(jarHybridPackageLock, jarHybridPackageLockContent);

        Mockito.doThrow(new ClassNotFoundException("No Hilla"))
                .when(options.getClassFinder())
                .loadClass("com.vaadin.hilla.EndpointController");
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());
        Mockito.when(options.getClassFinder().getResource(
                DEV_BUNDLE_JAR_PATH + "hybrid-" + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarHybridPackageLock.toURI().toURL());

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(false));

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        assertEquals(jarPackageLockContent, packageLockContents,
                "File should be gotten from jar on classpath");
    }

    @Test
    void noPackageLockExists_hillaUsed_jarHybridDevBundleLockIsCopied()
            throws IOException, ClassNotFoundException {
        Options options = new MockOptions(temporaryFolder)
                .withBuildDirectory("target");

        Path dummyView = options.getFrontendDirectory().toPath()
                .resolve(Path.of("views", "dummy.tsx"));
        Files.createDirectories(dummyView.getParent());
        Files.writeString(dummyView, "const x = 1;");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        File jarHybridPackageLock = new File(options.getNpmFolder(),
                "hybrid-temp.json");
        final String jarHybridPackageLockContent = "{ \"hybridJarData\"}";
        FileUtils.write(jarHybridPackageLock, jarHybridPackageLockContent);

        Mockito.when(options.getClassFinder()
                .loadClass("com.vaadin.hilla.EndpointController"))
                .thenReturn(Object.class);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());
        Mockito.when(options.getClassFinder().getResource(
                DEV_BUNDLE_JAR_PATH + "hybrid-" + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarHybridPackageLock.toURI().toURL());

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(false));

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        assertEquals(jarHybridPackageLockContent, packageLockContents,
                "File should be gotten from jar on classpath");
    }

    @Test
    void noPackageLockExists_hillaUsed_hybridPackageLockNotPresentInJar_jarDevBundleIsCopied()
            throws IOException, ClassNotFoundException {
        Options options = new MockOptions(temporaryFolder)
                .withBuildDirectory("target");

        Path dummyView = options.getFrontendDirectory().toPath()
                .resolve(Path.of("views", "dummy.tsx"));
        Files.createDirectories(dummyView.getParent());
        Files.writeString(dummyView, "const x = 1;");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(options.getClassFinder()
                .loadClass("com.vaadin.hilla.EndpointController"))
                .thenReturn(Object.class);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());
        Mockito.when(options.getClassFinder().getResource(
                DEV_BUNDLE_JAR_PATH + "hybrid-" + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(null);

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(false));

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        assertEquals(jarPackageLockContent, packageLockContents,
                "File should be gotten from jar on classpath");
    }

    @Test
    void pnpm_noPackageLockExists_devBundleLockYamlIsCopied_notJarLockOrJson()
            throws IOException {
        Options options = new MockOptions(temporaryFolder)
                .withBuildDirectory("target").withEnablePnpm(true);

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        final String jarPackageLockContent = "{ \"jarData\"}";
        FileUtils.write(jarPackageLock, jarPackageLockContent);

        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_YAML))
                .thenReturn(jarPackageLock.toURI().toURL());

        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_JSON);
        File devPackageLock = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_YAML);

        final String packageLockContent = "{ \"bundleFile\"}";
        FileUtils.write(devPackageLock, packageLockContent);
        FileUtils.write(devPackageLockJson, "{ \"json\"}");

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(false));

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_YAML),
                StandardCharsets.UTF_8);

        assertEquals(packageLockContent, packageLockContents,
                "dev-bundle file should be used");
    }

    private static final String LOCK_WITH_NPMJS_URLS = """
            {
              "name": "test",
              "lockfileVersion": 3,
              "requires": true,
              "packages": {
                "": { "name": "test" },
                "node_modules/foo": {
                  "version": "1.0.0",
                  "resolved": "https://registry.npmjs.org/foo/-/foo-1.0.0.tgz",
                  "integrity": "sha512-fooHash"
                },
                "node_modules/bar": {
                  "version": "2.3.4",
                  "resolved": "https://registry.npmjs.org/bar/-/bar-2.3.4.tgz",
                  "integrity": "sha512-barHash"
                }
              }
            }
            """;

    @Test
    void customRegistryConfigured_resolvedStrippedIntegrityKept()
            throws IOException {
        Options options = new MockOptions(temporaryFolder)
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        FileUtils.write(jarPackageLock, LOCK_WITH_NPMJS_URLS,
                StandardCharsets.UTF_8);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(true));

        final String result = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        assertFalse(result.contains("registry.npmjs.org"),
                "resolved URLs pointing to npmjs.org should be stripped when a custom registry is configured");
        assertFalse(result.contains("\"resolved\""),
                "resolved fields should be removed so npm re-resolves against the configured registry");
        assertTrue(
                result.contains("sha512-fooHash")
                        && result.contains("sha512-barHash"),
                "integrity fields should be preserved");
        assertTrue(result.contains("\"version\": \"1.0.0\""),
                "version pins should be preserved");
    }

    private static final String LOCK_WITH_CUSTOM_REGISTRY_URLS = """
            {
              "name": "test",
              "lockfileVersion": 3,
              "requires": true,
              "packages": {
                "": { "name": "test" },
                "node_modules/foo": {
                  "version": "1.0.0",
                  "resolved": "https://nexus.example.com/repository/npm/foo/-/foo-1.0.0.tgz",
                  "integrity": "sha512-fooHash"
                },
                "node_modules/bar": {
                  "version": "2.3.4",
                  "resolved": "https://registry.npmjs.org/bar/-/bar-2.3.4.tgz",
                  "integrity": "sha512-barHash"
                }
              }
            }
            """;

    @Test
    void customRegistryConfigured_resolvedFromCustomRegistryKept()
            throws IOException {
        Options options = new MockOptions(temporaryFolder)
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        FileUtils.write(jarPackageLock, LOCK_WITH_CUSTOM_REGISTRY_URLS,
                StandardCharsets.UTF_8);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(true));

        final String result = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        assertTrue(result.contains(
                "https://nexus.example.com/repository/npm/foo/-/foo-1.0.0.tgz"),
                "resolved URLs already pointing at the custom registry should be kept so npm does not needlessly re-resolve them");
        assertFalse(result.contains("registry.npmjs.org"),
                "resolved URLs pointing to npmjs.org should still be stripped");
    }

    private static final String LOCK_WITH_MULTIPLE_REGISTRY_URLS = """
            {
              "name": "test",
              "lockfileVersion": 3,
              "requires": true,
              "packages": {
                "": { "name": "test" },
                "node_modules/a": {
                  "resolved": "https://registry-a.example.com/npm/a/-/a-1.0.0.tgz",
                  "integrity": "sha512-aHash"
                },
                "node_modules/b": {
                  "resolved": "https://registry-b.example.com/npm/b/-/b-1.0.0.tgz",
                  "integrity": "sha512-bHash"
                },
                "node_modules/c": {
                  "resolved": "https://registry-c.example.com/npm/c/-/c-1.0.0.tgz",
                  "integrity": "sha512-cHash"
                },
                "node_modules/d": {
                  "resolved": "https://registry-d.example.com/npm/d/-/d-1.0.0.tgz",
                  "integrity": "sha512-dHash"
                },
                "node_modules/e": {
                  "resolved": "https://registry.npmjs.org/e/-/e-1.0.0.tgz",
                  "integrity": "sha512-eHash"
                }
              }
            }
            """;

    @Test
    void multipleCustomRegistries_onlyConfiguredRegistriesKept()
            throws IOException {
        Options options = new MockOptions(temporaryFolder)
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        FileUtils.write(jarPackageLock, LOCK_WITH_MULTIPLE_REGISTRY_URLS,
                StandardCharsets.UTF_8);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());

        // Only registries A and B are configured; C, D and the default npm
        // registry are not.
        BundleBuildUtils.copyPackageLockFromBundle(options,
                mockTools(Set.of("https://registry-a.example.com/npm/",
                        "https://registry-b.example.com/npm/")));

        final String result = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        assertTrue(result.contains("registry-a.example.com"),
                "resolved URLs from configured registry A should be kept");
        assertTrue(result.contains("registry-b.example.com"),
                "resolved URLs from configured registry B should be kept");
        assertFalse(result.contains("registry-c.example.com"),
                "resolved URLs from unconfigured registry C should be stripped");
        assertFalse(result.contains("registry-d.example.com"),
                "resolved URLs from unconfigured registry D should be stripped");
        assertFalse(result.contains("registry.npmjs.org"),
                "resolved URLs from the default npm registry should be stripped");
    }

    @Test
    void noCustomRegistry_resolvedKeptVerbatim() throws IOException {
        Options options = new MockOptions(temporaryFolder)
                .withBuildDirectory("target");

        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        FileUtils.write(jarPackageLock, LOCK_WITH_NPMJS_URLS,
                StandardCharsets.UTF_8);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(false));

        final String result = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        assertEquals(LOCK_WITH_NPMJS_URLS, result,
                "Without a custom registry the lock should be copied verbatim");
    }

    @Test
    void customRegistry_invalidJson_copiedVerbatim() throws IOException {
        Options options = new MockOptions(temporaryFolder)
                .withBuildDirectory("target");

        final String notJson = "{ not valid json";
        File jarPackageLock = new File(options.getNpmFolder(), "temp.json");
        FileUtils.write(jarPackageLock, notJson, StandardCharsets.UTF_8);
        Mockito.when(options.getClassFinder()
                .getResource(DEV_BUNDLE_JAR_PATH + Constants.PACKAGE_LOCK_JSON))
                .thenReturn(jarPackageLock.toURI().toURL());

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(true));

        final String result = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_JSON),
                StandardCharsets.UTF_8);

        assertEquals(notJson, result,
                "Unparseable content should be copied verbatim without failing the build");
    }

    @Test
    void pnpm_packageLockExists_nothingIsCopied() throws IOException {
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Mockito.anyString())).thenReturn(null);
        Options options = new Options(Mockito.mock(Lookup.class), finder,
                temporaryFolder).withBuildDirectory("target")
                .withEnablePnpm(true);

        File packageLockFile = new File(temporaryFolder,
                Constants.PACKAGE_LOCK_YAML);
        packageLockFile.createNewFile();
        File devBundleFolder = new File(
                new File(options.getNpmFolder(),
                        options.getBuildDirectoryName()),
                Constants.DEV_BUNDLE_LOCATION);
        devBundleFolder.mkdirs();
        File devPackageLockJson = new File(devBundleFolder,
                Constants.PACKAGE_LOCK_YAML);

        final String existingLockFile = "{ \"existing\" }";
        FileUtils.write(packageLockFile, existingLockFile);

        FileUtils.write(devPackageLockJson, "{ \"bundleFile\"}");

        BundleBuildUtils.copyPackageLockFromBundle(options, mockTools(false));

        final String packageLockContents = FileUtils
                .readFileToString(packageLockFile, StandardCharsets.UTF_8);

        assertEquals(existingLockFile, packageLockContents,
                "Existing file should not be overwritten");
    }

}
