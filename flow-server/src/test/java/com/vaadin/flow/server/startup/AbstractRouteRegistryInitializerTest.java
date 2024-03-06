/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;

public class AbstractRouteRegistryInitializerTest {

    private AbstractRouteRegistryInitializer initializer = new AbstractRouteRegistryInitializer() {

    };

    @Tag(Tag.DIV)
    public static class TestParentLayout extends Component
            implements RouterLayout {

    }

    @Tag(Tag.DIV)
    @Route("foo")
    @ParentLayout(TestParentLayout.class)
    public static class RouteAndParentLayout extends Component {

    }

    @Tag(Tag.DIV)
    @Route("foo")
    @ParentLayout(TestParentLayout.class)
    public static class RouteAndParentRouterLayout extends Component
            implements RouterLayout {

    }

    @Test(expected = InvalidRouteLayoutConfigurationException.class)
    public void routeAndParentLayout_notRouterLayout_throws() {
        initializer.validateRouteClasses(null,
                Stream.of(RouteAndParentLayout.class));

    }

    @Test
    public void routeAndParentLayout_routerLayout_returnsValidatedClass() {
        Set<Class<? extends Component>> classes = initializer
                .validateRouteClasses(null,
                        Stream.of(RouteAndParentRouterLayout.class));
        Assert.assertEquals(1, classes.size());
        Assert.assertEquals(RouteAndParentRouterLayout.class,
                classes.iterator().next());
    }

}
