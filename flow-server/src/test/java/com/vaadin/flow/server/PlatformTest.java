/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class PlatformTest {
    private ClassLoader oldContextClassLoader;

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Before
    public void rememberContextClassLoader() {
        oldContextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @After
    public void restoreContextClassLoader() {
        Thread.currentThread().setContextClassLoader(oldContextClassLoader);
    }

    @Before
    @After
    public void cleanMemoizedValues() {
        Platform.hillaVersion = null;
        Platform.vaadinVersion = null;
    }

    private void fakeVaadinHilla(String vaadinVersion, String hillaVersion)
            throws IOException {
        Platform.hillaVersion = null;
        Platform.vaadinVersion = null;

        final List<URL> classPath = new LinkedList<>();

        if (hillaVersion != null) {
            final Path hillaJar = temporary.newFolder().toPath();
            final Path pomProperties = hillaJar
                    .resolve(Platform.HILLA_POM_PROPERTIES);
            Files.createDirectories(pomProperties.getParent());
            Files.writeString(pomProperties, "version=" + hillaVersion);
            classPath.add(hillaJar.toUri().toURL());
        }

        if (vaadinVersion != null) {
            final Path vaadinJar = temporary.newFolder().toPath();
            final Path pomProperties = vaadinJar.resolve(
                    "META-INF/maven/com.vaadin/vaadin-core/pom.properties");
            Files.createDirectories(pomProperties.getParent());
            Files.writeString(pomProperties, "version=" + vaadinVersion);
            classPath.add(vaadinJar.toUri().toURL());
        }

        if (classPath.isEmpty()) {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        } else {
            final URLClassLoader classLoader = new URLClassLoader(
                    classPath.toArray(new URL[0]), null);
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    @Test
    public void testGetVaadinVersionReturnsEmptyOptionalWhenVaadinNotOnClasspath() {
        assertEquals(Optional.empty(), Platform.getVaadinVersion());
    }

    @Test
    public void testGetVaadinVersionReturnsProperVersionWhenVaadinOnClasspath()
            throws Exception {
        fakeVaadinHilla("24.1.0", null);
        assertEquals(Optional.of("24.1.0"), Platform.getVaadinVersion());
        assertEquals(Optional.empty(), Platform.getHillaVersion());
        fakeVaadinHilla("24.1.1", null);
        assertEquals(Optional.of("24.1.1"), Platform.getVaadinVersion());
        assertEquals(Optional.empty(), Platform.getHillaVersion());
    }

    @Test
    public void testGetHillaVersionReturnsEmptyOptionalWhenHillaNotOnClasspath() {
        assertEquals(Optional.empty(), Platform.getHillaVersion());
    }

    @Test
    public void testGetHillaVersionReturnsProperVersionWhenHillaOnClasspath()
            throws Exception {
        fakeVaadinHilla(null, "2.1.0");
        assertEquals(Optional.of("2.1.0"), Platform.getHillaVersion());
        assertEquals(Optional.empty(), Platform.getVaadinVersion());
        fakeVaadinHilla(null, "2.0.6");
        assertEquals(Optional.of("2.0.6"), Platform.getHillaVersion());
        assertEquals(Optional.empty(), Platform.getVaadinVersion());
    }

    @Test
    public void testGetVaadinHillaVersionReturnsProperVersionWhenBothVaadinAndHillaOnClasspath()
            throws Exception {
        fakeVaadinHilla("24.0.0", "2.1.0");
        assertEquals(Optional.of("2.1.0"), Platform.getHillaVersion());
        assertEquals(Optional.of("24.0.0"), Platform.getVaadinVersion());
        fakeVaadinHilla("24.1.1", "2.0.6");
        assertEquals(Optional.of("2.0.6"), Platform.getHillaVersion());
        assertEquals(Optional.of("24.1.1"), Platform.getVaadinVersion());
    }
}
