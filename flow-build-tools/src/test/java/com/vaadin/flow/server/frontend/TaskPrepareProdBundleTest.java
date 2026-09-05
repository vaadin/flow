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
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskPrepareProdBundleTest {

    private static final String PWA_ICONS_IN_BUNDLE = "webapp/pwa-icons";
    // The layout the vaadin-prod-bundle artifact ships: icons scaled from
    // PwaConfiguration.DEFAULT_ICON, named after the default icon path.
    private static final Set<String> DEFAULT_ICONS_IN_BUNDLE = Set.of(
            PWA_ICONS_IN_BUNDLE + "/icons/icon-16x16.png",
            PWA_ICONS_IN_BUNDLE + "/icons/icon-512x512.png");
    private static final String BUNDLE_ENTRY_POINT = "webapp/VAADIN/build/main.js";

    @TempDir
    File temporaryFolder;

    private Options options;

    @BeforeEach
    void setUp() throws Exception {
        File projectDirectory = new File(temporaryFolder, "my-project");
        Path resourceOutputDirectory = projectDirectory.toPath()
                .resolve(Path.of("target", "classes", "META-INF", "VAADIN"));
        Files.createDirectories(resourceOutputDirectory);

        File bundleJar = new File(temporaryFolder, "vaadin-prod-bundle.jar");
        createDefaultProdBundleJar(bundleJar);

        ClassFinder classFinder = new ClassFinder.DefaultClassFinder(
                new URLClassLoader(new URL[] { bundleJar.toURI().toURL() },
                        null));
        options = new Options(Mockito.mock(Lookup.class), classFinder,
                projectDirectory).withBuildResultFolders(
                        resourceOutputDirectory.resolve("webapp").toFile(),
                        resourceOutputDirectory.toFile());
    }

    @Test
    void execute_defaultIconPath_defaultIconsCopied()
            throws ExecutionFailedException {
        new TaskPrepareProdBundle(options,
                pwaConfiguration(PwaConfiguration.DEFAULT_ICON)).execute();

        assertTrue(copiedFile(BUNDLE_ENTRY_POINT).exists(),
                "Bundle files should have been copied");
        assertEquals(DEFAULT_ICONS_IN_BUNDLE, copiedPwaIcons(),
                "Icons generated from the default icon are the ones served, "
                        + "so they should have been copied");
    }

    @Test
    void execute_customIconPath_defaultIconsNotCopied()
            throws ExecutionFailedException {
        new TaskPrepareProdBundle(options,
                pwaConfiguration("custom/icons/logo.png")).execute();

        assertTrue(copiedFile(BUNDLE_ENTRY_POINT).exists(),
                "Bundle files other than the PWA icons should have been copied");
        assertEquals(Set.of(), copiedPwaIcons(),
                "Icons generated from the default icon are never served with a "
                        + "custom icon path, so they should not have been copied");
    }

    private File copiedFile(String relativePath) {
        return new File(options.getResourceOutputDirectory(), relativePath);
    }

    /**
     * The paths, relative to the bundle root, of every file copied under the
     * PWA icons folder.
     */
    private Set<String> copiedPwaIcons() {
        Path outputDirectory = options.getResourceOutputDirectory().toPath();
        Path pwaIcons = outputDirectory.resolve(PWA_ICONS_IN_BUNDLE);
        if (!Files.isDirectory(pwaIcons)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.walk(pwaIcons)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> outputDirectory.relativize(file).toString()
                            .replace(File.separatorChar, '/'))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static PwaConfiguration pwaConfiguration(String iconPath) {
        return new PwaConfiguration(true, PwaConfiguration.DEFAULT_NAME,
                "Flow PWA", "", PwaConfiguration.DEFAULT_BACKGROUND_COLOR,
                PwaConfiguration.DEFAULT_THEME_COLOR, iconPath,
                PwaConfiguration.DEFAULT_PATH,
                PwaConfiguration.DEFAULT_OFFLINE_PATH,
                PwaConfiguration.DEFAULT_DISPLAY,
                PwaConfiguration.DEFAULT_START_URL, new String[] {}, false);
    }

    private static void createDefaultProdBundleJar(File jar)
            throws IOException {
        try (JarOutputStream out = new JarOutputStream(
                Files.newOutputStream(jar.toPath()))) {
            writeEntry(out, "config/stats.json", "{\"npmModules\":{}}");
            writeEntry(out, BUNDLE_ENTRY_POINT, "console.log('bundle');");
            for (String icon : DEFAULT_ICONS_IN_BUNDLE) {
                writeEntry(out, icon, "default icon");
            }
        }
    }

    private static void writeEntry(JarOutputStream out, String path,
            String content) throws IOException {
        out.putNextEntry(new JarEntry("vaadin-prod-bundle/" + path));
        out.write(content.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
}
