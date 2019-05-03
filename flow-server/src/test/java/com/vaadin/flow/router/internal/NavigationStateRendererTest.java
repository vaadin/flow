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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationStateBuilder;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.TestRouteRegistry;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class NavigationStateRendererTest {

    Router router;

    @Before
    public void init() {
        RouteRegistry registry = ApplicationRouteRegistry
                .getInstance(Mockito.mock(ServletContext.class));
        router = new Router(registry);
    }

  @Test
    public void getRouterLayoutForSingle() throws Exception {
        NavigationStateRenderer childRenderer = new NavigationStateRenderer(
                navigationStateFromTarget(RouteParentLayout.class));

        List<Class<? extends RouterLayout>> routerLayoutTypes = childRenderer
                .getRouterLayoutTypes(RouteParentLayout.class, router);

        Assert.assertEquals(
                "Found layout even though RouteParentLayout doesn't have any parents.",
                0, routerLayoutTypes.size());
    }

    @Test
    public void getRouterLayoutForSingleParent() throws Exception {
        NavigationStateRenderer childRenderer = new NavigationStateRenderer(
                navigationStateFromTarget(SingleView.class));
        RouteConfiguration.forRegistry(router.getRegistry())
                .setAnnotatedRoute(SingleView.class);

        List<Class<? extends RouterLayout>> routerLayoutTypes = childRenderer
                .getRouterLayoutTypes(SingleView.class, router);

        Assert.assertEquals("Not all expected layouts were found", 1,
                routerLayoutTypes.size());
        Assert.assertEquals("Wrong class found", RouteParentLayout.class,
                routerLayoutTypes.get(0));
    }

    @Test
    public void getRouterLayoutForMulipleLayers() throws Exception {
        NavigationStateRenderer childRenderer = new NavigationStateRenderer(
                navigationStateFromTarget(ChildConfiguration.class));
        RouteConfiguration.forRegistry(router.getRegistry())
                .setAnnotatedRoute(ChildConfiguration.class);

        List<Class<? extends RouterLayout>> routerLayoutTypes = childRenderer
                .getRouterLayoutTypes(ChildConfiguration.class, router);

        Assert.assertEquals("Not all expected layouts were found", 2,
                routerLayoutTypes.size());
        Assert.assertEquals("Wrong class found as first in array",
                MiddleLayout.class, routerLayoutTypes.get(0));
        Assert.assertEquals("Wrong class found as second in array",
                RouteParentLayout.class, routerLayoutTypes.get(1));
    }

    @Test
    public void instantiatorUse() throws ServiceException {

        MockVaadinServletService service = new MockVaadinServletService();
        service.init(new MockInstantiator() {
            @Override
            public <T extends HasElement> T createRouteTarget(
                    Class<T> routeTargetType, NavigationEvent event) {
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

    @Test
    public void preserveOnRefreshAndWindowNameNotKnown_firstNavigationEvent_clientSideCallTriggered() {
        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a NavigationStateRenderer mapping to PreservedView
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                navigationStateFromTarget(PreservedView.class));

        // given the session has a cache of something at this location
        AbstractNavigationStateRenderer.setPreservedComponent(session, "",
                new Location("preserved"), Mockito.mock(Component.class));

        // given a UI that contain no window name with an instrumented Page
        // that records JS invocations
        AtomicBoolean jsInvoked = new AtomicBoolean(false);
        MockUI ui = new MockUI(session) {
            final Page page = new Page(this) {
                @Override
                public PendingJavaScriptResult executeJs(
                        String expression, Serializable... params) {
                    jsInvoked.set(true);
                    return super.executeJs(expression, params);
                }
            };

            @Override
            public Page getPage() {
                return page;
            }
        };

        // when a navigation event reaches the renderer
        renderer.handle(new NavigationEvent(
                new Router(new TestRouteRegistry()),
                new Location("preserved"), ui, NavigationTrigger.PAGE_LOAD));

        // then client-side JS was invoked
        Assert.assertTrue("Expected JS invocation", jsInvoked.get());
    }

    @Test
    public void preserveOnRefreshAndWindowNameKnown_navigationEvents_componentIsCachedRetrievedAndFlushed() {
        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a UI that contain a window name ROOT.123
        MockUI ui1 = new MockUI(session);
        ExtendedClientDetails details =
                Mockito.mock(ExtendedClientDetails.class);
        Mockito.when(details.getWindowName()).thenReturn("ROOT.123");
        ui1.getInternals().setExtendedClientDetails(details);

        // given a NavigationStateRenderer mapping to PreservedView
        NavigationStateRenderer renderer1 = new NavigationStateRenderer(
                navigationStateFromTarget(PreservedView.class));

        // when a navigation event reaches the renderer
        renderer1.handle(new NavigationEvent(
                new Router(new TestRouteRegistry()),
                new Location("preserved"), ui1, NavigationTrigger.PAGE_LOAD));

        // then the session has a cached record of the view
        Assert.assertTrue("Session expected to have cached view",
                AbstractNavigationStateRenderer.getPreservedComponent(session,
                        "ROOT.123", new Location("preserved"))
                        .isPresent());

        // given the recently instantiated view
        final Component view = (Component) ui1.getInternals()
                .getActiveRouterTargetsChain().get(0);

        // given a new UI with the same window name
        MockUI ui2 = new MockUI(session);
        ui2.getInternals().setExtendedClientDetails(details);

        // given a new NavigationStateRenderer mapping to PreservedView
        NavigationStateRenderer renderer2 = new NavigationStateRenderer(
                navigationStateFromTarget(PreservedView.class));

        // when another navigation targets the same location
        renderer2.handle(new NavigationEvent(
                new Router(new TestRouteRegistry()),
                new Location("preserved"), ui2, NavigationTrigger.PAGE_LOAD));

        // then the same view is routed to
        Assert.assertEquals("Expected same view",
                view, ui1.getInternals().getActiveRouterTargetsChain().get(0));

        // given yet another new UI with the same window name
        MockUI ui3 = new MockUI(session);
        ui3.getInternals().setExtendedClientDetails(details);

        // given a new NavigationStateRenderer mapping to another location
        NavigationStateRenderer renderer3 = new NavigationStateRenderer(
                navigationStateFromTarget(RegularView.class));

        // when a navigation event targets that other location
        renderer3.handle(new NavigationEvent(
                new Router(new TestRouteRegistry()),
                new Location("regular"), ui2, NavigationTrigger.PAGE_LOAD));

        // then session no longer has a cached record at location "preserved"
        Assert.assertFalse("Session expected to not have cached view",
                AbstractNavigationStateRenderer.hasPreservedComponentOfLocation(
                        session, new Location("preserved")));
    }

    @Route(value = "preserved")
    @PreserveOnRefresh
    private static class PreservedView extends Text {
        public PreservedView() {
            super("abc");
        }
    }

    @Route(value = "regular")
    private static class RegularView extends Text {
        public RegularView() {
            super("def");
        }
    }

    private MockVaadinServletService createMockServiceWithInstantiator() {
        MockVaadinServletService service = new MockVaadinServletService();
        service.init(new MockInstantiator() {
            @Override
            public <T extends HasElement> T createRouteTarget(
                    Class<T> routeTargetType, NavigationEvent event) {
                try {
                    return routeTargetType.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return service;
    }

    private NavigationState navigationStateFromTarget(
            Class<? extends Component> target) {
        return new NavigationStateBuilder(router).withTarget(target).build();
    }
}
