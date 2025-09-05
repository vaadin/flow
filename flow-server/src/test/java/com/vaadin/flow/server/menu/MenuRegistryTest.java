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

package com.vaadin.flow.server.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.servlet.ServletContext;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.MockServletContext;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

import static com.vaadin.flow.server.frontend.FrontendUtils.GENERATED;
import static com.vaadin.flow.internal.menu.MenuRegistry.FILE_ROUTES_JSON_NAME;
import static com.vaadin.flow.internal.menu.MenuRegistry.FILE_ROUTES_JSON_PROD_PATH;

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
    private VaadinRequest request;

    private AutoCloseable closeable;

    private MockedStatic<FrontendUtils> frontendUtils;

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
        Mockito.when(deploymentConfiguration.getProjectFolder())
                .thenReturn(tmpDir.getRoot());
        Mockito.when(deploymentConfiguration.getBuildFolder())
                .thenReturn("build");

        frontendUtils = Mockito.mockStatic(FrontendUtils.class);
        frontendUtils.when(() -> FrontendUtils.isHillaUsed(Mockito.any()))
                .thenReturn(true);

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
        frontendUtils.close();
        closeable.close();
        CurrentInstance.clearAll();
    }

    @Test
    public void getMenuItemsContainsExpectedClientPaths() throws IOException {
        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        Map<String, AvailableViewInfo> menuItems = MenuRegistry
                .getMenuItems(true);

        Assert.assertEquals(10, menuItems.size());
        assertClientRoutes(menuItems);
    }

    @Test
    public void getMenuItemsWithNestedFiltering_doesNotThrow()
            throws IOException {
        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), nestedLoginRequiredRouteFile);

        Map<String, AvailableViewInfo> menuItems = MenuRegistry
                .getMenuItems(true);

        Assert.assertEquals(0, menuItems.size());
    }

    @Test
    public void getMenuItemsNoFilteringContainsAllClientPaths()
            throws IOException {
        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        Map<String, AvailableViewInfo> menuItems = MenuRegistry
                .getMenuItems(false);

        Assert.assertEquals(13, menuItems.size());
        // Validate as if logged in as all routes should be available
        assertClientRoutes(menuItems, true, true, false);
    }

    @Test
    public void testNonCollidingServerAndClientRoutesDoesNotThrow()
            throws IOException {
        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(MyRoute.class, MyInfo.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        Map<String, AvailableViewInfo> menuItems = MenuRegistry
                .getMenuItems(false);
        Assert.assertEquals(15, menuItems.size());

        RouteUtil.checkForClientRouteCollisions(vaadinService,
                routeConfiguration.getAvailableRoutes());
    }

    @Test(expected = InvalidRouteConfigurationException.class)
    public void testCollidingServerAndClientRouteDoesThrow()
            throws IOException {
        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(MyRoute.class, MyInfo.class)
                .forEach(routeConfiguration::setAnnotatedRoute);
        routeConfiguration.setAnnotatedRoute(ConflictRoute.class);
    }

    @Test
    public void productionMode_getMenuItemsContainsExpectedClientPaths()
            throws IOException {
        Mockito.when(deploymentConfiguration.isProductionMode())
                .thenReturn(true);
        // Clear any production mode execution route contents
        MenuRegistry.clearFileRoutesCache();

        tmpDir.newFolder("META-INF", "VAADIN");
        File clientFiles = new File(tmpDir.getRoot(),
                FILE_ROUTES_JSON_PROD_PATH);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        ClassLoader mockClassLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(mockClassLoader.getResource(FILE_ROUTES_JSON_PROD_PATH))
                .thenReturn(clientFiles.toURI().toURL());
        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class, Mockito.CALLS_REAL_METHODS)) {
            menuRegistry.when(() -> MenuRegistry.getClassLoader())
                    .thenReturn(mockClassLoader);

            Map<String, AvailableViewInfo> menuItems = MenuRegistry
                    .getMenuItems(true);

            Assert.assertEquals(10, menuItems.size());
            assertClientRoutes(menuItems);
        } finally {
            // Clear our routes from production mode cache
            MenuRegistry.clearFileRoutesCache();
        }
    }

    @Test
    public void getMenuItemsContainsExpectedServerPaths() {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(MyRoute.class, MyInfo.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        Map<String, AvailableViewInfo> menuItems = MenuRegistry
                .getMenuItems(true);

        Assert.assertEquals(2, menuItems.size());
        assertServerRoutes(menuItems);
    }

    @Test
    public void getMenuItemsContainBothClientAndServerPaths()
            throws IOException {
        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(MyRoute.class, MyInfo.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        Map<String, AvailableViewInfo> menuItems = MenuRegistry
                .getMenuItems(true);

        Assert.assertEquals(12, menuItems.size());
        assertClientRoutes(menuItems);
        assertServerRoutes(menuItems);
    }

    @Test
    public void collectMenuItems_returnsCorrectPaths() throws IOException {
        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(MyRoute.class, MyInfo.class, MyRequiredParamRoute.class,
                MyRequiredAndOptionalParamRoute.class,
                MyOptionalParamRoute.class, MyVarargsParamRoute.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        Map<String, AvailableViewInfo> menuItems = MenuRegistry
                .collectMenuItems();

        Assert.assertEquals(8, menuItems.size());
        assertClientRoutes(menuItems, false, false, true);
        assertServerRoutes(menuItems);
        assertServerRoutesWithParameters(menuItems, true);
    }

    @Test
    public void testWithLoggedInUser_userHasRoles() throws IOException {
        Mockito.when(request.getUserPrincipal())
                .thenReturn(Mockito.mock(Principal.class));
        Mockito.when(request.isUserInRole(Mockito.anyString()))
                .thenReturn(true);

        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        Map<String, AvailableViewInfo> menuItems = MenuRegistry
                .getMenuItems(true);

        Assert.assertEquals(13, menuItems.size());
        assertClientRoutes(menuItems, true, true, false);

        // Verify that getMenuItemsList returns the same data
        List<AvailableViewInfo> menuItemsList = MenuRegistry
                .collectMenuItemsList();
        Assert.assertEquals(
                "List of menu items has incorrect size. Excluded menu item like /login is not expected.",
                7, menuItemsList.size());
        assertOrder(menuItemsList,
                new String[] { "/", "/about", "/hilla", "/hilla/sub",
                        "/opt_params", "/params_with_opt_children",
                        "/wc_params" });
    }

    @Test
    public void testWithLoggedInUser_noMatchingRoles() throws IOException {
        Mockito.when(request.getUserPrincipal())
                .thenReturn(Mockito.mock(Principal.class));
        Mockito.when(request.isUserInRole(Mockito.anyString()))
                .thenReturn(false);

        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        Map<String, AvailableViewInfo> menuItems = MenuRegistry
                .getMenuItems(true);

        Assert.assertEquals(11, menuItems.size());
        assertClientRoutes(menuItems, true, false, false);
    }

    @Test
    public void getMenuItemsList_returnsCorrectPaths() throws IOException {
        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), testClientRouteFile);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(MyRoute.class, MyInfo.class, MyRequiredParamRoute.class,
                MyRequiredAndOptionalParamRoute.class,
                MyOptionalParamRoute.class, MyVarargsParamRoute.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        List<AvailableViewInfo> menuItems = MenuRegistry.collectMenuItemsList();
        Assert.assertEquals(8, menuItems.size());
        assertOrder(menuItems,
                new String[] { "/", "/home", "/info", "/opt_params", "/param",
                        "/param/varargs", "/params_with_opt_children",
                        "/wc_params" });
        // verifying that data is same as with collectMenuItems
        Map<String, AvailableViewInfo> mapMenuItems = menuItems.stream()
                .collect(Collectors.toMap(AvailableViewInfo::route,
                        item -> item));
        assertClientRoutes(mapMenuItems, false, false, true, "/");
        assertServerRoutes(mapMenuItems);
        assertServerRoutesWithParameters(mapMenuItems, true);
    }

    @Test
    public void getMenuItemsList_assertOrder() {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(TestRouteA.class, TestRouteB.class, TestRouteC.class,
                TestRouteD.class, TestRouteDA.class, TestRouteDB.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        List<AvailableViewInfo> menuItems = MenuRegistry.collectMenuItemsList();
        Assert.assertEquals(4, menuItems.size());
        assertOrder(menuItems,
                new String[] { "/d", "/c", "/a", "/b", "/d/a", "/d/b" });
    }

    @Test
    public void hasHillaAutoLayout_fileRoutesHasSingleRootLayout_true()
            throws IOException {
        ArrayNode fileRoutes = (ArrayNode) JacksonUtils.getMapper()
                .readTree(testClientRouteFile);
        JsonNode layout = fileRoutes.get(0);
        ArrayNode children = (ArrayNode) layout.get("children");
        Assert.assertNotNull(children);

        assertHasHillaMainLayout(testClientRouteFile, true);
    }

    @Test
    public void hasHillaAutoLayout_fileRoutesHasEmptyChildren_true()
            throws IOException {
        ArrayNode fileRoutes = (ArrayNode) JacksonUtils.getMapper()
                .readTree(emptyChildren);
        JsonNode layout = fileRoutes.get(0);
        ArrayNode children = (ArrayNode) layout.get("children");
        Assert.assertNotNull(children);
        Assert.assertEquals(0, children.size());

        assertHasHillaMainLayout(emptyChildren, true);
    }

    @Test
    public void hasHillaAutoLayout_fileRoutesHasSingleRootRoute_false()
            throws IOException {
        Assert.assertFalse(singleRoute.contains("\"children\""));

        assertHasHillaMainLayout(singleRoute, false);
    }

    @Test
    public void hasHillaAutoLayout_fileRoutesHasMultipleRootRoutes_false()
            throws IOException {
        assertHasHillaMainLayout(multipleRootRoutes, false);
    }

    @Test
    public void hasHillaAutoLayout_fileRoutesHasNonEmptyRoute_false()
            throws IOException {
        assertHasHillaMainLayout(nonEmptyRoute, false);
    }

    private void assertHasHillaMainLayout(String fileRoutes, boolean expected)
            throws IOException {
        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(), fileRoutes);

        boolean hasHillaMainLayout = MenuRegistry
                .hasHillaMainLayout(vaadinService.getDeploymentConfiguration());
        Assert.assertEquals(expected, hasHillaMainLayout);
    }

    private void assertOrder(List<AvailableViewInfo> menuItems,
            String[] expectedOrder) {
        for (int i = 0; i < menuItems.size(); i++) {
            Assert.assertEquals(expectedOrder[i], menuItems.get(i).route());
        }
    }

    private void assertClientRoutes(Map<String, AvailableViewInfo> menuItems) {
        assertClientRoutes(menuItems, false, false, false);
    }

    private void assertClientRoutes(Map<String, AvailableViewInfo> menuItems,
            boolean authenticated, boolean hasRole, boolean excludeExpected) {
        assertClientRoutes(menuItems, authenticated, hasRole, excludeExpected,
                "");
    }

    private void assertClientRoutes(Map<String, AvailableViewInfo> menuItems,
            boolean authenticated, boolean hasRole, boolean excludeExpected,
            String expectedRootPath) {
        Assert.assertTrue("Client route '" + expectedRootPath + "' missing",
                menuItems.containsKey(expectedRootPath));
        Assert.assertEquals("Public", menuItems.get(expectedRootPath).title());
        Assert.assertNotNull("Public should contain default menu data",
                menuItems.get(expectedRootPath).menu());

        if (authenticated) {
            Assert.assertTrue("Client route 'about' missing",
                    menuItems.containsKey("/about"));
            Assert.assertEquals("About", menuItems.get("/about").title());
            Assert.assertTrue("Login should be required",
                    menuItems.get("/about").loginRequired());
            Assert.assertNotNull("About should contain default menu data",
                    menuItems.get("/about").menu());

            if (hasRole) {
                Assert.assertTrue("Client route 'hilla' missing",
                        menuItems.containsKey("/hilla"));
                Assert.assertEquals("Hilla", menuItems.get("/hilla").title());
                Assert.assertTrue("Login should be required",
                        menuItems.get("/hilla").loginRequired());
                Assert.assertArrayEquals("Faulty roles fo hilla",
                        new String[] { "ROLE_USER" },
                        menuItems.get("/hilla").rolesAllowed());
                Assert.assertNotNull("Hilla should contain default menu data",
                        menuItems.get("/hilla").menu());

                Assert.assertTrue("Client child route 'hilla/sub' missing",
                        menuItems.containsKey("/hilla/sub"));
                Assert.assertEquals("Hilla Sub",
                        menuItems.get("/hilla/sub").title());
            } else {
                Assert.assertFalse(
                        "Roles do not match no hilla should be available",
                        menuItems.containsKey("/hilla"));
            }
        } else {
            Assert.assertFalse(
                    "Not authenticated about view should not be available",
                    menuItems.containsKey("/about"));
            Assert.assertFalse(
                    "Not authenticated hilla view should not be available",
                    menuItems.containsKey("/hilla"));
        }

        if (excludeExpected) {
            Assert.assertFalse("Client route 'login' should be excluded",
                    menuItems.containsKey("/login"));
        } else {
            Assert.assertTrue("Client route 'login' missing",
                    menuItems.containsKey("/login"));
            Assert.assertEquals("Login", menuItems.get("/login").title());
            Assert.assertNull(menuItems.get("/login").menu().title());
            Assert.assertTrue("Login view should be excluded",
                    menuItems.get("/login").menu().exclude());
        }
    }

    private void assertServerRoutes(Map<String, AvailableViewInfo> menuItems) {
        Assert.assertTrue("Server route 'home' missing",
                menuItems.containsKey("/home"));
        Assert.assertEquals("MyRoute", menuItems.get("/home").title());
        Assert.assertEquals("Home", menuItems.get("/home").menu().title());

        Assert.assertTrue("Server route 'info' missing",
                menuItems.containsKey("/info"));
        Assert.assertEquals("MyInfo", menuItems.get("/info").title());
        Assert.assertEquals("MyInfo", menuItems.get("/info").menu().title());
    }

    private void assertServerRoutesWithParameters(
            Map<String, AvailableViewInfo> menuItems) {
        assertServerRoutesWithParameters(menuItems, false);
    }

    private void assertServerRoutesWithParameters(
            Map<String, AvailableViewInfo> menuItems, boolean excludeExpected) {
        if (excludeExpected) {
            Assert.assertFalse(
                    "Server route '/param/:param' should be excluded",
                    menuItems.containsKey("/param/:param"));
            Assert.assertFalse(
                    "Server route '/param/:param1' should be excluded",
                    menuItems.containsKey("/param/:param1"));
        } else {
            Assert.assertTrue("Server route '/param/:param' missing",
                    menuItems.containsKey("/param/:param"));
            Assert.assertTrue(
                    "Server route '/param/:param' should be excluded from menu",
                    menuItems.get("/param/:param").menu().exclude());

            Assert.assertTrue("Server route '/param/:param1' missing",
                    menuItems.containsKey("/param/:param1"));
            Assert.assertTrue(
                    "Server route '/param/:param1' should be excluded from menu",
                    menuItems.get("/param/:param1").menu().exclude());
        }

        Assert.assertTrue(
                "Server route with optional parameters '/param' missing",
                menuItems.containsKey("/param"));
        Assert.assertFalse(
                "Server route '/param' should be included in the menu",
                menuItems.get("/param").menu().exclude());

        Assert.assertTrue(
                "Server route with optional parameters '/param/varargs' missing",
                menuItems.containsKey("/param/varargs"));
        Assert.assertFalse(
                "Server route '/param/varargs' should be included in the menu",
                menuItems.get("/param/varargs").menu().exclude());
    }

    @Tag("div")
    @Route("home")
    @Menu(title = "Home")
    public static class MyRoute extends Component {
    }

    @Tag("div")
    @Route("info")
    @Menu
    public static class MyInfo extends Component {
    }

    @Tag("div")
    @Route("about")
    @Menu(title = "about")
    public static class ConflictRoute extends Component {
    }

    @Tag("div")
    @Route("param/:param")
    @Menu
    public static class MyRequiredParamRoute extends Component {
    }

    @Tag("div")
    @Route("param/:param1/:param2?")
    @Menu
    public static class MyRequiredAndOptionalParamRoute extends Component {
    }

    @Tag("div")
    @Route("param/:param1?/:param2?(edit)")
    @Menu
    public static class MyOptionalParamRoute extends Component {
    }

    @Tag("div")
    @Route("param/varargs/:param*")
    @Menu
    public static class MyVarargsParamRoute extends Component {
    }

    @Tag("div")
    @Route("a")
    @Menu(order = 1.1)
    public static class TestRouteA extends Component {
    }

    @Tag("div")
    @Route("b")
    @Menu(order = 1.2)
    public static class TestRouteB extends Component {
    }

    @Tag("div")
    @Route("c")
    @Menu(order = 0.1)
    public static class TestRouteC extends Component {
    }

    @Tag("div")
    @Route("d")
    @Menu(order = 0)
    public static class TestRouteD extends Component {
    }

    @Tag("div")
    @Route("d/b")
    public static class TestRouteDB extends Component {

    }

    @Tag("div")
    @Route("d/a")
    public static class TestRouteDA extends Component {
    }

    /**
     * Extending class to let us mock the getRouteRegistry method for testing.
     */
    public static class MockService extends VaadinServletService {

        @Override
        public RouteRegistry getRouteRegistry() {
            return super.getRouteRegistry();
        }

        @Override
        public Instantiator getInstantiator() {
            return new DefaultInstantiator(this);
        }
    }

    public static String testClientRouteFile = """
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
                    "title": "Hilla",
                    "children": [
                      {
                        "route": "sub",
                        "params": {},
                        "title": "Hilla Sub"
                      }
                    ]
                  },
                  {
                    "route": "wc_params/:param?",
                    "loginRequired": false,
                    "params": {
                       ":param": "*"
                    },
                    "title": "wc_params path is included in menu"
                  },
                  {
                    "route": "opt_params/:param?",
                    "loginRequired": false,
                    "params": {
                       ":param": "opt"
                    },
                    "title": "opt_params path is included in menu"
                  },
                  {
                    "route": "params_with_opt_children",
                    "loginRequired": false,
                    "children": [
                        {
                            "route": ":param?",
                            "loginRequired": false,
                            "params": {
                               ":param": "opt"
                            },
                            "title": "params_with_opt_children/:param? path is included in menu"
                        }
                    ]
                  },
                  {
                    "route": "req_params/:param",
                    "loginRequired": false,
                    "params": {
                       ":param": "req"
                    },
                    "title": "req_params path is excluded from menu"
                  },
                  {
                    "route": "params",
                    "loginRequired": false,
                    "title": null,
                    "children": [
                      {
                        "route": ":param",
                        "loginRequired": false,
                        "params": {
                           ":param": "req"
                        },
                        "title": "params/:param path is excluded from menu",
                        "children": [
                          {
                            "route": "sub",
                            "loginRequired": false,
                            "title": "params/:param/sub path is excluded from menu"
                          }
                        ]
                      }
                    ]
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

    String nestedLoginRequiredRouteFile = """
            [
              {
                "route": "",
                "params": {},
                "title": "current Title",
                "children": [
                  {
                    "route": "",
                    "loginRequired": true,
                    "params": {},
                    "title": "navigate"
                  },
                  {
                    "route": "admin",
                    "loginRequired": true,
                    "title": "Admin",
                    "params": {},
                    "children": [
                      {
                        "route": "planets",
                        "loginRequired": true,
                        "title": "Planets",
                        "params": {},
                        "children": [
                          {
                            "route": "",
                            "loginRequired": true,
                            "title": "Planets",
                            "params": {}
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            ]
            """;

    String emptyChildren = """
            [
              {
                "route": "",
                "title": "Main Layout",
                "children": []
              }
            ]
            """;

    String nonEmptyRoute = """
            [
              {
                "route": "foo",
                "title": "Main Layout",
                "children": [
                  {
                    "route": "hilla",
                    "flowLayout": false,
                    "params": {},
                    "title": "Hilla view"
                  }
                ]
              }
            ]
            """;

    String singleRoute = """
            [
              {
                "route": "",
                "menu": {
                  "title": "Public page",
                  "icon": "vaadin:group"
                },
                "flowLayout": false,
                "params": {},
                "title": "Public"
              }
            ]
            """;

    String multipleRootRoutes = """
            [
              {
                "route": "hilla",
                "flowLayout": false,
                "params": {},
                "children": [
                  {
                    "route": "",
                    "flowLayout": false,
                    "params": {},
                    "title": "Layout"
                  }
                ]
              },
              {
                "route": "",
                "flowLayout": false,
                "params": {},
                "title": "Layout",
                "children": [
                  {
                    "route": "components",
                    "menu": {
                      "title": "React Components"
                    },
                    "flowLayout": false,
                    "params": {},
                    "title": "Components"
                  }
                ]
              }
            ]
            """;
}