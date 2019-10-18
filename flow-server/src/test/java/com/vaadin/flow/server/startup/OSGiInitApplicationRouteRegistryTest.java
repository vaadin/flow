/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.osgi.OSGiAccess;

/**
 * Tests for {@link ApplicationRouteRegistry} instance which is initialized via
 * OSGi routes initialization logic.
 *
 */
@RunWith(EnableOSGiRunner.class)
@NotThreadSafe
public class OSGiInitApplicationRouteRegistryTest
        extends RouteRegistryTestBase {

    private ApplicationRouteRegistry registry;
    private RouteRegistry osgiCollectorRegistry;

    @Before
    public void init() {
        OSGiAccess.getInstance()
                .setServletContainerInitializers(Collections.emptyList());

        // In case new attributes are added to OsgiServletContext, they should
        // also be set to null here
        OSGiAccess.getInstance().getOsgiServletContext()
                .setAttribute(ApplicationRouteRegistry.ApplicationRouteRegistryWrapper.class.getName(), null);

        registry = ApplicationRouteRegistry.getInstance(
                new VaadinServletContext(Mockito.mock(ServletContext.class)));

        osgiCollectorRegistry = ApplicationRouteRegistry.getInstance(
                new VaadinServletContext(
                        OSGiAccess.getInstance().getOsgiServletContext()));
    }

    @Test
    public void assertFakeOsgiServletContext() {
        Assert.assertNotNull(OSGiAccess.getInstance().getOsgiServletContext());
        Assert.assertNotNull(osgiCollectorRegistry);
    }

    @Test
    public void initializedRoutes_registryIsEmpty_registryIsInitializedFromOSGi() {
        getInitializationRegistry().clean();
        getInitializationRegistry().setRoute("foo", RouteComponent1.class,
                Collections.emptyList());
        getInitializationRegistry().setRoute("bar", RouteComponent2.class,
                Collections.singletonList(MainLayout.class));

        List<RouteData> routes = getTestedRegistry().getRegisteredRoutes();
        Assert.assertEquals(2, routes.size());

        RouteData data = routes.get(0);
        Assert.assertEquals("bar", data.getUrl());
        Assert.assertEquals(RouteComponent2.class, data.getNavigationTarget());
        Assert.assertEquals(Collections.singletonList(MainLayout.class),
                data.getParentLayouts());

        data = routes.get(1);
        Assert.assertEquals("foo", data.getUrl());
        Assert.assertEquals(RouteComponent1.class, data.getNavigationTarget());
        Assert.assertEquals(Collections.emptyList(), data.getParentLayouts());

        Assert.assertEquals(Optional.of(RouteComponent1.class),
                getTestedRegistry().getNavigationTarget("foo"));
        Assert.assertEquals(Optional.of(RouteComponent2.class),
                getTestedRegistry().getNavigationTarget("bar"));

        Assert.assertTrue(getTestedRegistry()
                .getRouteLayouts("foo", RouteComponent1.class).isEmpty());
        Assert.assertEquals(Collections.singletonList(MainLayout.class),
                getTestedRegistry().getRouteLayouts("bar",
                        RouteComponent2.class));
    }

    @Test
    public void initializedRoutes_registryIsNotEmpty_registryIsNotInitializedFromOSGi() {
        getInitializationRegistry().clean();
        getTestedRegistry().setRoute("foo", RouteComponent2.class,
                Collections.singletonList(MainLayout.class));

        getInitializationRegistry().setRoute("bar", RouteComponent1.class,
                Collections.emptyList());

        List<RouteData> routes = getTestedRegistry().getRegisteredRoutes();
        Assert.assertEquals(2, routes.size());

        Optional<RouteData> fooRoute = routes.stream()
                .filter(routeData -> "foo".equals(routeData.getUrl()))
                .findFirst();
        Assert.assertTrue(
                "After mixing new routes from OSGiDataCollector, the existing routes should remain in the route registry.",
                fooRoute.isPresent());
        Assert.assertEquals("foo", fooRoute.get().getUrl());
        Assert.assertEquals(RouteComponent2.class,
                fooRoute.get().getNavigationTarget());
        Assert.assertEquals(Collections.singletonList(MainLayout.class),
                fooRoute.get().getParentLayouts());

        Assert.assertEquals(Optional.of(RouteComponent2.class),
                getTestedRegistry().getNavigationTarget("foo"));

        Assert.assertEquals(Collections.singletonList(MainLayout.class),
                getTestedRegistry().getRouteLayouts("foo",
                        RouteComponent2.class));

        Optional<RouteData> barRoute = routes.stream()
                .filter(routeData -> "bar".equals(routeData.getUrl()))
                .findFirst();
        Assert.assertTrue(
                "After mixing new routes from OSGiDataCollector, the new routes should be added to the route registry.",
                barRoute.isPresent());
        Assert.assertEquals("bar", barRoute.get().getUrl());
        Assert.assertEquals(RouteComponent1.class,
                barRoute.get().getNavigationTarget());
        Assert.assertEquals(Collections.emptyList(),
                barRoute.get().getParentLayouts());

        Assert.assertEquals(Optional.of(RouteComponent1.class),
                getTestedRegistry().getNavigationTarget("bar"));

        Assert.assertEquals(Collections.emptyList(), getTestedRegistry()
                .getRouteLayouts("bar", RouteComponent1.class));
    }

    @Test
    public void removeRoute_registryIsInitializedFromOSGi_registryIsModified_routesAreRemoved() {
        getInitializationRegistry().clean();
        getInitializationRegistry().setRoute("foo", RouteComponent1.class,
                Collections.emptyList());

        getTestedRegistry().setRoute("bar", RouteComponent2.class,
                Collections.singletonList(MainLayout.class));

        getTestedRegistry().setRoute("baz", RouteComponent1.class,
                Collections.singletonList(MainLayout.class));

        // this removes the route that has been added from the OSGi
        // initialization registry
        getTestedRegistry().removeRoute("foo");
        // this removes the route form the registry itself
        getTestedRegistry().removeRoute(RouteComponent2.class);

        List<RouteData> routes = getTestedRegistry().getRegisteredRoutes();
        Assert.assertEquals(1, routes.size());

        RouteData data = routes.get(0);
        Assert.assertEquals("baz", data.getUrl());
        Assert.assertEquals(RouteComponent1.class, data.getNavigationTarget());

        Assert.assertEquals(Optional.of(RouteComponent1.class),
                getTestedRegistry().getNavigationTarget("baz"));

        Assert.assertEquals(Collections.singletonList(MainLayout.class),
                getTestedRegistry().getRouteLayouts("baz",
                        RouteComponent1.class));
    }

    @Test
    public void initializedRoutesTwice_registryIsEmpty_registryIsInitializedFromOSGi() {
        getInitializationRegistry().clean();
        getInitializationRegistry().setRoute("foo", RouteComponent1.class,
                Collections.emptyList());
        getInitializationRegistry().setRoute("bar", RouteComponent2.class,
                Collections.emptyList());
        getInitializationRegistry().clean();
        getInitializationRegistry().setRoute("foo", RouteComponent3.class,
                Collections.emptyList());
        getInitializationRegistry().setRoute("xyz", RouteComponent4.class,
                Collections.emptyList());

        List<RouteData> routes = getTestedRegistry().getRegisteredRoutes();
        Assert.assertEquals(2, routes.size());

        Optional<RouteData> fooRoute1 = routes.stream()
                .filter(routeData -> "foo".equals(routeData.getUrl()))
                .findFirst();
        Assert.assertTrue(fooRoute1.isPresent());
        Assert.assertEquals(RouteComponent3.class,
                fooRoute1.get().getNavigationTarget());
        Assert.assertEquals(Collections.emptyList(),
                fooRoute1.get().getParentLayouts());
        Assert.assertEquals(Optional.of(RouteComponent3.class),
                getTestedRegistry().getNavigationTarget("foo"));
        Assert.assertTrue(getTestedRegistry()
                .getRouteLayouts("foo", RouteComponent3.class).isEmpty());

        Optional<RouteData> xyzRoute1 = routes.stream()
                .filter(routeData -> "xyz".equals(routeData.getUrl()))
                .findFirst();
        Assert.assertTrue(xyzRoute1.isPresent());
        Assert.assertEquals(RouteComponent4.class,
                xyzRoute1.get().getNavigationTarget());
        Assert.assertEquals(Collections.emptyList(),
                xyzRoute1.get().getParentLayouts());
        Assert.assertEquals(Optional.of(RouteComponent4.class),
                getTestedRegistry().getNavigationTarget("xyz"));
        Assert.assertTrue(getTestedRegistry()
                .getRouteLayouts("xyz", RouteComponent4.class).isEmpty());
    }

    @Test
    public void initializedRoutes_modifyingRegistry_registryIsModifiedFromOSGi() {
        getInitializationRegistry().clean();
        getInitializationRegistry().setRoute("foo", RouteComponent1.class,
                Collections.emptyList());
        getInitializationRegistry().setRoute("bar", RouteComponent2.class,
                Collections.emptyList());

        getTestedRegistry().getRegisteredRoutes();

        // Modifying OsgiRouteRegistry and OSGiDataCollector
        getTestedRegistry().removeRoute("foo");
        getTestedRegistry().setRoute("xyz", RouteComponent3.class,
                Collections.emptyList());
        getInitializationRegistry().clean();
        getInitializationRegistry().setRoute("abc", RouteComponent4.class,
                Collections.emptyList());

        List<RouteData> routesAfterCollectorChanges = getTestedRegistry()
                .getRegisteredRoutes();
        Assert.assertEquals(2, routesAfterCollectorChanges.size());

        Optional<RouteData> xyzRoute = routesAfterCollectorChanges.stream()
                .filter(routeData -> "xyz".equals(routeData.getUrl()))
                .findFirst();
        Assert.assertTrue(xyzRoute.isPresent());
        Assert.assertEquals(RouteComponent3.class,
                xyzRoute.get().getNavigationTarget());
        Assert.assertEquals(Collections.emptyList(),
                xyzRoute.get().getParentLayouts());

        Optional<RouteData> abcRoute = routesAfterCollectorChanges.stream()
                .filter(routeData -> "abc".equals(routeData.getUrl()))
                .findFirst();
        Assert.assertTrue(abcRoute.isPresent());
        Assert.assertEquals(RouteComponent4.class,
                abcRoute.get().getNavigationTarget());
        Assert.assertEquals(Collections.emptyList(),
                abcRoute.get().getParentLayouts());
    }

    @Test
    public void initializedRoutes_registryCreatedAfterRoutesAdded_registryIsInitializedFromOSGi() {
        getInitializationRegistry().clean();
        getInitializationRegistry().setRoute("foo", RouteComponent1.class,
                Collections.emptyList());
        getInitializationRegistry().setRoute("f", RouteComponent1.class,
                Collections.emptyList());
        getInitializationRegistry().setRoute("bar", RouteComponent2.class,
                Collections.singletonList(MainLayout.class));

        ApplicationRouteRegistry anotherRegistry = ApplicationRouteRegistry
                .getInstance(new VaadinServletContext(
                        Mockito.mock(ServletContext.class)));

        List<RouteData> routes = anotherRegistry.getRegisteredRoutes();
        Assert.assertEquals(2, routes.size());

        Optional<RouteData> barRoute = routes.stream()
                .filter(routeData -> "bar".equals(routeData.getUrl()))
                .findFirst();
        Assert.assertTrue(barRoute.isPresent());
        Assert.assertEquals(RouteComponent2.class,
                barRoute.get().getNavigationTarget());
        Assert.assertEquals(Collections.singletonList(MainLayout.class),
                barRoute.get().getParentLayouts());

        Optional<RouteData> fooRoute = routes.stream()
                .filter(routeData -> "foo".equals(routeData.getUrl()))
                .findFirst();
        Assert.assertTrue(fooRoute.isPresent());
        Assert.assertEquals(RouteComponent1.class,
                fooRoute.get().getNavigationTarget());
        Assert.assertEquals(Collections.emptyList(),
                fooRoute.get().getParentLayouts());
        Assert.assertNotNull(fooRoute.get().getRouteAliases());
        Assert.assertEquals(1, fooRoute.get().getRouteAliases().size());
        Assert.assertEquals("f",
                fooRoute.get().getRouteAliases().get(0).getUrl());
        Assert.assertEquals(RouteComponent1.class,
                fooRoute.get().getRouteAliases().get(0).getNavigationTarget());
        Assert.assertEquals(Collections.emptyList(),
                fooRoute.get().getRouteAliases().get(0).getParentLayouts());

        Assert.assertEquals(Optional.of(RouteComponent1.class),
                anotherRegistry.getNavigationTarget("foo"));
        Assert.assertEquals(Optional.of(RouteComponent1.class),
                anotherRegistry.getNavigationTarget("f"));
        Assert.assertEquals(Optional.of(RouteComponent2.class),
                anotherRegistry.getNavigationTarget("bar"));

        Assert.assertTrue(anotherRegistry
                .getRouteLayouts("foo", RouteComponent1.class).isEmpty());
        Assert.assertTrue(anotherRegistry
                .getRouteLayouts("f", RouteComponent1.class).isEmpty());
        Assert.assertEquals(Collections.singletonList(MainLayout.class),
                anotherRegistry.getRouteLayouts("bar", RouteComponent2.class));
    }

    @Override
    protected RouteRegistry getTestedRegistry() {
        return registry;
    }

    @Override
    protected RouteRegistry getInitializationRegistry() {
        return osgiCollectorRegistry;
    }

    @Tag("div")
    private static class RouteComponent1 extends Component {
    }

    @Tag("span")
    private static class RouteComponent2 extends Component {
    }

    @Tag("span")
    private static class RouteComponent3 extends Component {
    }

    @Tag("span")
    private static class RouteComponent4 extends Component {
    }
}
