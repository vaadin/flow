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

import jakarta.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
        Mockito.when(deploymentConfiguration.getProjectFolder())
                .thenReturn(tmpDir.getRoot());
        Mockito.when(deploymentConfiguration.getBuildFolder())
                .thenReturn("build");

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
                7, menuEntries.size());
        assertOrder(menuEntries,
                new String[] { "/", "/about", "/hilla", "/hilla/sub",
                        "/opt_params", "/params_with_opt_children",
                        "/wc_params" });
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
        Assert.assertEquals(8, menuEntries.size());
        assertOrder(menuEntries,
                new String[] { "/", "/home", "/info", "/opt_params", "/param",
                        "/param/varargs", "/params_with_opt_children",
                        "/wc_params" });

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

    @Test
    public void getPageHeader_serverSideRoutes_withContentComponent_pageHeadersFromAnnotationAndName() {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(NormalRoute.class, NormalRouteWithPageTitle.class,
                MandatoryParameterRouteWithPageTitle.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        UI mockUi = Mockito.mock(UI.class);
        UIInternals uiInternals = Mockito.mock(UIInternals.class);
        Location location = Mockito.mock(Location.class);
        Mockito.when(mockUi.getInternals()).thenReturn(uiInternals);
        Mockito.when(uiInternals.getActiveViewLocation()).thenReturn(location);
        Mockito.when(uiInternals.getActiveRouterTargetsChain())
                .thenReturn(Collections.emptyList());

        final UI currentUi = UI.getCurrent();

        try {
            UI.setCurrent(mockUi);

            Mockito.when(location.getPath()).thenReturn("/normal-route");
            Optional<String> header = MenuConfiguration
                    .getPageHeader(new NormalRoute());
            Assert.assertTrue(header.isPresent());
            // directly from class name
            Assert.assertEquals("NormalRoute", header.get());

            Mockito.when(location.getPath())
                    .thenReturn("normal-route-with-page-title");
            header = MenuConfiguration
                    .getPageHeader(new NormalRouteWithPageTitle());
            Assert.assertTrue(header.isPresent());
            // directly from @PageTitle
            Assert.assertEquals("My Normal Route", header.get());

            Mockito.when(uiInternals.getActiveRouterTargetsChain())
                    .thenReturn(List.of(new RouteOrLayoutWithDynamicTitle()));
            header = MenuConfiguration.getPageHeader(new NormalRoute());
            Assert.assertTrue(header.isPresent());
            // from HasDynamicTitle
            Assert.assertEquals("My Route with dynamic title", header.get());
            Mockito.when(uiInternals.getActiveRouterTargetsChain())
                    .thenReturn(Collections.emptyList());

            Mockito.when(location.getPath())
                    .thenReturn("mandatory-parameter-route");
            header = MenuConfiguration
                    .getPageHeader(new MandatoryParameterRouteWithPageTitle());
            Assert.assertTrue(header.isPresent());
            // directly from class name
            Assert.assertEquals("MandatoryParameterRouteWithPageTitle",
                    header.get());

        } finally {
            UI.setCurrent(currentUi);
        }
    }

    @Test
    public void getPageHeader_serverSideRoutes_noContentComponent_pageHeadersOnlyForMenuEntries() {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        Arrays.asList(NormalRoute.class, NormalRouteWithPageTitle.class,
                OptionalParameterRouteWithPageTitle.class,
                MandatoryParameterRouteWithPageTitle.class)
                .forEach(routeConfiguration::setAnnotatedRoute);

        UI mockUi = Mockito.mock(UI.class);
        UIInternals uiInternals = Mockito.mock(UIInternals.class);
        Location location = Mockito.mock(Location.class);
        Mockito.when(mockUi.getInternals()).thenReturn(uiInternals);
        Mockito.when(uiInternals.getActiveViewLocation()).thenReturn(location);

        final UI currentUi = UI.getCurrent();

        try {
            UI.setCurrent(mockUi);

            Mockito.when(location.getPath()).thenReturn("/normal-route");
            Optional<String> header = MenuConfiguration.getPageHeader();
            Assert.assertTrue(header.isPresent());
            // from class name, from menu config
            Assert.assertEquals("NormalRoute", header.get());

            Mockito.when(location.getPath())
                    .thenReturn("normal-route-with-page-title");
            header = MenuConfiguration.getPageHeader();
            // no @Menu annotation -> no available view info
            Assert.assertFalse(header.isPresent());

            Mockito.when(location.getPath())
                    .thenReturn("mandatory-parameter-route");
            header = MenuConfiguration.getPageHeader();
            // mandatory route parameter -> no menu entry -> no available view
            // info
            Assert.assertFalse(header.isPresent());

            Mockito.when(location.getPath())
                    .thenReturn("optional-parameter-route");
            header = MenuConfiguration.getPageHeader();
            // optional route parameter -> menu is eligible
            Assert.assertTrue(header.isPresent());
            Assert.assertEquals("OptionalParameterRouteWithPageTitle",
                    header.get());

        } finally {
            UI.setCurrent(currentUi);
        }
    }

    @Test
    public void testGetPageHeader_clientViews_pageHeaderFromTitle()
            throws IOException {
        Mockito.when(request.getUserPrincipal())
                .thenReturn(Mockito.mock(Principal.class));
        Mockito.when(request.isUserInRole(Mockito.anyString()))
                .thenReturn(true);

        File generated = tmpDir.newFolder(GENERATED);
        File clientFiles = new File(generated, FILE_ROUTES_JSON_NAME);
        Files.writeString(clientFiles.toPath(),
                MenuConfigurationTest.testPageHeaderClientRouteFile);

        UI mockUi = Mockito.mock(UI.class);
        UIInternals uiInternals = Mockito.mock(UIInternals.class);
        Location location = Mockito.mock(Location.class);
        Mockito.when(mockUi.getInternals()).thenReturn(uiInternals);
        Mockito.when(uiInternals.getActiveViewLocation()).thenReturn(location);

        final UI currentUi = UI.getCurrent();

        try {
            UI.setCurrent(mockUi);

            Mockito.when(location.getPath()).thenReturn("/");
            Optional<String> header = MenuConfiguration.getPageHeader();
            Assert.assertTrue(header.isPresent());
            // from ViewConfig.title
            Assert.assertEquals("Public", header.get());

            Mockito.when(location.getPath()).thenReturn("/about");
            header = MenuConfiguration.getPageHeader();
            Assert.assertTrue(header.isPresent());
            // from ViewConfig.title, with exclude=true
            Assert.assertEquals("About", header.get());

            Mockito.when(location.getPath()).thenReturn("/other");
            header = MenuConfiguration.getPageHeader();
            Assert.assertTrue(header.isPresent());
            // from ViewConfig.title, with menu config
            Assert.assertEquals("Other", header.get());

            Mockito.when(location.getPath()).thenReturn("/hilla");
            header = MenuConfiguration.getPageHeader();
            Assert.assertTrue(header.isPresent());
            // from ViewConfig.title, when flow layout is false
            Assert.assertEquals("Hilla", header.get());

            Mockito.when(location.getPath()).thenReturn("/flow/hello");
            header = MenuConfiguration.getPageHeader();
            Assert.assertTrue(header.isPresent());
            // from ViewConfig.title, when flow layout is false
            Assert.assertEquals("Hello", header.get());

            Mockito.when(uiInternals.getActiveRouterTargetsChain())
                    .thenReturn(List.of(new RouteOrLayoutWithDynamicTitle()));
            header = MenuConfiguration.getPageHeader();
            Assert.assertTrue(header.isPresent());
            // from HasDynamicTitle
            Assert.assertEquals("My Route with dynamic title", header.get());
            Mockito.when(uiInternals.getActiveRouterTargetsChain())
                    .thenReturn(Collections.emptyList());

        } finally {
            UI.setCurrent(currentUi);
        }
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
                menuOptions.containsKey("/"));
        Assert.assertEquals("Public", menuOptions.get("/").title());

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

    @Tag("some-tag")
    @Route("normal-route")
    @Menu(title = "Normal Route")
    public static class NormalRoute extends Component {
    }

    @Tag("some-tag")
    @PageTitle("My Normal Route")
    @Route("normal-route-with-page-title")
    public static class NormalRouteWithPageTitle extends Component {
    }

    public static class RouteOrLayoutWithDynamicTitle
            implements HasDynamicTitle, HasElement {
        @Override
        public String getPageTitle() {
            return "My Route with dynamic title";
        }

        @Override
        public Element getElement() {
            return null;
        }
    }

    @Tag("some-tag")
    @Route("optional-parameter-route")
    @Menu(title = "Optional Param route")
    public static class OptionalParameterRouteWithPageTitle extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter String parameter) {

        }
    }

    @Tag("some-tag")
    @Route("mandatory-parameter-route")
    @Menu(title = "Mandatory Param route")
    public static class MandatoryParameterRouteWithPageTitle extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {

        }
    }

    public static String testPageHeaderClientRouteFile = """
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
                    "menu": { "exclude": true },
                    "title": "About"
                  },
                  {
                    "route": "other",
                    "menu": { "title": "Other" },
                    "title": "Other"
                  },
                  {
                    "route": "hilla",
                    "title": "Hilla",
                    "flowLayout": false
                  }
                ]
              },
              {
                "route": "flow",
                "params": {},
                "children": [
                  {
                    "route": "hello",
                    "menu": {
                      "title": "Hello For Flow Layout"
                    },
                    "title": "Hello"
                  }
                ]
              }
            ]
            """;
}
