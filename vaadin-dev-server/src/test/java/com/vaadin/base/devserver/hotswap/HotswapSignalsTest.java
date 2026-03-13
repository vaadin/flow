/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.base.devserver.hotswap;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Test class for verifying that local signal values are properly transferred
 * during hotswap refresh.
 *
 */
class HotswapSignalsTest {

    private MockVaadinServletService service;
    private VaadinSession session;
    private MockUI ui;

    @Tag("div")
    public static class ViewWithValueSignal extends Component {
        final ValueSignal<String> name = new ValueSignal<>("default");
    }

    public static abstract class SuperViewWithValueSignal extends Component {
        final ValueSignal<String> name = new ValueSignal<>("default");
    }

    @Tag("div")
    public static class ViewWithSuperViewWithValueSignal
            extends SuperViewWithValueSignal {
    }

    @Tag("div")
    public static class ViewWithListSignal extends Component {
        final ListSignal<String> items = new ListSignal<>();
    }

    @Tag("div")
    public static class ViewWithMultipleSignals extends Component {
        final ValueSignal<String> title = new ValueSignal<>("untitled");
        final ValueSignal<Integer> count = new ValueSignal<>(0);
        final ListSignal<String> tags = new ListSignal<>();
    }

    @Tag("my-layout")
    public static class LayoutWithSignal extends Component
            implements RouterLayout {
        final ValueSignal<String> name = new ValueSignal<>("default");
    }

    @Tag("div")
    public static class ViewWithLayoutWithSignal extends Component {
    }

    @BeforeEach
    void setup() {
        service = new MockVaadinServletService();
        session = HotswapperTest.createMockVaadinSession(service);
    }

    @AfterEach
    void cleanup() {
        CurrentInstance.clearAll();
    }

    @Test
    void refreshRoute_valueSignalTransferred() {
        ui = HotswapperTest.initUIAndNavigateTo(service, session,
                ViewWithValueSignal.class);
        withSessionLock(() -> {
            ViewWithValueSignal oldView = getActiveView();
            oldView.name.set("modified");

            ui.refreshCurrentRoute(false);

            ViewWithValueSignal newView = getActiveView();
            assertNotSame(oldView, newView);
            assertEquals("modified", newView.name.peek());
        });
    }

    @Test
    void refreshRoute_valueSignalInSuperClassTransferred() {
        ui = HotswapperTest.initUIAndNavigateTo(service, session,
                ViewWithSuperViewWithValueSignal.class);
        withSessionLock(() -> {
            ViewWithSuperViewWithValueSignal oldView = getActiveView();
            oldView.name.set("modified");

            ui.refreshCurrentRoute(false);

            ViewWithSuperViewWithValueSignal newView = getActiveView();
            assertNotSame(oldView, newView);
            assertEquals("modified", newView.name.peek());
        });
    }

    @Test
    void refreshRouteLayout_valueSignalTransferred() {
        ui = HotswapperTest.initUIAndNavigateTo(service, session,
                ViewWithLayoutWithSignal.class, LayoutWithSignal.class);
        withSessionLock(() -> {
            ViewWithLayoutWithSignal oldView = getActiveView();
            LayoutWithSignal oldLayout = getActiveLayout(
                    LayoutWithSignal.class);
            oldLayout.name.set("modified");

            // emulates change in the layout class which causes whole layout
            // chain to be recreated
            ui.refreshCurrentRoute(true);

            ViewWithLayoutWithSignal newView = getActiveView();
            LayoutWithSignal newLayout = getActiveLayout(
                    LayoutWithSignal.class);
            assertNotSame(oldView, newView);
            assertNotSame(oldLayout, newLayout);
            assertEquals("modified", newLayout.name.peek());
        });
    }

    @Test
    void refreshRoute_listSignalTransferred() {
        ui = HotswapperTest.initUIAndNavigateTo(service, session,
                ViewWithListSignal.class);

        withSessionLock(() -> {
            ViewWithListSignal oldView = getActiveView();
            oldView.items.insertLast("a");
            oldView.items.insertLast("b");
            oldView.items.insertLast("c");

            ui.refreshCurrentRoute(false);

            ViewWithListSignal newView = getActiveView();
            assertNotSame(oldView, newView);

            List<String> values = newView.items.peek().stream()
                    .map(entry -> entry.peek()).collect(Collectors.toList());
            assertEquals(List.of("a", "b", "c"), values);
        });
    }

    @Test
    void refreshRoute_multipleSignalsTransferred() {
        ui = HotswapperTest.initUIAndNavigateTo(service, session,
                ViewWithMultipleSignals.class);

        withSessionLock(() -> {
            ViewWithMultipleSignals oldView = getActiveView();
            oldView.title.set("my title");
            oldView.count.set(42);
            oldView.tags.insertLast("java");
            oldView.tags.insertLast("flow");

            ui.refreshCurrentRoute(false);

            ViewWithMultipleSignals newView = getActiveView();
            assertNotSame(oldView, newView);
            assertEquals("my title", newView.title.peek());
            assertEquals(42, newView.count.peek());

            List<String> tagValues = newView.tags.peek().stream()
                    .map(entry -> entry.peek()).collect(Collectors.toList());
            assertEquals(List.of("java", "flow"), tagValues);
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends HasElement> T getActiveView() {
        return (T) ui.getInternals().getActiveRouterTargetsChain().get(0);
    }

    @SuppressWarnings("unchecked")
    private <T extends HasElement> T getActiveLayout(Class<T> layoutType) {
        return (T) ui.getInternals().getActiveRouterTargetsChain().stream()
                .filter(layoutType::isInstance).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No active layout of type: " + layoutType.getName()));
    }

    private void withSessionLock(Runnable action) {
        session.getLockInstance().lock();
        try {
            VaadinSession.setCurrent(session);
            VaadinService.setCurrent(service);
            action.run();
        } finally {
            session.getLockInstance().unlock();
        }
    }
}
