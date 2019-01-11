package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class RouteConfigurationTest {

    private ApplicationRouteRegistry registry;
    private MockService vaadinService;
    private VaadinSession session;
    private ServletContext servletContext;

    @Before
    public void init() {
        servletContext = Mockito.mock(ServletContext.class);
        registry = ApplicationRouteRegistry.getInstance(servletContext);

        Mockito.when(servletContext.getAttribute(RouteRegistry.class.getName()))
                .thenReturn(registry);

        vaadinService = Mockito.mock(MockService.class);
        Mockito.when(vaadinService.getRouteRegistry()).thenReturn(registry);

        VaadinService.setCurrent(vaadinService);

        session = new MockVaadinSession(vaadinService) {
            @Override
            public VaadinService getService() {
                return vaadinService;
            }
        };
    }

    /**
     * Get registry by handing the session lock correctly.
     *
     * @param session
     *            target vaadin session
     * @return session route registry for session if exists or new.
     */
    private SessionRouteRegistry getRegistry(VaadinSession session) {
        try {
            session.lock();
            return (SessionRouteRegistry) SessionRouteRegistry
                    .getSessionRegistry(session);
        } finally {
            session.unlock();
        }
    }

    @Test
    public void routeConfigurationUpdateLock_newConfigurationIsGottenOnlyAfterUnlock() {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(getRegistry(session));

        routeConfiguration.update(() -> {
            routeConfiguration.setRoute("", MyRoute.class,
                    Collections.emptyList());

            Assert.assertTrue("Registry should still remain empty",
                    routeConfiguration.getAvailableRoutes().isEmpty());

            routeConfiguration.setRoute("path", Secondary.class,
                    Collections.emptyList());

            Assert.assertTrue("Registry should still remain empty",
                    routeConfiguration.getAvailableRoutes().isEmpty());
        });

        Assert.assertEquals(
                "After unlock registry should be updated for others to configure with new data",
                2, routeConfiguration.getAvailableRoutes().size());
    }

    @Test
    public void isRouteRegistered_returnsCorrectly() {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(getRegistry(session));

        routeConfiguration.update(() -> {
            routeConfiguration.setRoute("", MyRoute.class,
                    Collections.emptyList());
            routeConfiguration.setRoute("path", Secondary.class,
                    Collections.emptyList());
        });

        Assert.assertTrue("Registered 'MyRoute.class' should return true",
                routeConfiguration.isRouteRegistered(MyRoute.class));
        Assert.assertTrue("Registered 'Secondary.class' should return true",
                routeConfiguration.isRouteRegistered(Secondary.class));
        Assert.assertFalse("Unregistered 'Url.class' should return false",
                routeConfiguration.isRouteRegistered(Url.class));
    }

    @Test
    public void routeConfiguration_getMethodsReturnCorrectly() {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(getRegistry(session));

        routeConfiguration.update(() -> {
            routeConfiguration.setRoute("", MyRoute.class);
            routeConfiguration.setRoute("path", Secondary.class);
            routeConfiguration.setRoute("parents", MiddleLayout.class,
                    MainLayout.class);
        });

        Assert.assertEquals(
                "After unlock registry should be updated for others to configure with new data",
                3, routeConfiguration.getAvailableRoutes().size());
        Assert.assertTrue("Expected path '' to be registered",
                routeConfiguration.isPathRegistered(""));
        Assert.assertTrue("Expected path 'path' to be registered",
                routeConfiguration.isPathRegistered("path"));
        Assert.assertTrue("Expected path 'parents' to be registered",
                routeConfiguration.isPathRegistered("parents"));

        Assert.assertEquals("Url should have only been 'parents'", "parents",
                routeConfiguration.getUrl(MiddleLayout.class));

        Optional<Class<? extends Component>> pathRoute = routeConfiguration
                .getRoute("path");
        Assert.assertTrue("'path' should have returned target class",
                pathRoute.isPresent());
        Assert.assertEquals("'path' registration should be Secondary",
                Secondary.class, pathRoute.get());

        routeConfiguration.update(() -> {
            routeConfiguration.removeRoute("path");
            routeConfiguration.setRoute("url", Url.class);
        });

        Assert.assertFalse(
                "Removing the path 'path' should have cleared it from the registry",
                routeConfiguration.isPathRegistered("path"));

        Assert.assertTrue("Expected path 'url' to be registered",
                routeConfiguration.isPathRegistered("url"));

        Optional<Class<? extends Component>> urlRoute = routeConfiguration
                .getRoute("url");

        Assert.assertFalse(
                "'url' with no parameters should not have returned a class",
                urlRoute.isPresent());

        urlRoute = routeConfiguration.getRoute("url",
                Collections.singletonList("param"));
        Assert.assertTrue("'url' with parameters should have returned a class",
                urlRoute.isPresent());
        Assert.assertEquals("'url' registration should be Url", Url.class,
                urlRoute.get());
    }

    @Test
    public void addListenerToApplicationScoped_noEventForSessionChange() {
        VaadinServlet servlet = Mockito.mock(VaadinServlet.class);
        Mockito.when(servlet.getServletContext()).thenReturn(servletContext);
        Mockito.when(vaadinService.getServlet()).thenReturn(servlet);

        try {
            CurrentInstance.set(VaadinService.class, vaadinService);
            VaadinSession.setCurrent(session);
            session.lock();
            RouteConfiguration routeConfiguration = RouteConfiguration
                    .forApplicationScope();

            List<RouteBaseData> added = new ArrayList<>();
            List<RouteBaseData> removed = new ArrayList<>();

            routeConfiguration.addRoutesChangeListener(event -> {
                added.clear();
                removed.clear();
                added.addAll(event.getAddedRoutes());
                removed.addAll(event.getRemovedRoutes());
            });

            routeConfiguration.setRoute("old", MyRoute.class);

            Assert.assertEquals(1, added.size());
            Assert.assertEquals(0, removed.size());

            added.clear();
            removed.clear();

            routeConfiguration = RouteConfiguration.forSessionScope();

            routeConfiguration.setRoute("new", MyRoute.class);

            Assert.assertEquals(0, added.size());
            Assert.assertEquals(0, removed.size());
        } finally {
            session.unlock();
            CurrentInstance.clearAll();
        }
    }

    @Test
    public void addListenerToSessionScoped_alsoEventsForApplicationScope() {
        VaadinServlet servlet = Mockito.mock(VaadinServlet.class);
        Mockito.when(servlet.getServletContext()).thenReturn(servletContext);
        Mockito.when(vaadinService.getServlet()).thenReturn(servlet);

        try {
            CurrentInstance.set(VaadinService.class, vaadinService);
            VaadinSession.setCurrent(session);
            session.lock();

            List<RouteBaseData> added = new ArrayList<>();
            List<RouteBaseData> removed = new ArrayList<>();

            RouteConfiguration routeConfiguration = RouteConfiguration
                    .forSessionScope();

            routeConfiguration.addRoutesChangeListener(event -> {
                added.clear();
                removed.clear();
                added.addAll(event.getAddedRoutes());
                removed.addAll(event.getRemovedRoutes());
            });

            routeConfiguration.setRoute("old", MyRoute.class);

            Assert.assertEquals(1, added.size());
            Assert.assertEquals(0, removed.size());

            added.clear();
            removed.clear();

            routeConfiguration = RouteConfiguration.forApplicationScope();

            routeConfiguration.setRoute("new", MyRoute.class);

            Assert.assertEquals(1, added.size());
            Assert.assertEquals(0, removed.size());
        } finally {
            session.unlock();
            CurrentInstance.clearAll();
        }
    }

    @Test
    public void configurationForSessionRegistry_buildsWithCorrectRegistry() {
        SessionRouteRegistry registry = getRegistry(session);
        registry.update(() -> {
            registry.setRoute("", MyRoute.class, Collections.emptyList());
            registry.setRoute("path", Secondary.class, Collections.emptyList());
        });

        try {
            VaadinSession.setCurrent(session);
            session.lock();
            RouteConfiguration routeConfiguration = RouteConfiguration
                    .forSessionScope();

            Assert.assertEquals(
                    "After unlock registry should be updated for others to configure with new data",
                    2, routeConfiguration.getAvailableRoutes().size());
        } finally {
            session.unlock();
            CurrentInstance.clearAll();
        }
    }

    @Test
    public void configurationForApplicationScope_buildsWithCorrectRegistry() {
        registry.update(() -> {
            registry.setRoute("", MyRoute.class, Collections.emptyList());
            registry.setRoute("path", Secondary.class, Collections.emptyList());
        });

        VaadinServlet servlet = Mockito.mock(VaadinServlet.class);
        Mockito.when(servlet.getServletContext()).thenReturn(servletContext);
        Mockito.when(vaadinService.getServlet()).thenReturn(servlet);

        try {
            CurrentInstance.set(VaadinService.class, vaadinService);
            session.lock();
            RouteConfiguration routeConfiguration = RouteConfiguration
                    .forApplicationScope();

            Assert.assertEquals(
                    "After unlock registry should be updated for others to configure with new data",
                    2, routeConfiguration.getAvailableRoutes().size());
        } finally {
            session.unlock();
            CurrentInstance.clearAll();
        }
    }

    @Test
    public void setRoutes_allExpectedRoutesAreSet() {

        RouteRegistry registry = Mockito.mock(RouteRegistry.class);
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);

        Mockito.doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((Command) args[0]).execute();
            return null;
        }).when(registry).update(Mockito.any(Command.class));

        routeConfiguration.setRoutes(Stream.of(MyRoute.class, MyInfo.class,
                MyPalace.class, MyModular.class).collect(Collectors.toSet()));

        Mockito.verify(registry).update(Mockito.any());

        Mockito.verify(registry).setRoute("home", MyRoute.class,
                Collections.emptyList());

        Mockito.verify(registry).setRoute("info", MyInfo.class,
                Collections.emptyList());

        Mockito.verify(registry).setRoute("palace", MyPalace.class,
                Collections.emptyList());

        Mockito.verify(registry).setRoute("modular", MyModular.class,
                Collections.emptyList());
    }

    @Test
    public void registeredRouteWithAlias_allPathsAreRegistered() {
        RouteRegistry registry = Mockito.mock(RouteRegistry.class);
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);

        routeConfiguration.setAnnotatedRoute(MyRouteWithAliases.class);

        Mockito.verify(registry).setRoute("withAliases",
                MyRouteWithAliases.class, Collections.emptyList());
        Mockito.verify(registry).setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());
        Mockito.verify(registry).setRoute("person", MyRouteWithAliases.class,
                Collections.emptyList());

    }

    @Test
    public void routeWithParent_parentsAreCollectedCorrectly() {
        RouteRegistry registry = Mockito.mock(RouteRegistry.class);
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);

        routeConfiguration.setAnnotatedRoute(SingleLayout.class);

        Mockito.verify(registry).setRoute("single", SingleLayout.class,
                Collections.singletonList(MainLayout.class));

        routeConfiguration.setAnnotatedRoute(DoubleLayout.class);

        Mockito.verify(registry).setRoute("double", DoubleLayout.class,
                Arrays.asList(MiddleLayout.class, MainLayout.class));
    }

    @Test
    public void parentLayoutAnnotatedClass_parentsCorrecltCollected() {
        RouteRegistry registry = Mockito.mock(RouteRegistry.class);
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);

        routeConfiguration.setParentAnnotatedRoute("middle",
                MiddleLayout.class);

        Mockito.verify(registry).setRoute("middle", MiddleLayout.class,
                Collections.singletonList(MainLayout.class));
    }

    @Test
    public void abolute_route_alias_gets_expected_parent_layouts() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteConfiguration
                .forRegistry(registry)
                .discoverParentLayouts(AbsoluteRoute.class, "alias");

        Assert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouteConfiguration.forRegistry(registry)
                .discoverParentLayouts(AbsoluteCenterRoute.class,
                        "absolute/alias");

        Assert.assertThat(
                "Expected to receive MiddleParent and Parent classes as parents.",
                parentLayouts,
                IsIterableContainingInOrder.contains(new Class[] {
                        AbsoluteCenterParent.class, RoutePrefixParent.class }));

    }

    @Test
    public void absolute_route_gets_expected_parent_layouts() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteConfiguration
                .forRegistry(registry)
                .discoverParentLayouts(AbsoluteRoute.class, "single");

        Assert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouteConfiguration.forRegistry(registry)
                .discoverParentLayouts(AbsoluteCenterRoute.class,
                        "absolute/child");

        Assert.assertThat(
                "Expected to receive MiddleParent and Parent classes as parents.",
                parentLayouts,
                IsIterableContainingInOrder.contains(new Class[] {
                        AbsoluteCenterParent.class, RoutePrefixParent.class }));
    }

    @Test
    public void expected_parent_layouts_are_found_for_route_alias() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteConfiguration
                .forRegistry(registry).discoverParentLayouts(
                        RouteAliasWithParentPrefix.class, "aliasparent/alias");

        Assert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder.contains(
                        new Class[] { RouteAliasPrefixParent.class }));
    }

    @Test
    public void expected_parent_layouts_are_found_for_route() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteConfiguration
                .forRegistry(registry).discoverParentLayouts(
                        BaseRouteWithParentPrefixAndRouteAlias.class, "parent");

        Assert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouteConfiguration.forRegistry(registry)
                .discoverParentLayouts(RootWithParents.class, "");

        Assert.assertThat(
                "Expected to receive MiddleParent and Parent classes as parents.",
                parentLayouts, IsIterableContainingInOrder.contains(
                        new Class[] { MiddleParent.class, Parent.class }));
    }

    @Test // 3424
    public void parent_layouts_resolve_correctly_for_route_parent() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteConfiguration
                .forRegistry(registry)
                .discoverParentLayouts(MultiTarget.class, "");

        Assert.assertThat(
                "Get parent layouts for route \"\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { Parent.class }));

        parentLayouts = RouteConfiguration.forRegistry(registry)
                .discoverParentLayouts(MultiTarget.class, "alias");

        Assert.assertThat(
                "Get parent layouts for routeAlias \"alias\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder.contains(
                        new Class[] { MiddleParent.class, Parent.class }));

        parentLayouts = RouteConfiguration.forRegistry(registry)
                .discoverParentLayouts(SubLayout.class, "parent/sub");

        Assert.assertThat(
                "Get parent layouts for route \"parent/sub\" with parent Route + ParentLayout gave wrong result.",
                parentLayouts,
                IsIterableContainingInOrder.contains(new Class[] {
                        MultiTarget.class, RoutePrefixParent.class }));
    }

    @Test
    public void absolute_middle_parent_route_should_not_contain_parent_prefix() {
        String routePath = RouteConfiguration.forRegistry(registry)
                .getRoutePath(AbsoluteRoute.class,
                        AbsoluteCenterRoute.class.getAnnotation(Route.class));
        Assert.assertEquals("No parent prefix should have been added.",
                "absolute/child", routePath);
    }

    @Test
    public void route_path_should_contain_parent_prefix() {
        String routePath = RouteConfiguration.forRegistry(registry)
                .getRoutePath(BaseRouteWithParentPrefixAndRouteAlias.class,
                        BaseRouteWithParentPrefixAndRouteAlias.class
                                .getAnnotation(Route.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "parent", routePath);
    }

    @Test
    public void absolute_route_should_not_contain_parent_prefix() {
        String routePath = RouteConfiguration.forRegistry(registry)
                .getRoutePath(AbsoluteRoute.class,
                        AbsoluteRoute.class.getAnnotation(Route.class));
        Assert.assertEquals("No parent prefix should have been added.",
                "single", routePath);
    }

    @Test
    public void route_path_should_contain_route_and_parent_prefix() {
        String routePath = RouteConfiguration.forRegistry(registry)
                .getRoutePath(RouteWithParentPrefixAndRouteAlias.class,
                        RouteWithParentPrefixAndRouteAlias.class
                                .getAnnotation(Route.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "parent/flow", routePath);
    }

    @Test
    public void absolute_route_alias_should_not_contain_parent_prefix() {
        String routePath = RouteConfiguration.forRegistry(registry)
                .getRouteAliasPath(AbsoluteRoute.class,
                        AbsoluteRoute.class.getAnnotation(RouteAlias.class));
        Assert.assertEquals("No parent prefix should have been added.", "alias",
                routePath);
    }

    @Test
    public void route_alias_path_should_not_contain_parent_prefix() {
        String routePath = RouteConfiguration.forRegistry(registry)
                .getRouteAliasPath(BaseRouteWithParentPrefixAndRouteAlias.class,
                        BaseRouteWithParentPrefixAndRouteAlias.class
                                .getAnnotation(RouteAlias.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "alias", routePath);
        routePath = RouteConfiguration.forRegistry(registry).getRouteAliasPath(
                RouteWithParentPrefixAndRouteAlias.class,
                RouteWithParentPrefixAndRouteAlias.class
                        .getAnnotation(RouteAlias.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "alias", routePath);
    }

    @Test
    public void absolute_middle_parent_for_route_alias_should_not_contain_parent_prefix() {
        String routePath = RouteConfiguration.forRegistry(registry)
                .getRouteAliasPath(AbsoluteRoute.class,
                        AbsoluteCenterRoute.class
                                .getAnnotation(RouteAlias.class));
        Assert.assertEquals("No parent prefix should have been added.",
                "absolute/alias", routePath);
    }

    @Test
    public void route_alias_should_contain_parent_prefix() {
        String routePath = RouteConfiguration.forRegistry(registry)
                .getRouteAliasPath(RouteAliasWithParentPrefix.class,
                        RouteAliasWithParentPrefix.class
                                .getAnnotation(RouteAlias.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "aliasparent/alias", routePath);
    }

    @Tag("div")
    @Route("home")
    private static class MyRoute extends Component {
    }

    @Tag("div")
    @Route("info")
    private static class MyInfo extends Component {
    }

    @Tag("div")
    @Route("palace")
    private static class MyPalace extends Component {
    }

    @Tag("div")
    @Route("modular")
    private static class MyModular extends Component {
    }

    @Tag("div")
    @Route("withAliases")
    @RouteAlias("version")
    @RouteAlias("person")
    private static class MyRouteWithAliases extends Component {
    }

    @Tag("div")
    @Route(value = "single", layout = MainLayout.class)
    private static class SingleLayout extends Component {
    }

    @Tag("div")
    @Route(value = "double", layout = MiddleLayout.class)
    private static class DoubleLayout extends Component {
    }

    @Tag("div")
    private static class MainLayout extends Component implements RouterLayout {
    }

    @Tag("div")
    @ParentLayout(MainLayout.class)
    private static class MiddleLayout extends Component
            implements RouterLayout {
    }

    @Tag("div")
    private static class Secondary extends Component {
    }

    @Tag("div")
    private static class Url extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    @Route(value = "single", layout = RoutePrefixParent.class, absolute = true)
    @RouteAlias(value = "alias", layout = RoutePrefixParent.class, absolute = true)
    public static class AbsoluteRoute extends Component {
    }

    @Tag(Tag.DIV)
    @RoutePrefix("parent")
    public static class RoutePrefixParent extends Component
            implements RouterLayout {
    }

    @Route(value = "child", layout = AbsoluteCenterParent.class)
    @RouteAlias(value = "alias", layout = AbsoluteCenterParent.class)
    public static class AbsoluteCenterRoute extends Component {
    }

    @Tag(Tag.DIV)
    @ParentLayout(RoutePrefixParent.class)
    @RoutePrefix(value = "absolute", absolute = true)
    public static class AbsoluteCenterParent extends Component
            implements RouterLayout {
    }

    @Route(value = "", layout = RoutePrefixParent.class)
    @RouteAlias("alias")
    @Tag(Tag.DIV)
    public static class BaseRouteWithParentPrefixAndRouteAlias
            extends Component {
    }

    @Tag(Tag.DIV)
    @ParentLayout(Parent.class)
    public static class MiddleParent extends Component implements RouterLayout {
    }

    @Route(value = "", layout = Parent.class)
    @RouteAlias(value = "alias", layout = MiddleParent.class)
    @Tag(Tag.DIV)
    @ParentLayout(RoutePrefixParent.class)
    public static class MultiTarget extends Component implements RouterLayout {
    }

    @Tag(Tag.DIV)
    public static class Parent extends Component implements RouterLayout {
    }

    @Route(value = "", layout = MiddleParent.class)
    @Tag(Tag.DIV)
    public static class RootWithParents extends Component {
    }

    @Tag(Tag.DIV)
    @RoutePrefix("aliasparent")
    public static class RouteAliasPrefixParent extends Component
            implements RouterLayout {
    }

    @Route(value = "flow", layout = RoutePrefixParent.class)
    @RouteAlias(value = "alias", layout = RouteAliasPrefixParent.class)
    @Tag(Tag.DIV)
    public static class RouteAliasWithParentPrefix extends Component {
    }

    @Route(value = "sub", layout = MultiTarget.class)
    @Tag(Tag.DIV)
    public static class SubLayout extends Component {
    }

    @Route(value = "flow", layout = RoutePrefixParent.class)
    @RouteAlias("alias")
    @Tag(Tag.DIV)
    public static class RouteWithParentPrefixAndRouteAlias extends Component {
    }

    /**
     * Extending class to let us mock the getRouteRegistry method for testing.
     */
    private static class MockService extends VaadinServletService {

        @Override
        public RouteRegistry getRouteRegistry() {
            return super.getRouteRegistry();
        }
    }
}