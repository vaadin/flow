/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow.server.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import jakarta.servlet.ServletContext;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.internal.ClientRoutesProvider;
import com.vaadin.flow.server.MockServletContext;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

import static com.vaadin.flow.server.menu.MenuRegistry.FILE_ROUTES_JSON_NAME;

@NotThreadSafe
public class MenuRegistryTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private ApplicationRouteRegistry registry;
    @Mock
    private MockService vaadinService;
    private VaadinSession session;
    private ServletContext servletContext;
    private VaadinServletContext vaadinContext;
    @Mock
    private DeploymentConfiguration deploymentConfiguration;
    @Mock
    private ClientRoutesProvider provider;
    @Mock
    private VaadinRequest request;

    private AutoCloseable closeable;

    @Before
    public void init() {
        closeable = MockitoAnnotations.openMocks(this);
        servletContext = new MockServletContext();
        vaadinContext = new MockVaadinContext(servletContext);
        Lookup lookup = vaadinContext.getAttribute(Lookup.class);

        Mockito.when(lookup.lookupAll(ClientRoutesProvider.class))
                .thenReturn(Collections.singleton(provider));

        Mockito.when(provider.getClientRoutes())
                .thenReturn(Arrays.asList("", "about", "hilla", "login"));

        registry = ApplicationRouteRegistry.getInstance(vaadinContext);

        Mockito.when(vaadinService.getRouteRegistry()).thenReturn(registry);
        Mockito.when(vaadinService.getContext()).thenReturn(vaadinContext);
        Mockito.when(vaadinService.getInstantiator())
                .thenReturn(new DefaultInstantiator(vaadinService));

        Mockito.when(vaadinService.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);

        Mockito.when(deploymentConfiguration.getFrontendFolder())
                .thenReturn(tmpDir.getRoot());

        VaadinService.setCurrent(vaadinService);

        session = new MockVaadinSession(vaadinService) {
            @Override
            public VaadinService getService() {
                return vaadinService;
            }
        };

        VaadinSession.setCurrent(session);
    }

    @After
    public void cleanup() throws Exception {
        closeable.close();
        CurrentInstance.clearAll();
    }

    @Test
    public void getMenuItemsContainsExpectedClientPaths() throws IOException {
        File generated = tmpDir.newFolder("generated");
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        Map<String, ViewInfo> menuItems = new MenuRegistry().getMenuItems();

        Assert.assertEquals(4, menuItems.size());
    }

    @Test
    public void getMenuItemsContainsExpectedServerPaths() {
        Mockito.when(request.getService()).thenReturn(vaadinService);
        CurrentInstance.set(VaadinRequest.class, request);
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(MyRoute.class, MyInfo.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        Map<String, ViewInfo> menuItems = new MenuRegistry().getMenuItems();

        Assert.assertEquals(2, menuItems.size());
    }

    @Test
    public void getMenuItemsContainBothClientAndServerPaths()
            throws IOException {
        File generated = tmpDir.newFolder("generated");
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        Mockito.when(request.getService()).thenReturn(vaadinService);
        CurrentInstance.set(VaadinRequest.class, request);
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(MyRoute.class, MyInfo.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        Map<String, ViewInfo> menuItems = new MenuRegistry().getMenuItems();

        Assert.assertEquals(6, menuItems.size());

    }

    @Tag("div")
    @Route("home")
    @Menu(title = "Home")
    private static class MyRoute extends Component {
    }

    @Tag("div")
    @Route("info")
    @Menu
    private static class MyInfo extends Component {
    }

    /**
     * Extending class to let us mock the getRouteRegistry method for testing.
     */
    private static class MockService extends VaadinServletService {

        @Override
        public RouteRegistry getRouteRegistry() {
            return super.getRouteRegistry();
        }

        @Override
        public Instantiator getInstantiator() {
            return new DefaultInstantiator(this);
        }
    }

    String testClientRouteFile = """
            [
              {
                "route": "",
                "params": {},
                "title": "Layout",
                "children": [
                  {
                    "route": "",
                    "params": {},
                    "title": "Public"
                  },
                  {
                    "route": "about",
                    "loginRequired": true,
                    "params": {},
                    "title": "About"
                  },
                  {
                    "route": "hilla",
                    "loginRequired": true,
                    "rolesAllowed": [
                      "ROLE_USER"
                    ],
                    "params": {},
                    "title": "Hilla"
                  },
                  {
                    "route": "login",
                    "menu": {
                      "exclude": true
                    },
                    "params": {},
                    "title": "Login"
                  }
                ]
              }
            ]
            """;
}