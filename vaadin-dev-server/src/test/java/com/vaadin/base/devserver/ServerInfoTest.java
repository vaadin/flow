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
package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.hilla.EndpointRequestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ServerInfoTest {
    private ClassLoader oldContextClassLoader;

    @TempDir
    File temporary;

    private MockedStatic<EndpointRequestUtil> endpointRequestUtilMockedStatic;

    @BeforeEach
    void rememberContextClassLoader() throws Exception {
        oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        fakePlatform(false, false);
    }

    @AfterEach
    void restoreContextClassLoader() {
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
            final Path vaadinJar = Files.createTempDirectory(temporary.toPath(),
                    "tmp");
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
    void hillaVersionIsDashWhenNoHillaOnClasspath() {
        assertEquals("-", ServerInfo.fetchHillaVersion());
    }

    @Test
    void vaadinVersionIsDashWhenNoVaadinOnClasspath() {
        assertEquals("-", ServerInfo.fetchVaadinVersion());
    }

    @Test
    void fetchContainerInfoDoesNotThrow() {
        ServerInfo.ContainerInfo result = ServerInfo.fetchContainerInfo();
        assertNotNull(result, "Expected non-null enum value");
    }

    @Test
    void fetchContainerInfoDetectsContainer() {
        ServerInfo.ContainerInfo result = ServerInfo.fetchContainerInfo();
        // If any known container indicator exists, detection should find it
        if (Files.exists(Path.of("/.dockerenv"))
                || Files.exists(Path.of("/run/.containerenv"))
                || System.getenv("KUBERNETES_SERVICE_HOST") != null
                || System.getenv("container") != null || Files.exists(Path.of(
                        "/sys/firmware/devicetree/base/hypervisor/compatible"))) {
            assertNotEquals(ServerInfo.ContainerInfo.NONE, result,
                    "Should detect container runtime, not NONE");
        } else {
            assertEquals(ServerInfo.ContainerInfo.NONE, result);
        }
    }
}
