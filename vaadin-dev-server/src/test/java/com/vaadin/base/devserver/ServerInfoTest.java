package com.vaadin.base.devserver;

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

import static org.junit.Assert.*;

public class ServerInfoTest {
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

    private void fakeVaadin() throws IOException {
        final Path vaadinJar = temporary.newFolder().toPath();
        final Path pomProperties = vaadinJar.resolve(
                "META-INF/maven/com.vaadin/vaadin-core/pom.properties");
        Files.createDirectories(pomProperties.getParent());
        Files.writeString(pomProperties, "version=24.1.0");
        final URLClassLoader classLoader = new URLClassLoader(
                new URL[] { vaadinJar.toUri().toURL() }, null);
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Test
    public void testGetProductNameWithNoProducts() {
        ServerInfo serverInfo = new ServerInfo();
        assertEquals("", serverInfo.getProductName());
    }

    @Test
    public void testGetProductNameWithVaadinOnClasspath() throws Exception {
        fakeVaadin();
        ServerInfo serverInfo = new ServerInfo();
        assertEquals("Vaadin", serverInfo.getProductName());
    }

    @Test
    public void hillaVersionIsDashWhenNoHillaOnClasspath() {
        final ServerInfo serverInfo = new ServerInfo();
        assertEquals("-", serverInfo.getHillaVersion());
    }
}
