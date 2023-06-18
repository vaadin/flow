/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PlatformTest {
    private ClassLoader oldContextClassLoader;

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Before
    public void rememberContextClassLoader() {
        oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        Platform.hillaVersion = null;
    }

    @After
    public void restoreContextClassLoader() {
        Thread.currentThread().setContextClassLoader(oldContextClassLoader);
    }

    private void fakeHilla(String hillaVersion) throws IOException {
        Platform.hillaVersion = null;
        if (hillaVersion == null) {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
            return;
        }
        final Path hillaJar = temporary.newFolder().toPath();
        final Path pomProperties = hillaJar
                .resolve("META-INF/maven/dev.hilla/hilla/pom.properties");
        Files.createDirectories(pomProperties.getParent());
        Files.writeString(pomProperties, "version=" + hillaVersion);
        final URLClassLoader classLoader = new URLClassLoader(
                new URL[] { hillaJar.toUri().toURL() }, oldContextClassLoader);
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Test
    public void testGetVaadinVersionReturnsEmptyOptionalWhenVaadinNotOnClasspath() {
        assertEquals(Optional.empty(), Platform.getVaadinVersion());
    }

    @Test
    public void testGetHillaVersionReturnsEmptyOptionalWhenHillaNotOnClasspath() {
        assertEquals(Optional.empty(), Platform.getHillaVersion());
    }

    @Test
    public void testGetHillaVersionReturnsProperVersionWhenHillaOnClasspath()
            throws Exception {
        fakeHilla("2.1.0");
        assertEquals(Optional.of("2.1.0"), Platform.getHillaVersion());
        fakeHilla("2.0.6");
        assertEquals(Optional.of("2.0.6"), Platform.getHillaVersion());
    }
}
