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
