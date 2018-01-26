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
package com.vaadin.flow.router.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.tests.util.MockUI;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

@NotThreadSafe
public class NavigationStateRendererTest {

    @Test
    public void getRouterLayoutForSingle() {
        NavigationStateRenderer childRenderer = new NavigationStateRenderer(
                navigationStateFromTarget(RouteParentLayout.class));

        List<Class<? extends RouterLayout>> routerLayoutTypes = childRenderer
                .getRouterLayoutTypes(RouteParentLayout.class);

        Assert.assertEquals(
                "Found layout even though RouteParentLayout doesn't have any parents.",
                0, routerLayoutTypes.size());
    }

    @Test
    public void getRouterLayoutForSingleParent() {
        NavigationStateRenderer childRenderer = new NavigationStateRenderer(
                navigationStateFromTarget(SingleView.class));

        List<Class<? extends RouterLayout>> routerLayoutTypes = childRenderer
                .getRouterLayoutTypes(SingleView.class);

        Assert.assertEquals("Not all expected layouts were found", 1,
                routerLayoutTypes.size());
        Assert.assertEquals("Wrong class found", RouteParentLayout.class,
                routerLayoutTypes.get(0));
    }

    @Test
    public void getRouterLayoutForMulipleLayers() {
        NavigationStateRenderer childRenderer = new NavigationStateRenderer(
                navigationStateFromTarget(ChildConfiguration.class));

        List<Class<? extends RouterLayout>> routerLayoutTypes = childRenderer
                .getRouterLayoutTypes(ChildConfiguration.class);

        Assert.assertEquals("Not all expected layouts were found", 2,
                routerLayoutTypes.size());
        Assert.assertEquals("Wrong class found as first in array",
                MiddleLayout.class, routerLayoutTypes.get(0));
        Assert.assertEquals("Wrong class found as second in array",
                RouteParentLayout.class, routerLayoutTypes.get(1));
    }

    @Test
    public void instantiatorUse() {

        MockVaadinServletService service = new MockVaadinServletService();
        service.init(new MockInstantiator() {
            @Override
            public <T> T getOrCreate(Class<T> routeTargetType) {
                Assert.assertEquals(Component.class, routeTargetType);
                return (T) new Text("foo");
            }
        });
        MockUI ui = new MockUI(new MockVaadinSession(service));

        NavigationEvent event = new NavigationEvent(
                new Router(new TestRouteRegistry()), new Location(""), ui,
                NavigationTrigger.PAGE_LOAD);
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                navigationStateFromTarget(ChildConfiguration.class));

        Component routeTarget = renderer.getRouteTarget(Component.class, event);

        Assert.assertEquals(Text.class, routeTarget.getClass());

        UI.setCurrent(null);
    }

    @Route("parent")
    private static class RouteParentLayout extends Component
            implements RouterLayout {
    }

    @ParentLayout(RouteParentLayout.class)
    private static class MiddleLayout extends Component
            implements RouterLayout {

    }

    @Route(value = "child", layout = MiddleLayout.class)
    private static class ChildConfiguration extends Component {
    }

    @Route(value = "single", layout = RouteParentLayout.class)
    private static class SingleView extends Component {
    }

    private NavigationState navigationStateFromTarget(
            Class<? extends Component> target) {
        return new NavigationStateBuilder().withTarget(target).build();
    }
}