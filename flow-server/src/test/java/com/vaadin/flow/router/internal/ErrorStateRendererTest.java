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
package com.vaadin.flow.router.internal;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.ErrorNavigationEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationStateBuilder;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.ErrorStateRenderer.ExceptionsTrace;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ErrorStateRendererTest {

    /**
     * This view reroutes to {@link InfiniteLoopErrorTarget}
     */
    @Tag(Tag.A)
    @Route("npe")
    public static class InfiniteLoopNPEView extends Component
            implements BeforeEnterObserver {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.rerouteToError(NullPointerException.class);
        }
    }

    /**
     * This layout forwards to {@link InfiniteLoopNPEView}
     */
    @Tag(Tag.A)
    public static class InfiniteLoopErrorLayout extends Component
            implements RouterLayout, BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.forwardTo(InfiniteLoopNPEView.class);
        }

    }

    /**
     * This class has a parent layout which forwards to
     * {@link InfiniteLoopNPEView}
     */
    @Tag(Tag.A)
    @ParentLayout(InfiniteLoopErrorLayout.class)
    public static class InfiniteLoopErrorTarget extends Component
            implements HasErrorParameter<Exception> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<Exception> parameter) {
            return 500;
        }

    }

    @Tag(Tag.A)
    @Route("happy")
    public static class HappyPathViewView extends Component {
    }

    @Tag(Tag.A)
    public static class HappyPathErrorLayout extends Component
            implements RouterLayout, BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.forwardTo(HappyPathViewView.class);
        }

    }

    @Tag(Tag.A)
    @ParentLayout(HappyPathErrorLayout.class)
    public static class HappyPathErrorTarget extends Component
            implements HasErrorParameter<Exception> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<Exception> parameter) {
            return 500;
        }

    }

    @Test(expected = ExceptionsTrace.class)
    public void handle_openNPEErrorTarget_infiniteReroute_noStackOverflow_throws() {
        UI ui = configureMocks();

        NavigationState state = new NavigationStateBuilder(ui.getRouter())
                .withTarget(InfiniteLoopErrorTarget.class).build();
        ErrorStateRenderer renderer = new ErrorStateRenderer(state);

        RouteConfiguration.forRegistry(ui.getRouter().getRegistry())
                .setAnnotatedRoute(InfiniteLoopNPEView.class);

        ErrorParameter<Exception> parameter = new ErrorParameter<>(
                Exception.class, new NullPointerException());
        ErrorNavigationEvent event = new ErrorNavigationEvent(ui.getRouter(),
                new Location("error"), ui, NavigationTrigger.CLIENT_SIDE,
                parameter);
        // event should route to ErrorTarget whose layout forwards to NPEView
        // which reroute to ErrorTarget and this is an infinite loop
        renderer.handle(event);
    }

    @Test(expected = ExceptionsTrace.class)
    public void handle_openNPEView_infiniteReroute_noStackOverflow_throws() {
        UI ui = configureMocks();

        NavigationState state = new NavigationStateBuilder(ui.getRouter())
                .withTarget(InfiniteLoopNPEView.class).build();
        NavigationStateRenderer renderer = new NavigationStateRenderer(state);

        RouteConfiguration.forRegistry(ui.getRouter().getRegistry())
                .setAnnotatedRoute(InfiniteLoopNPEView.class);
        ((ApplicationRouteRegistry) ui.getRouter().getRegistry())
                .setErrorNavigationTargets(
                        Collections.singleton(InfiniteLoopErrorTarget.class));

        NavigationEvent event = new NavigationEvent(ui.getRouter(),
                new Location("npe"), ui, NavigationTrigger.CLIENT_SIDE);
        // event should route to ErrorTarget whose layout forwards to NPEView
        // which reroute to ErrorTarget and this is an infinite loop
        renderer.handle(event);

        JsonObject routerLinkState = Json.createObject();
        routerLinkState.put("href", "router_link");
        routerLinkState.put("scrollPositionX", 0d);
        routerLinkState.put("scrollPositionY", 0d);

        event = new NavigationEvent(ui.getRouter(), new Location("npe"), ui,
                NavigationTrigger.ROUTER_LINK, routerLinkState, false);
        // event should route to ErrorTarget whose layout forwards to NPEView
        // which reroute to ErrorTarget and this is an infinite loop
        renderer.handle(event);
    }

    @Test
    public void handle_errorViewLayoutForwardsToAView_viewIsNavigated() {
        UI ui = configureMocks();

        NavigationState state = new NavigationStateBuilder(ui.getRouter())
                .withTarget(HappyPathErrorTarget.class).build();
        ErrorStateRenderer renderer = new ErrorStateRenderer(state);

        RouteConfiguration.forRegistry(ui.getRouter().getRegistry())
                .setAnnotatedRoute(HappyPathViewView.class);

        ErrorParameter<Exception> parameter = new ErrorParameter<>(
                Exception.class, new NullPointerException());
        ErrorNavigationEvent event = new ErrorNavigationEvent(ui.getRouter(),
                new Location("error"), ui, NavigationTrigger.CLIENT_SIDE,
                parameter);
        Assert.assertEquals(200, renderer.handle(event));

        List<HasElement> chain = ui.getInternals()
                .getActiveRouterTargetsChain();
        Assert.assertEquals(1, chain.size());
        Assert.assertEquals(HappyPathViewView.class, chain.get(0).getClass());
    }

    private UI configureMocks() {
        MockVaadinServletService service = new MockVaadinServletService();
        service.init();

        MockVaadinSession session = new AlwaysLockedVaadinSession(service);
        session.setConfiguration(new MockDeploymentConfiguration());

        MockUI ui = new MockUI(session);
        return ui;
    }
}
