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
import java.util.Optional;

import org.junit.Before;

import com.vaadin.annotations.Route;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.Title;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

public class NewRoutingTestBase {

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
        final NewRouter router;

        public RouterTestUI() {
            this(new NewRouter());
        }

        public RouterTestUI(NewRouter router) {
            this.router = router;
        }

        @Override
        public Optional<RouterInterface> getRouter() {
            return Optional.of(router);
        }
    }

    protected NewRouter router;

    @Before
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        router = new NewRouter();
        Field field = RouteRegistry.getInstance().getClass()
                .getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(RouteRegistry.getInstance(), false);
    }
}
