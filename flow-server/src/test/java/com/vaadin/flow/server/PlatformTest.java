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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    private void fakeHilla(String hillaVersion) throws IOException {
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
    public void testGetVaadinVersionReturnsNullWhenVaadinNotOnClasspath() {
        assertNull(Platform.getVaadinVersion().orElse(null));
    }

    @Test
    public void testGetHillaVersionReturnsNullWhenHillaNotOnClasspath() {
        assertNull(Platform.getHillaVersion().orElse(null));
    }

    @Test
    public void testGetHillaVersionReturnsVersionWhenHillaOnClasspath()
            throws Exception {
        fakeHilla("2.1.0");
        assertEquals("2.1.0", Platform.getHillaVersion().orElse(null));
        fakeHilla("2.0.6");
        assertEquals("2.0.6", Platform.getHillaVersion().orElse(null));
    }
}
