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
package com.vaadin.flow.server.startup;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.VaadinContext;

public class AbstractRouteRegistryInitializerTest {

    private AbstractRouteRegistryInitializer initializer = new AbstractRouteRegistryInitializer() {

    };

    VaadinContext context = Mockito.mock(VaadinContext.class);

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

    @Test(expected = InvalidRouteLayoutConfigurationException.class)
    public void routeAndParentLayout_notRouterLayout_throws() {
        initializer.validateRouteClasses(context,
                Stream.of(RouteAndParentLayout.class));
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
