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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.router.DefaultRoutePathProvider;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RoutePathProvider;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.VaadinContext;

public class AbstractRouteRegistryInitializerTest {

    private AbstractRouteRegistryInitializer initializer = new AbstractRouteRegistryInitializer() {

    };

    VaadinContext context = Mockito.mock(VaadinContext.class);

    @Before
    public void setUp() throws Exception {
        Lookup lookup = Lookup.of(new DefaultRoutePathProvider(),
                RoutePathProvider.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
    }

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

    @Tag(Tag.DIV)
    @Route("foo")
    @RouteAlias("foo")
    public static class RouteAndAliasWithSamePath extends Component {

    }

    @Tag(Tag.DIV)
    @RoutePrefix("parent")
    public static class PrefixedParentLayout extends Component
            implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @RoutePrefix("nested")
    @ParentLayout(PrefixedParentLayout.class)
    public static class NestedPrefixedParentLayout extends Component
            implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @Route("foo")
    @RouteAlias(value = "foo", layout = PrefixedParentLayout.class)
    public static class RouteAndAliasWithSamePathDifferentLayoutPrefix
            extends Component {

    }

    @Tag(Tag.DIV)
    @Route(value = "foo", layout = PrefixedParentLayout.class)
    @RouteAlias(value = "foo", layout = PrefixedParentLayout.class)
    public static class RouteAndAliasWithSamePathSameLayoutPrefix
            extends Component {

    }

    @Tag(Tag.DIV)
    @Route(value = "foo", layout = NestedPrefixedParentLayout.class)
    @RouteAlias(value = "foo", layout = NestedPrefixedParentLayout.class)
    public static class RouteAndAliasWithSamePathSameNestedLayoutPrefix
            extends Component {

    }

    @Tag(Tag.DIV)
    @Route("foo")
    @RouteAlias("bar")
    @RouteAlias("baz")
    @RouteAlias("bar")
    @RouteAlias("baz")
    @RouteAlias("hey")
    public static class AliasesWithSamePath extends Component {

    }

    @Route("foo")
    public static class NonComponent {

    }

    @Test(expected = InvalidRouteLayoutConfigurationException.class)
    public void routeAndParentLayout_notRouterLayout_throws() {
        initializer.validateRouteClasses(context,
                Stream.of(RouteAndParentLayout.class));
    }

    @Test
    public void validateRouteClasses_annotationOnNonComponentClass_throws() {
        InvalidRouteConfigurationException exception = Assert.assertThrows(
                InvalidRouteConfigurationException.class,
                () -> initializer.validateRouteClasses(context,
                        Stream.of(NonComponent.class)));
        Assert.assertTrue(containsQuotedAnnotationName(exception.getMessage(),
                Route.class));
        Assert.assertTrue(exception.getMessage()
                .contains("not extend '" + Component.class.getCanonicalName()));
    }

    @Test
    public void validateRouteClasses_samePathForRouteAndAlias_throws() {
        InvalidRouteConfigurationException exception = Assert.assertThrows(
                InvalidRouteConfigurationException.class,
                () -> initializer.validateRouteClasses(context,
                        Stream.of(RouteAndAliasWithSamePath.class)));
        Assert.assertTrue(containsQuotedAnnotationName(exception.getMessage(),
                Route.class));
        Assert.assertTrue(containsQuotedAnnotationName(exception.getMessage(),
                RouteAlias.class));
        Assert.assertTrue(exception.getMessage().contains("same path"));
        Assert.assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void validateRouteClasses_samePathForRepeatableAlias_throws() {
        InvalidRouteConfigurationException exception = Assert.assertThrows(
                InvalidRouteConfigurationException.class,
                () -> initializer.validateRouteClasses(context,
                        Stream.of(AliasesWithSamePath.class)));
        Assert.assertFalse(containsQuotedAnnotationName(exception.getMessage(),
                Route.class));
        Assert.assertTrue(containsQuotedAnnotationName(exception.getMessage(),
                RouteAlias.class));
        Assert.assertTrue(exception.getMessage().contains("same paths"));
        Assert.assertTrue(exception.getMessage().contains("bar"));
        Assert.assertTrue(exception.getMessage().contains("baz"));
        Assert.assertFalse(exception.getMessage().contains("foo"));
        Assert.assertFalse(exception.getMessage().contains("hey"));
    }

    @Test
    public void validateRouteClasses_samePathForRouteAndAlias_sameLayoutPrefix_throws() {
        InvalidRouteConfigurationException exception = Assert.assertThrows(
                InvalidRouteConfigurationException.class,
                () -> initializer.validateRouteClasses(context, Stream
                        .of(RouteAndAliasWithSamePathSameLayoutPrefix.class)));
        Assert.assertTrue(containsQuotedAnnotationName(exception.getMessage(),
                Route.class));
        Assert.assertTrue(containsQuotedAnnotationName(exception.getMessage(),
                RouteAlias.class));
        Assert.assertTrue(exception.getMessage().contains("same path"));
        Assert.assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void validateRouteClasses_samePathForRouteAndAlias_sameNestedLayoutPrefix_throws() {
        InvalidRouteConfigurationException exception = Assert.assertThrows(
                InvalidRouteConfigurationException.class,
                () -> initializer.validateRouteClasses(context, Stream.of(
                        RouteAndAliasWithSamePathSameNestedLayoutPrefix.class)));
        Assert.assertTrue(containsQuotedAnnotationName(exception.getMessage(),
                Route.class));
        Assert.assertTrue(containsQuotedAnnotationName(exception.getMessage(),
                RouteAlias.class));
        Assert.assertTrue(exception.getMessage().contains("same path"));
        Assert.assertTrue(exception.getMessage().contains("foo"));
    }

    @Test
    public void validateRouteClasses_samePathForRouteAndAlias_differentLayoutPrefix_doNotThrow() {
        Set<Class<? extends Component>> classes = initializer
                .validateRouteClasses(context, Stream.of(
                        RouteAndAliasWithSamePathDifferentLayoutPrefix.class));
        Assert.assertEquals(1, classes.size());
        Assert.assertEquals(
                RouteAndAliasWithSamePathDifferentLayoutPrefix.class,
                classes.iterator().next());
    }

    @Test
    public void routeAndParentLayout_routerLayout_returnsValidatedClass() {
        Set<Class<? extends Component>> classes = initializer
                .validateRouteClasses(context,
                        Stream.of(RouteAndParentRouterLayout.class));
        Assert.assertEquals(1, classes.size());
        Assert.assertEquals(RouteAndParentRouterLayout.class,
                classes.iterator().next());
    }

    private static boolean containsQuotedAnnotationName(String message,
            Class<?> clazz) {
        return message.contains("'@" + clazz.getSimpleName() + "'");
    }
}
