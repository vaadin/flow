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

class BundleBuildUtilsTest {

    @TempDir
    File temporaryFolder;

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

        BundleBuildUtils.copyPackageLockFromBundle(options);

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

        BundleBuildUtils.copyPackageLockFromBundle(options);

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

        BundleBuildUtils.copyPackageLockFromBundle(options);

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

        BundleBuildUtils.copyPackageLockFromBundle(options);

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

        BundleBuildUtils.copyPackageLockFromBundle(options);

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

        BundleBuildUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils.readFileToString(
                new File(options.getNpmFolder(), Constants.PACKAGE_LOCK_YAML),
                StandardCharsets.UTF_8);

        assertEquals(packageLockContent, packageLockContents,
                "dev-bundle file should be used");
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

        BundleBuildUtils.copyPackageLockFromBundle(options);

        final String packageLockContents = FileUtils
                .readFileToString(packageLockFile, StandardCharsets.UTF_8);

        assertEquals(existingLockFile, packageLockContents,
                "Existing file should not be overwritten");
    }

}
