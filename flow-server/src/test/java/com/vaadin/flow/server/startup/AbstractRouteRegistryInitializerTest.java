/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.server.InvalidRouteConfigurationException;
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

    @Route("foo")
    public static class NonComponent {

    }

    @Test(expected = InvalidRouteLayoutConfigurationException.class)
    public void routeAndParentLayout_notRouterLayout_throws() {
        initializer.validateRouteClasses(Stream.of(RouteAndParentLayout.class));

    }

    @Test
    public void validateRouteClasses_annotationOnNonComponentClass_throws() {
        InvalidRouteConfigurationException exception = Assert.assertThrows(
                InvalidRouteConfigurationException.class, () -> initializer
                        .validateRouteClasses(Stream.of(NonComponent.class)));
        Assert.assertTrue(
                exception.getMessage().contains(Route.class.getSimpleName()));
        Assert.assertTrue(exception.getMessage()
                .contains("not extend '" + Component.class.getCanonicalName()));
    }

    @Test
    public void routeAndParentLayout_routerLayout_returnsValidatedClass() {
        Set<Class<? extends Component>> classes = initializer
                .validateRouteClasses(
                        Stream.of(RouteAndParentRouterLayout.class));
        Assert.assertEquals(1, classes.size());
        Assert.assertEquals(RouteAndParentRouterLayout.class,
                classes.iterator().next());
    }

}
