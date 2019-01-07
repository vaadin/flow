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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.osgi.OSGiAccess;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Tests for {@link ApplicationRouteRegistry} instance which is initialized via
 * OSGi routes initialization logic.
 *
 */
@RunWith(EnableOSGiRunner.class)
public class OSGiInitApplicationRouteRegistryTest
        extends RouteRegistryTestBase {

    private ApplicationRouteRegistry registry;
    private RouteRegistry osgiCollectorRegistry;

    @Before
    public void init() {
        registry = ApplicationRouteRegistry
                .getInstance(Mockito.mock(ServletContext.class));
        OSGiAccess.getInstance()
                .setServletContainerInitializers(Collections.emptyList());
        osgiCollectorRegistry = ApplicationRouteRegistry
                .getInstance(OSGiAccess.getInstance().getOsgiServletContext());
    }

    @Test
    public void assertFakeOsgiServletContext() {
        Assert.assertNotNull(OSGiAccess.getInstance().getOsgiServletContext());
        Assert.assertNotNull(osgiCollectorRegistry);
    }

    @Test
    public void initalizedRoutes_registryIsEmpty_registryIsInitializedFromOSGi() {
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

        System.out.println("getRouteLayouts foo: " + getTestedRegistry()
                .getRouteLayouts("foo", RouteComponent1.class));
        Assert.assertTrue(getTestedRegistry()
                .getRouteLayouts("foo", RouteComponent1.class).isEmpty());
        Assert.assertEquals(Collections.singletonList(MainLayout.class),
                getTestedRegistry().getRouteLayouts("bar",
                        RouteComponent2.class));
    }

    @Test
    public void initalizedRoutes_registryIsNotEmpty_registryIsNotInitializedFromOSGi() {
        getTestedRegistry().setRoute("foo", RouteComponent2.class,
                Collections.singletonList(MainLayout.class));

        getInitializationRegistry().clean();
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

}
