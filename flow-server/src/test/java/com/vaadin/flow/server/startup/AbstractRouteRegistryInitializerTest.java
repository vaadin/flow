/*
 * Copyright 2000-2020 Vaadin Ltd.
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
        initializer.validateRouteClasses(Stream.of(RouteAndParentLayout.class));

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
