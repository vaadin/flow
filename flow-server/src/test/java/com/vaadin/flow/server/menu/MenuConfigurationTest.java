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

import jakarta.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.MockServletContext;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

import static com.vaadin.flow.server.frontend.FrontendUtils.GENERATED;
import static com.vaadin.flow.internal.menu.MenuRegistry.FILE_ROUTES_JSON_NAME;

@NotThreadSafe
public class MenuConfigurationTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private ApplicationRouteRegistry registry;
    @Mock
    private MenuRegistryTest.MockService vaadinService;
    private VaadinSession session;
    private ServletContext servletContext;
    private VaadinServletContext vaadinContext;
    @Mock
    private DeploymentConfiguration deploymentConfiguration;
    @Mock
    private VaadinRequest request;

    private AutoCloseable closeable;

    @Before
    public void init() {
        closeable = MockitoAnnotations.openMocks(this);
        servletContext = new MockServletContext();
        vaadinContext = new MockVaadinContext(servletContext);

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

        Mockito.when(request.getService()).thenReturn(vaadinService);
        CurrentInstance.set(VaadinRequest.class, request);
    }

    @After
    public void cleanup() throws Exception {
        closeable.close();
        CurrentInstance.clearAll();
    }

    @Test
    public void testWithLoggedInUser_userHasRoles() throws IOException {
        Mockito.when(request.getUserPrincipal())
                .thenReturn(Mockito.mock(Principal.class));
        Mockito.when(request.isUserInRole(Mockito.anyString()))
                .thenReturn(true);

        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(),
                MenuRegistryTest.testClientRouteFile);

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        Assert.assertEquals(
                "List of menu items has incorrect size. Excluded menu item like /login is not expected.",
                4, menuEntries.size());
        assertOrder(menuEntries,
                new String[] { "", "/about", "/hilla", "/hilla/sub" });
    }

    @Test
    public void getMenuItemsList_returnsCorrectPaths() throws IOException {
        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(),
                MenuRegistryTest.testClientRouteFile);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(MenuRegistryTest.MyRoute.class,
                MenuRegistryTest.MyInfo.class,
                MenuRegistryTest.MyRequiredParamRoute.class,
                MenuRegistryTest.MyRequiredAndOptionalParamRoute.class,
                MenuRegistryTest.MyOptionalParamRoute.class,
                MenuRegistryTest.MyVarargsParamRoute.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        Assert.assertEquals(5, menuEntries.size());
        assertOrder(menuEntries, new String[] { "", "/home", "/info", "/param",
                "/param/varargs" });

        Map<String, MenuEntry> mapMenuItems = menuEntries.stream()
                .collect(Collectors.toMap(MenuEntry::path, item -> item));
        assertClientRoutes(mapMenuItems, false, false);
        assertServerRoutes(mapMenuItems);
        assertServerRoutesWithParameters(mapMenuItems, true);
    }

    @Test
    public void getMenuItemsList_assertOrder() {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(MenuRegistryTest.TestRouteA.class,
                MenuRegistryTest.TestRouteB.class,
                MenuRegistryTest.TestRouteC.class,
                MenuRegistryTest.TestRouteD.class,
                MenuRegistryTest.TestRouteDA.class,
                MenuRegistryTest.TestRouteDB.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        ;
        Assert.assertEquals(4, menuEntries.size());
        assertOrder(menuEntries,
                new String[] { "/d", "/c", "/a", "/b", "/d/a", "/d/b" });
    }

    private void assertOrder(List<MenuEntry> menuEntries,
            String[] expectedOrder) {
        for (int i = 0; i < menuEntries.size(); i++) {
            Assert.assertEquals(expectedOrder[i], menuEntries.get(i).path());
        }
    }

    private void assertClientRoutes(Map<String, MenuEntry> menuOptions,
            boolean authenticated, boolean hasRole) {
        Assert.assertTrue("Client route '' missing",
                menuOptions.containsKey(""));
        Assert.assertEquals("Public", menuOptions.get("").title());

        if (authenticated) {
            Assert.assertTrue("Client route 'about' missing",
                    menuOptions.containsKey("/about"));
            Assert.assertEquals("About", menuOptions.get("/about").title());

            if (hasRole) {
                Assert.assertTrue("Client route 'hilla' missing",
                        menuOptions.containsKey("/hilla"));
                Assert.assertEquals("Hilla", menuOptions.get("/hilla").title());

                Assert.assertTrue("Client child route 'hilla/sub' missing",
                        menuOptions.containsKey("/hilla/sub"));
                Assert.assertEquals("Hilla Sub",
                        menuOptions.get("/hilla/sub").title());
            } else {
                Assert.assertFalse(
                        "Roles do not match no hilla should be available",
                        menuOptions.containsKey("/hilla"));
            }
        } else {
            Assert.assertFalse(
                    "Not authenticated about view should not be available",
                    menuOptions.containsKey("/about"));
            Assert.assertFalse(
                    "Not authenticated hilla view should not be available",
                    menuOptions.containsKey("/hilla"));
        }

        Assert.assertFalse("Client route 'login' should be excluded",
                menuOptions.containsKey("/login"));
    }

    private void assertServerRoutes(Map<String, MenuEntry> menuItems) {
        Assert.assertTrue("Server route 'home' missing",
                menuItems.containsKey("/home"));
        Assert.assertEquals("Home", menuItems.get("/home").title());
        Assert.assertEquals(MenuRegistryTest.MyRoute.class,
                menuItems.get("/home").menuClass());

        Assert.assertTrue("Server route 'info' missing",
                menuItems.containsKey("/info"));
        Assert.assertEquals("MyInfo", menuItems.get("/info").title());
        Assert.assertEquals(MenuRegistryTest.MyInfo.class,
                menuItems.get("/info").menuClass());
    }

    private void assertServerRoutesWithParameters(
            Map<String, MenuEntry> menuItems, boolean excludeExpected) {
        if (excludeExpected) {
            Assert.assertFalse(
                    "Server route '/param/:param' should be excluded",
                    menuItems.containsKey("/param/:param"));
            Assert.assertFalse(
                    "Server route '/param/:param1' should be excluded",
                    menuItems.containsKey("/param/:param1"));
        }

        Assert.assertTrue(
                "Server route with optional parameters '/param' missing",
                menuItems.containsKey("/param"));

        Assert.assertTrue(
                "Server route with optional parameters '/param/varargs' missing",
                menuItems.containsKey("/param/varargs"));
    }
}
