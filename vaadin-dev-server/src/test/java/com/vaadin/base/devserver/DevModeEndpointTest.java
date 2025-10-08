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

import jakarta.servlet.ServletRegistration;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.base.devserver.startup.AbstractDevModeTest;
import com.vaadin.base.devserver.startup.DevModeStartupListener;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR;
import static com.vaadin.flow.testutil.FrontendStubs.createStubNode;
import static com.vaadin.flow.testutil.FrontendStubs.createStubViteServer;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@NotThreadSafe
public class DevModeEndpointTest extends AbstractDevModeTest {

    Set<Class<?>> classes;
    DevModeStartupListener devModeStartupListener;

    private static class VaadinServletSubClass extends VaadinServlet {

    }

    @Before
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setup() throws Exception {
        super.setup();
        assertFalse("No DevModeHandler should be available at test start",
                DevModeHandlerManager
                        .getDevModeHandler(
                                new VaadinServletContext(servletContext))
                        .isPresent());

        createStubNode(false, true, baseDir);
        createStubViteServer("ready in 500 ms", 500, baseDir, true);

        // Prevent TaskRunNpmInstall#cleanUp from deleting node_modules
        new File(baseDir, "node_modules/.modules.yaml").createNewFile();

        ServletRegistration vaadinServletRegistration = Mockito
                .mock(ServletRegistration.class);

        Mockito.doReturn(new TestEndpointGeneratorTaskFactory()).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);

        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);

        Mockito.when(vaadinServletRegistration.getClassName())
                .thenReturn(VaadinServletSubClass.class.getName());

        classes = new HashSet<>();
        classes.add(this.getClass());

        Map registry = new HashMap();

        // Adding extra registrations to make sure that
        // DevModeInitializer picks
        // the correct registration which is a VaadinServlet
        // registration.
        registry.put("extra1", Mockito.mock(ServletRegistration.class));
        registry.put("foo", vaadinServletRegistration);
        registry.put("extra2", Mockito.mock(ServletRegistration.class));
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn(registry);
        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(servletContext.getClassLoader())
                .thenReturn(this.getClass().getClassLoader());

        FileUtils.forceMkdir(new File(baseDir, "src/main/java"));

        devModeStartupListener = new DevModeStartupListener();
    }

    @Test
    public void should_generateOpenApi() throws Exception {
        File generatedOpenApiJson = Paths
                .get(baseDir, TARGET, "classes/com/vaadin/hilla/openapi.json")
                .toFile();

        Assert.assertFalse(generatedOpenApiJson.exists());
        try (MockedStatic<FrontendUtils> util = Mockito
                .mockStatic(FrontendUtils.class, Mockito.CALLS_REAL_METHODS)) {
            util.when(() -> FrontendUtils.isHillaUsed(Mockito.any()))
                    .thenReturn(true);
            util.when(() -> FrontendUtils.isHillaUsed(Mockito.any(),
                    Mockito.any())).thenReturn(true);
            devModeStartupListener.onStartup(classes, servletContext);
            handler = getDevModeHandler();
            waitForDevServer();
        }
        Assert.assertTrue("Should generate OpenAPI spec if Endpoint is used.",
                generatedOpenApiJson.exists());
    }

    @Test
    public void should_generateTs_files() throws Exception {
        // Configure a folder that has .java classes with valid
        // endpoints
        // Not using `src/test/java` because there are invalid endpoint
        // names
        // in some tests
        File src = new File(
                getClass().getClassLoader().getResource("java").getFile());
        Mockito.when(appConfig.getStringProperty(
                Mockito.eq(CONNECT_JAVA_SOURCE_FOLDER_TOKEN),
                Mockito.anyString())).thenReturn(src.getAbsolutePath());

        File ts1 = new File(baseDir,
                DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "MyEndpoint.ts");
        File ts2 = new File(baseDir, DEFAULT_PROJECT_FRONTEND_GENERATED_DIR
                + "connect-client.default.ts");

        assertFalse(ts1.exists());
        assertFalse(ts2.exists());
        try (MockedStatic<FrontendUtils> util = Mockito
                .mockStatic(FrontendUtils.class, Mockito.CALLS_REAL_METHODS)) {
            util.when(() -> FrontendUtils.isHillaUsed(Mockito.any()))
                    .thenReturn(true);
            util.when(() -> FrontendUtils.isHillaUsed(Mockito.any(),
                    Mockito.any())).thenReturn(true);
            devModeStartupListener.onStartup(classes, servletContext);
            handler = getDevModeHandler();
            waitForDevServer();
        }
        assertTrue(ts1.exists());
        assertTrue(ts2.exists());
    }

}
