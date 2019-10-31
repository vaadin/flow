package com.vaadin.flow.router;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.MockServletContext;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

@NotThreadSafe
public class RouteConfigurationTest {

    private ApplicationRouteRegistry registry;
    private MockService vaadinService;
    private VaadinSession session;
    private ServletContext servletContext;
    private VaadinServletContext vaadinContext;

    @Before
    public void init() {
        servletContext = new MockServletContext();
        vaadinContext = new VaadinServletContext(servletContext);
        registry = ApplicationRouteRegistry
                .getInstance(vaadinContext);

        vaadinService = Mockito.mock(MockService.class);
        Mockito.when(vaadinService.getRouteRegistry()).thenReturn(registry);
        Mockito.when(vaadinService.getContext()).thenReturn(vaadinContext);

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
    public void routeConfigurationUpdateLock_configurationIsUpdatedOnlyAfterUnlock() {
        CountDownLatch waitReaderThread = new CountDownLatch(1);
        CountDownLatch waitUpdaterThread = new CountDownLatch(2);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(getRegistry(session));

        Thread readerThread = new Thread() {
            @Override
            public void run() {
                awaitCountDown(waitUpdaterThread);

                Assert.assertTrue("Registry should still remain empty",
                        routeConfiguration.getAvailableRoutes().isEmpty());

                awaitCountDown(waitUpdaterThread);

                Assert.assertTrue("Registry should still remain empty",
                        routeConfiguration.getAvailableRoutes().isEmpty());

                waitReaderThread.countDown();
            }
        };

        readerThread.start();

        routeConfiguration.update(() -> {
            routeConfiguration.setRoute("", MyRoute.class,
                    Collections.emptyList());

            waitUpdaterThread.countDown();

            routeConfiguration.setRoute("path", Secondary.class,
                    Collections.emptyList());

            waitUpdaterThread.countDown();
            try {
                waitReaderThread.await();
            } catch (InterruptedException e) {
                Assert.fail();
            }
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

        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            Arrays.asList(MyRoute.class, MyInfo.class, MyPalace.class,
                    MyModular.class)
                    .forEach(routeConfiguration::setAnnotatedRoute);
        });

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

    private void awaitCountDown(CountDownLatch countDownLatch) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Assert.fail();
        }
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