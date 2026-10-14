/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.hilla.EndpointRequestUtil;

import static org.junit.Assert.assertEquals;

public class ServerInfoTest {
    private ClassLoader oldContextClassLoader;

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private MockedStatic<EndpointRequestUtil> endpointRequestUtilMockedStatic;

    @Before
    public void rememberContextClassLoader() throws Exception {
        oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        fakePlatform(false, false);
    }

    @After
    public void restoreContextClassLoader() {
        Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        if (endpointRequestUtilMockedStatic != null) {
            endpointRequestUtilMockedStatic.close();
            endpointRequestUtilMockedStatic = null;
        }
    }

    private void fakePlatform(boolean vaadin, boolean hilla)
            throws IOException {
        if (endpointRequestUtilMockedStatic != null) {
            endpointRequestUtilMockedStatic.close();
            endpointRequestUtilMockedStatic = null;
        }

        final LinkedList<URL> classpath = new LinkedList<>();
        if (vaadin) {
            final Path vaadinJar = temporary.newFolder().toPath();
            final Path pomProperties = vaadinJar.resolve(
                    "META-INF/maven/com.vaadin/vaadin-core/pom.properties");
            Files.createDirectories(pomProperties.getParent());
            Files.writeString(pomProperties, "version=24.1.0");
            classpath.add(vaadinJar.toUri().toURL());
        }
        final ClassLoader classLoader = new URLClassLoader(
                classpath.toArray(new URL[0]), null);
        if (hilla) {
            endpointRequestUtilMockedStatic = Mockito
                    .mockStatic(EndpointRequestUtil.class);
            endpointRequestUtilMockedStatic
                    .when(EndpointRequestUtil::isHillaAvailable)
                    .thenReturn(true);
        }
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Test
    public void hillaVersionIsDashWhenNoHillaOnClasspath() {
        assertEquals("-", ServerInfo.fetchHillaVersion());
    }

    @Test
    public void vaadinVersionIsDashWhenNoVaadinOnClasspath() {
        assertEquals("-", ServerInfo.fetchVaadinVersion());
    }
}
