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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.BaseJsonNode;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationStateBuilder;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.TestRouteRegistry;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.menu.AvailableViewInfo;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

@NotThreadSafe
public class NavigationStateRendererTest {

    @Route(value = "preserved")
    @PreserveOnRefresh
    private static class PreservedView extends Text {
        PreservedView() {
            super("");
        }
    }

    @Route(value = "preserved")
    @PreserveOnRefresh
    private static class PreservedEventView extends Text
            implements BeforeEnterObserver, AfterNavigationObserver {
        static boolean refreshBeforeEnter;
        static boolean refreshAfterNavigation;

        PreservedEventView() {
            super("");
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            refreshBeforeEnter = event.isRefreshEvent();
        }

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            refreshAfterNavigation = event.isRefreshEvent();

        }
    }

    @Route(value = "regular")
    public static class RegularView extends Text {
        public RegularView() {
            super("");
        }
    }

    @Route(value = "preservedLayout")
    @Tag("div")
    private static class PreservedLayout extends Component
            implements RouterLayout {
        PreservedLayout() {
            addAttachListener(e -> layoutAttachCount.getAndIncrement());
            layoutUUID = UUID.randomUUID().toString();
        }
    }

    @PreserveOnRefresh
    @Route(value = "preservedNested", layout = PreservedLayout.class)
    private static class PreservedNestedView extends Text {
        PreservedNestedView() {
            super("");
            addAttachListener(e -> viewAttachCount.getAndIncrement());
            viewUUID = UUID.randomUUID().toString();
        }
    }

    @Route(value = "proxyable")
    public static class ProxyableView extends Text {
        public ProxyableView() {
            super("");
        }
    }

    private Router router;

    @Before
    public void init() {
        RouteRegistry registry = ApplicationRouteRegistry
                .getInstance(new MockVaadinContext());
        router = new Router(registry);
    }

    @Test
    public void getRouterLayoutForSingle() {
        NavigationStateRenderer childRenderer = new NavigationStateRenderer(
                navigationStateFromTarget(RouteParentLayout.class));

        List<Class<? extends RouterLayout>> routerLayoutTypes = childRenderer
                .getRouterLayoutTypes(RouteParentLayout.class, router);

        Assert.assertEquals(
                "Found layout even though RouteParentLayout doesn't have any parents.",
                0, routerLayoutTypes.size());
    }

    @Test
    public void getRouterLayoutForSingleParent() {
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
    public void getRouterLayoutForMulipleLayers() {
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
    public void instantiatorUse() {

        MockVaadinServletService service = new MockVaadinServletService();
        service.init(new MockInstantiator() {
            @Override
            public <T extends HasElement> T createRouteTarget(
                    Class<T> routeTargetType, NavigationEvent event) {
                Assert.assertEquals(Component.class, routeTargetType);
                return (T) new Text("foo");
            }
        });
        MockUI ui = new MockUI(new AlwaysLockedVaadinSession(service));

        NavigationEvent event = new NavigationEvent(
                new Router(new TestRouteRegistry()), new Location(""), ui,
                NavigationTrigger.PAGE_LOAD);
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                navigationStateFromTarget(ChildConfiguration.class));

        Component routeTarget = renderer.getRouteTarget(Component.class, event,
                true);

        Assert.assertEquals(Text.class, routeTarget.getClass());

        UI.setCurrent(null);
    }

    @Test
    public void getRouteTarget_supportsProxyClasses() {
        try {
            Class<? extends ProxyableView> routeProxyClass = new ByteBuddy()
                    .subclass(ProxyableView.class)
                    .modifiers(Visibility.PUBLIC, SyntheticState.SYNTHETIC)
                    .make().load(ProxyableView.class.getClassLoader(),
                            ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();

            AtomicInteger routeCreationCounter = new AtomicInteger(0);
            MockVaadinServletService service = new MockVaadinServletService();
            service.init(new MockInstantiator() {
                @Override
                public <T extends HasElement> T createRouteTarget(
                        Class<T> routeTargetType, NavigationEvent event) {
                    Assert.assertEquals(ProxyableView.class, routeTargetType);
                    routeCreationCounter.incrementAndGet();
                    return (T) ReflectTools.createInstance(routeProxyClass);
                }
            });
            DeploymentConfiguration configuration = Mockito
                    .mock(DeploymentConfiguration.class);
            AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                    service) {
                @Override
                public DeploymentConfiguration getConfiguration() {
                    return configuration;
                }
            };
            Mockito.when(configuration.isReactEnabled()).thenReturn(true);
            MockUI ui = new MockUI(session);

            NavigationEvent event = new NavigationEvent(
                    new Router(new TestRouteRegistry()), new Location("child"),
                    ui, NavigationTrigger.UI_NAVIGATE);
            NavigationStateRenderer renderer = new NavigationStateRenderer(
                    navigationStateFromTarget(ProxyableView.class));
            renderer.handle(event);
            HasElement view = ui.getInternals().getActiveRouterTargetsChain()
                    .get(0);

            Component routeTarget = renderer.getRouteTarget(ProxyableView.class,
                    event, true);

            // Getting route target should not create a new instance
            Assert.assertEquals(
                    "Only one view instance should have been created", 1,
                    routeCreationCounter.get());
            Assert.assertSame(view, routeTarget);
        } finally {
            UI.setCurrent(null);
        }
    }

    @Route("parent")
    @Tag("div")
    private static class RouteParentLayout extends Component
            implements RouterLayout {
        RouteParentLayout() {
            addAttachListener(e -> layoutAttachCount.getAndIncrement());
            layoutUUID = UUID.randomUUID().toString();
        }
    }

    @ParentLayout(RouteParentLayout.class)
    private static class MiddleLayout extends Component
            implements RouterLayout {

    }

    @Route(value = "child", layout = MiddleLayout.class)
    private static class ChildConfiguration extends Component {
    }

    @Route(value = "single", layout = RouteParentLayout.class)
    @Tag("div")
    private static class SingleView extends Component {
        SingleView() {
            addAttachListener(e -> viewAttachCount.getAndIncrement());
            viewUUID = UUID.randomUUID().toString();
        }
    }

    @Route(value = "/:samplePersonID?/:action?(edit)")
    @RouteAlias(value = "")
    @Tag("div")
    private static class RootRouteWithParam extends Component
            implements BeforeEnterObserver {
        RootRouteWithParam() {
            addAttachListener(e -> viewAttachCount.getAndIncrement());
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            beforeEnterCount.getAndIncrement();
        }
    }

    @Test
    public void handle_preserveOnRefreshAndWindowNameNotKnown_clientSideCallTriggered() {
        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a NavigationStateRenderer mapping to PreservedView
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                navigationStateFromTarget(PreservedView.class));

        // given the session has a cache of something at this location
        AbstractNavigationStateRenderer.setPreservedChain(session, "",
                new Location("preserved"), new ArrayList<>(Collections
                        .singletonList(Mockito.mock(Component.class))));

        // given a UI that contain no window name with an instrumented Page
        // that records JS invocations
        AtomicBoolean jsInvoked = new AtomicBoolean(false);
        MockUI ui = new MockUI(session) {
            final Page page = new Page(this) {
                @Override
                public PendingJavaScriptResult executeJs(String expression,
                        Serializable... params) {
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
        renderer.handle(new NavigationEvent(new Router(new TestRouteRegistry()),
                new Location("preserved"), ui, NavigationTrigger.PAGE_LOAD));

        // then client-side JS was invoked
        Assert.assertTrue("Expected JS invocation", jsInvoked.get());
    }

    @Test
    public void handle_preserveOnRefreshAndWindowNameKnown_componentIsCachedRetrievedAndFlushed() {
        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a UI that contain a window name ROOT.123
        MockUI ui1 = new MockUI(session);
        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.when(details.getWindowName()).thenReturn("ROOT.123");
        ui1.getInternals().setExtendedClientDetails(details);

        // given a NavigationStateRenderer mapping to PreservedView
        NavigationStateRenderer renderer1 = new NavigationStateRenderer(
                navigationStateFromTarget(PreservedView.class));

        // when a navigation event reaches the renderer
        renderer1.handle(new NavigationEvent(
                new Router(new TestRouteRegistry()), new Location("preserved"),
                ui1, NavigationTrigger.PAGE_LOAD));

        // then the session has a cached record of the view
        Assert.assertTrue("Session expected to have cached view",
                AbstractNavigationStateRenderer.getPreservedChain(session,
                        "ROOT.123", new Location("preserved")).isPresent());

        // given the recently instantiated view
        final Component view = ui1.getCurrentView();

        // given a new UI with the same window name
        MockUI ui2 = new MockUI(session);
        ui2.getInternals().setExtendedClientDetails(details);

        // given a new NavigationStateRenderer mapping to PreservedView
        NavigationStateRenderer renderer2 = new NavigationStateRenderer(
                navigationStateFromTarget(PreservedView.class));

        // when another navigation targets the same location
        renderer2.handle(new NavigationEvent(
                new Router(new TestRouteRegistry()), new Location("preserved"),
                ui2, NavigationTrigger.PAGE_LOAD));

        // then the same view is routed to
        Assert.assertEquals("Expected same view", view,
                ui1.getInternals().getActiveRouterTargetsChain().get(0));

        // given yet another new UI with the same window name
        MockUI ui3 = new MockUI(session);
        ui3.getInternals().setExtendedClientDetails(details);

        // given a new NavigationStateRenderer mapping to another location
        NavigationStateRenderer renderer3 = new NavigationStateRenderer(
                navigationStateFromTarget(RegularView.class));

        // when a navigation event targets that other location
        renderer3.handle(new NavigationEvent(
                new Router(new TestRouteRegistry()), new Location("regular"),
                ui2, NavigationTrigger.PAGE_LOAD));

        // then session no longer has a cached record at location "preserved"
        Assert.assertFalse("Session expected to not have cached view",
                AbstractNavigationStateRenderer.hasPreservedChainOfLocation(
                        session, new Location("preserved")));
    }

    @Test
    public void handle_preserveOnRefresh_refreshIsFlaggedInEvent() {
        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a UI that contain a window name ROOT.123
        MockUI ui = new MockUI(session);
        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.when(details.getWindowName()).thenReturn("ROOT.123");
        ui.getInternals().setExtendedClientDetails(details);

        // given a NavigationStateRenderer mapping to PreservedEventView
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                navigationStateFromTarget(PreservedEventView.class));

        // when a navigation event reaches the renderer
        renderer.handle(new NavigationEvent(new Router(new TestRouteRegistry()),
                new Location("preserved"), ui, NavigationTrigger.PAGE_LOAD));

        // then the session has a cached record of the view
        Assert.assertTrue("Session expected to have cached view",
                AbstractNavigationStateRenderer.getPreservedChain(session,
                        "ROOT.123", new Location("preserved")).isPresent());

        // given the recently instantiated view
        final PreservedEventView view = (PreservedEventView) ui.getInternals()
                .getActiveRouterTargetsChain().get(0);
        Assert.assertFalse(
                "Initial view load should not be a refresh for before",
                view.refreshBeforeEnter);
        Assert.assertFalse(
                "Initial view load should not be a refresh for after",
                view.refreshAfterNavigation);

        // when another navigation targets the same location
        renderer.handle(new NavigationEvent(new Router(new TestRouteRegistry()),
                new Location("preserved"), ui, NavigationTrigger.PAGE_LOAD));

        // then the same view is routed to
        Assert.assertEquals("Expected same view", view,
                ui.getInternals().getActiveRouterTargetsChain().get(0));

        Assert.assertTrue("Reload should be flagged for before",
                view.refreshBeforeEnter);
        Assert.assertTrue("Reload should be flagged for after",
                view.refreshAfterNavigation);
    }

    @Test
    public void handle_preserveOnRefresh_otherUIChildrenAreMoved() {
        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a NavigationStateRenderer mapping to PreservedView
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                navigationStateFromTarget(PreservedView.class));

        // given the session has a cache of PreservedView at this location
        final PreservedView view = new PreservedView();
        AbstractNavigationStateRenderer.setPreservedChain(session, "ROOT.123",
                new Location("preserved"), new ArrayList<>(List.of(view)));

        // given an old UI that contains the component and an extra element
        MockUI ui0 = new MockUI(session);
        ui0.add(view);
        final Element otherElement = new Element("div");
        ui0.getElement().insertChild(1, otherElement);

        // given a new UI after a refresh with the same window name
        MockUI ui1 = new MockUI(session);
        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.when(details.getWindowName()).thenReturn("ROOT.123");
        ui1.getInternals().setExtendedClientDetails(details);

        // when a navigation event reaches the renderer
        renderer.handle(new NavigationEvent(new Router(new TestRouteRegistry()),
                new Location("preserved"), ui1, NavigationTrigger.PAGE_LOAD));

        // then both the view element and the other element are expected to be
        // transferred from the previous UI to the new UI
        final Set<Element> uiChildren = ui1.getElement().getChildren()
                .collect(Collectors.toSet());
        Assert.assertEquals(2, uiChildren.size());
        Assert.assertTrue("Component element expected transferred",
                uiChildren.contains(view.getElement()));
        Assert.assertTrue("Extra element expected transferred",
                uiChildren.contains(otherElement));
    }

    @Test
    public void handle_preserveOnRefreshView_routerLayoutIsPreserved_oldUiIsClosed() {
        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a NavigationStateRenderer mapping to PreservedNestedView
        router = session.getService().getRouter();
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                new NavigationStateBuilder(router)
                        .withTarget(PreservedNestedView.class)
                        .withPath("preservedNested").build());
        router.getRegistry().setRoute("preservedNested",
                PreservedNestedView.class, List.of(PreservedLayout.class));

        // given the session has a cache of PreservedNestedView at this location
        final PreservedLayout layout = new PreservedLayout();
        final PreservedNestedView nestedView = new PreservedNestedView();

        MockUI previousUi = new MockUI(session);
        previousUi.add(nestedView);

        AbstractNavigationStateRenderer.setPreservedChain(session, "ROOT.123",
                new Location("preservedNested"),
                new ArrayList<>(Arrays.asList(nestedView, layout)));

        // given a UI that contain a window name ROOT.123
        MockUI ui = new MockUI(session);
        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.when(details.getWindowName()).thenReturn("ROOT.123");
        ui.getInternals().setExtendedClientDetails(details);

        // when a navigation event reaches the renderer
        renderer.handle(
                new NavigationEvent(router, new Location("preservedNested"), ui,
                        NavigationTrigger.PAGE_LOAD));

        // then the view and the router layout are preserved
        Assert.assertEquals("Expected same view", nestedView,
                ui.getInternals().getActiveRouterTargetsChain().get(0));
        Assert.assertEquals("Expected same router layout", layout,
                ui.getInternals().getActiveRouterTargetsChain().get(1));

        Assert.assertTrue(previousUi.isClosing());
    }

    @Test
    public void handle_preserveOnRefresh_sameUI_uiIsNotClosed_childrenAreNotRemoved() {
        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // the path is the same, location params will be different
        String path = "foo";

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a NavigationStateRenderer mapping to PreservedView
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                navigationStateFromTarget(PreservedView.class));

        // given the session has a cache of PreservedView at this location
        final PreservedView view = new PreservedView();
        MockUI ui = new MockUI(session);

        ui.add(view);

        AbstractNavigationStateRenderer.setPreservedChain(session, "ROOT.123",
                new Location(path,
                        new QueryParameters(Collections.singletonMap("a",
                                Collections.emptyList()))),
                new ArrayList<>(List.of(view)));

        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.when(details.getWindowName()).thenReturn("ROOT.123");
        ui.getInternals().setExtendedClientDetails(details);

        AtomicInteger count = new AtomicInteger();

        view.addDetachListener(event -> count.getAndIncrement());

        NavigationEvent event = new NavigationEvent(
                new Router(new TestRouteRegistry()),
                new Location(path,
                        new QueryParameters(Collections.singletonMap("b",
                                Collections.emptyList()))),
                ui, NavigationTrigger.ROUTER_LINK,
                new ObjectMapper().createObjectNode(), false);
        renderer.handle(event);

        Assert.assertFalse(ui.isClosing());
        Assert.assertEquals(0, count.get());
    }

    private static AtomicInteger layoutAttachCount;
    private static AtomicInteger viewAttachCount;
    private static AtomicInteger beforeEnterCount;
    private static String layoutUUID;
    private static String viewUUID;

    @Test
    public void handle_preserveOnRefreshView_refreshCurrentRouteRecreatesComponents() {
        layoutAttachCount = new AtomicInteger();
        viewAttachCount = new AtomicInteger();

        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a NavigationStateRenderer mapping to PreservedNestedView
        router = session.getService().getRouter();
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                new NavigationStateBuilder(router)
                        .withTarget(PreservedNestedView.class)
                        .withPath("preservedNested").build());
        router.getRegistry().setRoute("preservedNested",
                PreservedNestedView.class, List.of(PreservedLayout.class));

        // given a UI that contain a window name ROOT.123
        MockUI ui = new MockUI(session);
        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.when(details.getWindowName()).thenReturn("ROOT.123");
        ui.getInternals().setExtendedClientDetails(details);

        // when a navigation event reaches the renderer
        renderer.handle(
                new NavigationEvent(router, new Location("preservedNested"), ui,
                        NavigationTrigger.PAGE_LOAD));

        String currentLayoutUUID = layoutUUID;
        String currentViewUUID = viewUUID;

        Assert.assertEquals(1, layoutAttachCount.get());
        Assert.assertEquals(1, viewAttachCount.get());

        ui.getInternals().clearLastHandledNavigation();

        // Should recreate route component only
        ui.refreshCurrentRoute(false);
        Assert.assertEquals(1, layoutAttachCount.get());
        Assert.assertEquals(2, viewAttachCount.get());
        Assert.assertEquals(currentLayoutUUID, layoutUUID);
        Assert.assertNotEquals(currentViewUUID, viewUUID);
        currentViewUUID = viewUUID;

        // Should recreate route component and parent layout
        ui.refreshCurrentRoute(true);
        Assert.assertEquals(2, layoutAttachCount.get());
        Assert.assertEquals(3, viewAttachCount.get());
        Assert.assertNotEquals(currentLayoutUUID, layoutUUID);
        Assert.assertNotEquals(currentViewUUID, viewUUID);

    }

    @Test
    public void handle_normalView_refreshCurrentRouteRecreatesComponents() {
        layoutAttachCount = new AtomicInteger();
        viewAttachCount = new AtomicInteger();

        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a NavigationStateRenderer mapping to PreservedNestedView
        router = session.getService().getRouter();
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                new NavigationStateBuilder(router).withTarget(SingleView.class)
                        .withPath("single").build());
        router.getRegistry().setRoute("single", SingleView.class,
                List.of(RouteParentLayout.class));

        MockUI ui = new MockUI(session);

        renderer.handle(new NavigationEvent(router, new Location("single"), ui,
                NavigationTrigger.PAGE_LOAD));

        String currentLayoutUUID = layoutUUID;
        String currentViewUUID = viewUUID;

        Assert.assertEquals(1, layoutAttachCount.get());
        Assert.assertEquals(1, viewAttachCount.get());

        ui.getInternals().clearLastHandledNavigation();

        // Should recreate route component only
        ui.refreshCurrentRoute(false);
        Assert.assertEquals(1, layoutAttachCount.get());
        Assert.assertEquals(2, viewAttachCount.get());
        Assert.assertEquals(currentLayoutUUID, layoutUUID);
        Assert.assertNotEquals(currentViewUUID, viewUUID);
        currentViewUUID = viewUUID;

        // Should recreate route component and parent layout
        ui.refreshCurrentRoute(true);
        Assert.assertEquals(2, layoutAttachCount.get());
        Assert.assertEquals(3, viewAttachCount.get());
        Assert.assertNotEquals(currentLayoutUUID, layoutUUID);
        Assert.assertNotEquals(currentViewUUID, viewUUID);

    }

    @Test
    public void handle_clientNavigation_withMatchingFlowRoute() {
        viewAttachCount = new AtomicInteger();
        beforeEnterCount = new AtomicInteger();

        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a NavigationStateRenderer mapping to PreservedNestedView
        router = session.getService().getRouter();
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                new NavigationStateBuilder(router)
                        .withTarget(RootRouteWithParam.class).withPath("")
                        .build());
        router.getRegistry().setRoute("", RootRouteWithParam.class, null);

        MockUI ui = new MockUI(session);

        renderer.handle(new NavigationEvent(router, new Location(""), ui,
                NavigationTrigger.PAGE_LOAD));

        Assert.assertEquals(1, beforeEnterCount.get());
        Assert.assertEquals(1, viewAttachCount.get());

        ui.getInternals().clearLastHandledNavigation();

        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class, Mockito.CALLS_REAL_METHODS)) {

            menuRegistry.when(() -> MenuRegistry.getClientRoutes(true))
                    .thenReturn(Collections.singletonMap("/client-route",
                            new AvailableViewInfo("", null, false,
                                    "/client-route", false, false, null, null,
                                    null, false, null)));

            // This should not call attach or beforeEnter on root route
            renderer.handle(
                    new NavigationEvent(router, new Location("client-route"),
                            ui, NavigationTrigger.CLIENT_SIDE));

            Assert.assertEquals(1, beforeEnterCount.get());
            Assert.assertEquals(1, viewAttachCount.get());
        }
    }

    @Test
    public void handle_refreshRoute_modalComponentsDetached() {
        beforeEnterCount = new AtomicInteger();
        viewAttachCount = new AtomicInteger();

        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();

        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        // given a NavigationStateRenderer mapping to PreservedNestedView
        router = session.getService().getRouter();
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                new NavigationStateBuilder(router)
                        .withTarget(RootRouteWithParam.class).withPath("")
                        .build());
        router.getRegistry().setRoute("", RootRouteWithParam.class, null);

        @Tag("modal-component")
        class ModalComponent extends Component {
            private int attachCount;
            private int detachCount;

            @Override
            protected void onAttach(AttachEvent attachEvent) {
                attachCount++;
                super.onAttach(attachEvent);
            }

            @Override
            protected void onDetach(DetachEvent detachEvent) {
                detachCount++;
                super.onDetach(detachEvent);
            }
        }

        ModalComponent modalComponent = new ModalComponent();
        MockUI ui = new MockUI(session);
        ui.addModal(modalComponent);

        renderer.handle(new NavigationEvent(router, new Location(""), ui,
                NavigationTrigger.REFRESH_ROUTE, (BaseJsonNode) null, false,
                true, true));

        Assert.assertEquals(1, modalComponent.attachCount);
        Assert.assertEquals(1, modalComponent.detachCount);
    }

    private MockVaadinServletService createMockServiceWithInstantiator() {
        MockVaadinServletService service = new MockVaadinServletService();
        service.init(new MockInstantiator() {
            @Override
            public <T extends HasElement> T createRouteTarget(
                    Class<T> routeTargetType, NavigationEvent event) {
                try {
                    return routeTargetType.getDeclaredConstructor()
                            .newInstance();
                } catch (InstantiationException | IllegalAccessException
                        | NoSuchMethodException | InvocationTargetException e) {
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

    @Test
    // In any of the following criteria, pushState shouldn't be invoked:
    // - forwardTo is true
    // - the navigation location is the same as the current location (repeated
    // navigation)
    // - navigation trigger is PAGE_LOAD, HISTORY, or PROGRAMMATIC
    public void handle_variousInputs_checkPushStateShouldBeCalledOrNot() {
        // given a service with instantiator
        MockVaadinServletService service = createMockServiceWithInstantiator();
        ((MockDeploymentConfiguration) service.getDeploymentConfiguration())
                .setReactEnabled(false);
        // given a locked session
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        // When using react router we have the sever do the update in all cases
        // to control the correct timing for url updates
        configuration.setReactEnabled(false);

        // given a NavigationStateRenderer mapping to RegularView
        new NavigationStateBuilder(router).withTarget(RegularView.class)
                .build();
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                navigationStateFromTarget(RegularView.class));

        // given a UI with an instrumented Page that records
        // getHistory().pushState calls
        AtomicBoolean pushStateCalled = new AtomicBoolean(false);
        List<Location> pushStateLocations = new ArrayList<>();
        MockUI ui = new MockUI(session) {
            final Page page = new Page(this) {
                final History history = new History(getUI().get()) {
                    @Override
                    public void pushState(BaseJsonNode state,
                            Location location) {
                        pushStateCalled.set(true);
                        pushStateLocations.add(location);
                    }
                };

                @Override
                public History getHistory() {
                    return history;
                }
            };

            @Override
            public Page getPage() {
                return page;
            }
        };

        renderer.handle(new NavigationEvent(new Router(new TestRouteRegistry()),
                new Location("regular"), ui, NavigationTrigger.UI_NAVIGATE,
                (BaseJsonNode) null, true));
        Assert.assertFalse(
                "No pushState invocation is expected when forwardTo is true.",
                pushStateCalled.get());

        ui.getInternals().clearLastHandledNavigation();

        renderer.handle(new NavigationEvent(new Router(new TestRouteRegistry()),
                new Location("regular"), ui, NavigationTrigger.PROGRAMMATIC));
        Assert.assertFalse(
                "No pushState invocation is expected when navigation trigger is PROGRAMMATIC.",
                pushStateCalled.get());

        ui.getInternals().clearLastHandledNavigation();

        renderer.handle(new NavigationEvent(new Router(new TestRouteRegistry()),
                new Location("regular"), ui, NavigationTrigger.HISTORY));
        Assert.assertFalse(
                "No pushState invocation is expected when navigation trigger is HISTORY.",
                pushStateCalled.get());

        ui.getInternals().clearLastHandledNavigation();

        renderer.handle(new NavigationEvent(new Router(new TestRouteRegistry()),
                new Location("regular"), ui, NavigationTrigger.PAGE_LOAD));
        Assert.assertFalse(
                "No pushState invocation is expected when navigation trigger is PAGE_LOAD.",
                pushStateCalled.get());

        pushStateCalled.set(false);
        pushStateLocations.clear();
        ui.getInternals().clearLastHandledNavigation();

        renderer.handle(new NavigationEvent(new Router(new TestRouteRegistry()),
                new Location("regular"), ui, NavigationTrigger.UI_NAVIGATE));
        Assert.assertFalse(
                "No pushState invocation is expected when navigating to the current location.",
                pushStateCalled.get());
    }

    @Test
    public void purgeInactiveUIPreservedChainCache_activeUI_throws() {
        MockVaadinServletService service = createMockServiceWithInstantiator();
        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        MockUI activeUI = new MockUI(session);
        Component attachedToActiveUI = new PreservedView();
        activeUI.add(attachedToActiveUI);

        Assert.assertThrows(IllegalStateException.class,
                () -> AbstractNavigationStateRenderer
                        .purgeInactiveUIPreservedChainCache(activeUI));

    }

    @Test
    public void purgeInactiveUIPreservedChainCache_inactiveUI_clearsCache() {
        MockVaadinServletService service = createMockServiceWithInstantiator();
        WrappedSession wrappedSession = Mockito.mock(WrappedSession.class);
        Mockito.when(wrappedSession.getId()).thenReturn("A-SESSION-ID");
        MockVaadinSession session = new AlwaysLockedVaadinSession(service) {
            @Override
            public WrappedSession getSession() {
                return wrappedSession;
            }
        };

        MockUI activeUI = new MockUI(session);
        Component attachedToActiveUI = new PreservedView();
        activeUI.add(attachedToActiveUI);

        MockUI inActiveUI = new MockUI(session);
        Component attachedToInactiveUI = new PreservedView();
        inActiveUI.add(attachedToInactiveUI);
        inActiveUI.close();

        // Simulate two tabs on the same view, but one has been closed
        Location location = new Location("preserved");
        AbstractNavigationStateRenderer.setPreservedChain(session, "ACTIVE",
                location,
                new ArrayList<>(Collections.singletonList(attachedToActiveUI)));
        AbstractNavigationStateRenderer.setPreservedChain(session, "INACTIVE",
                location, new ArrayList<>(
                        Collections.singletonList(attachedToInactiveUI)));

        AbstractNavigationStateRenderer
                .purgeInactiveUIPreservedChainCache(inActiveUI);

        Optional<ArrayList<HasElement>> active = AbstractNavigationStateRenderer
                .getPreservedChain(session, "ACTIVE", location);
        Assert.assertTrue(
                "Expected preserved chain for active window to be present",
                active.isPresent());

        Optional<ArrayList<HasElement>> inactive = AbstractNavigationStateRenderer
                .getPreservedChain(session, "INACTIVE", location);
        Assert.assertFalse(
                "Expected preserved chain for inactive window to be removed",
                inactive.isPresent());

    }

    @Test
    public void getRouteTarget_usageStatistics() {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        MockVaadinServletService service = new MockVaadinServletService();
        AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                service) {
            @Override
            public DeploymentConfiguration getConfiguration() {
                return configuration;
            }
        };
        Mockito.when(configuration.isReactEnabled()).thenReturn(true);

        MockUI ui = new MockUI(session);
        NavigationEvent event = new NavigationEvent(
                new Router(new TestRouteRegistry()), new Location("home"), ui,
                NavigationTrigger.UI_NAVIGATE);
        NavigationStateRenderer renderer = new NavigationStateRenderer(
                navigationStateFromTarget(RegularView.class));

        UsageStatistics.removeEntry(Constants.STATISTICS_FLOW_ROUTER);

        renderer.handle(event);

        Assert.assertTrue(UsageStatistics.getEntries().anyMatch(entry -> entry
                .getName().equals(Constants.STATISTICS_FLOW_ROUTER)));
    }

    @Layout
    @Tag("div")
    public static class MainLayout extends Component implements RouterLayout {
        private final Element element = new Element("div");

        @Override
        public Element getElement() {
            return element;
        }
    }

    @Test
    public void handle_clientNavigationToFlowLayout_setTitleFromClientRoute() {
        testClientNavigationTitle("Client", true);
    }

    @Test
    public void handle_clientNavigation_doNotSetTitleFromClientRoute() {
        testClientNavigationTitle(null, false);
    }

    private void testClientNavigationTitle(String expectedDocumentTitle,
            boolean clientRouteHasFlowLayout) {
        UI ui = createTestClientNavigationTitleUIForTitleTests();
        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class, Mockito.CALLS_REAL_METHODS)) {

            menuRegistry.when(() -> MenuRegistry.getClientRoutes(true))
                    .thenReturn(Collections.singletonMap("/client-route",
                            new AvailableViewInfo("Client", null, false,
                                    "/client-route", false, false, null, null,
                                    null, clientRouteHasFlowLayout, null)));

            NavigationEvent event = new NavigationEvent(
                    new Router(new TestRouteRegistry()),
                    new Location("client-route"), ui,
                    NavigationTrigger.UI_NAVIGATE);
            NavigationStateRenderer renderer = new NavigationStateRenderer(
                    new NavigationStateBuilder(router)
                            .withTarget(MainLayout.class)
                            .withPath("client-route").build());

            renderer.handle(event);

            Assert.assertNotNull(ui.getPage());
            if (expectedDocumentTitle == null) {
                Mockito.verify(ui.getPage(), Mockito.never())
                        .setTitle("Client");
            } else {
                Mockito.verify(ui.getPage()).setTitle(expectedDocumentTitle);
            }
        }
    }

    private UI createTestClientNavigationTitleUIForTitleTests() {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        MockVaadinServletService service = new MockVaadinServletService(
                configuration);
        AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                service) {
            @Override
            public DeploymentConfiguration getConfiguration() {
                return configuration;
            }
        };
        Mockito.when(configuration.isReactEnabled()).thenReturn(false);
        Page page = Mockito.mock(Page.class);
        Mockito.when(page.getHistory()).thenReturn(Mockito.mock(History.class));
        return new MockUI(session) {
            @Override
            public Page getPage() {
                return page;
            }
        };
    }
}
