/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.router;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.annotations.Route;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.Title;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentUtil;
import com.vaadin.ui.UI;

public class NEW_RouterTest {

    private NEW_Router router;
    private UI ui;

    @Route("")
    @Tag(Tag.DIV)
    public static class RootNavigationTarget extends Component {
    }

    @Route("foo")
    @Tag(Tag.DIV)
    public static class FooNavigationTarget extends Component {
    }

    @Route("foo/bar")
    @Tag(Tag.DIV)
    public static class FooBarNavigationTarget extends Component {
    }

    @Route("navigation-target-with-title")
    @Title("Custom Title")
    @Tag(Tag.DIV)
    public static class NavigationTargetWithTitle extends Component {
    }

    public static class RouterTestUI extends UI {
        final NEW_Router router;

        public RouterTestUI() {
            this(new NEW_Router());
        }

        public RouterTestUI(NEW_Router router) {
            this.router = router;
        }

        @Override
        public Optional<NEW_RouterInterface> getRouter() {
            return Optional.of(router);
        }
    }

    @Before
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        router = new NEW_Router();
        ui = new RouterTestUI(router);
        Field field = RouteRegistry.getInstance().getClass()
                .getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(RouteRegistry.getInstance(), false);
    }

    @Test
    public void basic_navigation() throws InvalidRouteConfigurationException {
        RouteRegistry.getInstance()
                .setNavigationTargets(Stream.of(RootNavigationTarget.class,
                        FooNavigationTarget.class, FooBarNavigationTarget.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals(RootNavigationTarget.class, getUIComponent());

        router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals(FooNavigationTarget.class, getUIComponent());

        router.navigate(ui, new Location("foo/bar"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals(FooBarNavigationTarget.class, getUIComponent());
    }

    @Test
    public void page_title_set_from_annotation()
            throws InvalidRouteConfigurationException {
        RouteRegistry.getInstance().setNavigationTargets(
                Collections.singleton(NavigationTargetWithTitle.class));
        router.navigate(ui, new Location("navigation-target-with-title"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Custom Title", ui.getInternals().getTitle());
    }

    private Class<? extends Component> getUIComponent() {
        return ComponentUtil.findParentComponent(ui.getElement().getChild(0))
                .get().getClass();
    }
}
