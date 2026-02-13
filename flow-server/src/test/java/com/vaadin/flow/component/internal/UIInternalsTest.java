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
package com.vaadin.flow.component.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UIInternalsTest {

    @Mock
    UI ui;

    MockVaadinServletService vaadinService;

    UIInternals internals;

    @Route
    @Push
    @Tag(Tag.DIV)
    public static class RouteTarget extends Component implements RouterLayout {

    }

    @Route(value = "foo", layout = RouteTarget.class)
    @Tag(Tag.DIV)
    public static class RouteTarget1 extends Component {

    }

    @Tag(Tag.DIV)
    static class MainLayout extends Component implements RouterLayout {
        static String ID = "main-layout-id";

        public MainLayout() {
            setId(ID);
        }
    }

    @Tag(Tag.DIV)
    @ParentLayout(MainLayout.class)
    static class SubLayout extends Component implements RouterLayout {
        static String ID = "sub-layout-id";

        public SubLayout() {
            setId(ID);
        }
    }

    @Tag(Tag.DIV)
    @Route(value = "child", layout = SubLayout.class)
    static class FirstView extends Component {
        static String ID = "child-view-id";

        public FirstView() {
            setId(ID);
        }
    }

    @Tag(Tag.DIV)
    static class AnotherLayout extends Component implements RouterLayout {
        static String ID = "another-layout-id";

        public AnotherLayout() {
            setId(ID);
        }
    }

    @Tag(Tag.DIV)
    @Route(value = "another", layout = MainLayout.class)
    static class AnotherView extends Component {
        static String ID = "another-view-id";

        public AnotherView() {
            setId(ID);
        }
    }

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(ui.getUI()).thenReturn(Optional.of(ui));
        Element body = new Element("body");
        Mockito.when(ui.getElement()).thenReturn(body);

        vaadinService = new MockVaadinServletService();
        internals = new UIInternals(ui);
        AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                vaadinService);
        internals.setSession(session);
        Mockito.when(ui.getSession()).thenReturn(session);
        Mockito.when(ui.getInternals()).thenReturn(internals);
    }

    @Test
    public void heartbeatTimestampSet_heartbeatListenersAreCalled() {
        List<Long> heartbeats = new ArrayList<>();
        Registration registration = internals.addHeartbeatListener(
                event -> heartbeats.add(event.getHeartbeatTime()));

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        assertEquals(1, heartbeats.size(),
                "Heartbeat listener should have fired");

        registration.remove();

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        assertEquals(1, heartbeats.size(),
                "Heartbeat listener should been removed and no new event recorded");
    }

    @Test
    public void heartbeatListenerRemovedFromHeartbeatEvent_noExplosion() {
        AtomicReference<Registration> reference = new AtomicReference<>();
        AtomicInteger runCount = new AtomicInteger();

        Registration registration = internals.addHeartbeatListener(event -> {
            runCount.incrementAndGet();
            reference.get().remove();
        });
        reference.set(registration);

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());
        assertEquals(1, runCount.get(), "Listener should have been run once");

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());
        assertEquals(1, runCount.get(),
                "Listener should not have been run again since it was removed");
    }

    @Test
    public void showRouteTarget_clientSideBootstrap() {
        PushConfiguration pushConfig = setUpInitialPush();

        internals.showRouteTarget(Mockito.mock(Location.class),
                new RouteTarget(), Collections.emptyList());

        Mockito.verify(pushConfig, Mockito.never()).setPushMode(Mockito.any());
    }

    @Test
    public void showRouteTarget_navigateToAnotherViewWithinSameLayoutHierarchy_detachedRouterLayoutChildrenRemoved() {
        MainLayout mainLayout = new MainLayout();
        SubLayout subLayout = new SubLayout();
        FirstView firstView = new FirstView();
        AnotherView anotherView = new AnotherView();

        List<RouterLayout> oldLayouts = Arrays.asList(subLayout, mainLayout);
        List<RouterLayout> newLayouts = Collections.singletonList(mainLayout);

        Location location = Mockito.mock(Location.class);
        setUpInitialPush();

        internals.showRouteTarget(location, firstView, oldLayouts);
        List<HasElement> activeRouterTargetsChain = internals
                .getActiveRouterTargetsChain();

        // Initial router layouts hierarchy is checked here in order to be
        // sure the sub layout and it's child view is in place BEFORE
        // navigation and old content cleanup
        assertArrayEquals(new HasElement[] { firstView, subLayout, mainLayout },
                activeRouterTargetsChain.toArray(),
                "Unexpected initial router targets chain");

        assertEquals(1, mainLayout.getElement().getChildren().count(),
                "Expected one child element for main layout before navigation");
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        Element subLayoutElement = mainLayout.getElement().getChildren()
                .findFirst().get();
        assertEquals(SubLayout.ID, subLayoutElement.getAttribute("id"),
                "Unexpected sub layout element");
        assertEquals(1, subLayoutElement.getChildren().count(),
                "Expected one child element for sub layout before navigation");
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        Element firstViewElement = subLayoutElement.getChildren().findFirst()
                .get();
        assertEquals(FirstView.ID, firstViewElement.getAttribute("id"),
                "Unexpected first view element");

        // Trigger navigation
        internals.showRouteTarget(location, anotherView, newLayouts);
        activeRouterTargetsChain = internals.getActiveRouterTargetsChain();
        assertArrayEquals(new HasElement[] { anotherView, mainLayout },
                activeRouterTargetsChain.toArray(),
                "Unexpected router targets chain after navigation");

        // Check that the old content (sub layout) is detached and it's
        // children are also detached
        assertEquals(1, mainLayout.getElement().getChildren().count(),
                "Expected one child element for main layout after navigation");
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        Element anotherViewElement = mainLayout.getElement().getChildren()
                .findFirst().get();
        assertEquals(AnotherView.ID, anotherViewElement.getAttribute("id"),
                "Unexpected another view element");
        assertEquals(0, subLayout.getElement().getChildren().count(),
                "Expected no child elements for sub layout after navigation");
    }

    @Test
    public void showRouteTarget_navigateToAnotherLayoutHierarchy_detachedLayoutHierarchyChildrenRemoved() {
        MainLayout mainLayout = new MainLayout();
        SubLayout subLayout = new SubLayout();
        FirstView firstView = new FirstView();
        AnotherLayout anotherLayout = new AnotherLayout();
        AnotherView anotherView = new AnotherView();

        List<RouterLayout> oldLayouts = Arrays.asList(subLayout, mainLayout);
        List<RouterLayout> newLayouts = Collections
                .singletonList(anotherLayout);

        Location location = Mockito.mock(Location.class);
        setUpInitialPush();

        // Initial navigation
        internals.showRouteTarget(location, firstView, oldLayouts);
        // Navigate to another view outside of the initial router hierarchy
        internals.showRouteTarget(location, anotherView, newLayouts);
        List<HasElement> activeRouterTargetsChain = internals
                .getActiveRouterTargetsChain();
        assertArrayEquals(new HasElement[] { anotherView, anotherLayout },
                activeRouterTargetsChain.toArray(),
                "Unexpected router targets chain after navigation");

        // Check that both main layout, sub layout and it's child view are
        // detached
        assertEquals(0, mainLayout.getElement().getChildren().count(),
                "Expected no child elements for main layout after navigation");
        assertEquals(0, subLayout.getElement().getChildren().count(),
                "Expected no child elements for sub layout after navigation");
    }

    @Test
    public void dumpPendingJavaScriptInvocations_detachListenerRegisteredOnce() {
        StateNode node = Mockito.spy(new StateNode(ElementData.class));
        node.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node);

        internals.addJavaScriptInvocation(new PendingJavaScriptInvocation(node,
                new UIInternals.JavaScriptInvocation("")));
        internals.dumpPendingJavaScriptInvocations();
        internals.dumpPendingJavaScriptInvocations();
        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node, Mockito.times(1))
                .addDetachListener(ArgumentMatchers.any());
    }

    @Test
    public void dumpPendingJavaScriptInvocations_multipleInvocationPerNode_onlyOneDetachListenerRegistered() {
        StateNode node = Mockito.spy(new StateNode(ElementData.class));
        node.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node);

        internals.addJavaScriptInvocation(new PendingJavaScriptInvocation(node,
                new UIInternals.JavaScriptInvocation("1")));
        internals.addJavaScriptInvocation(new PendingJavaScriptInvocation(node,
                new UIInternals.JavaScriptInvocation("2")));
        internals.addJavaScriptInvocation(new PendingJavaScriptInvocation(node,
                new UIInternals.JavaScriptInvocation("3")));
        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node, Mockito.times(1))
                .addDetachListener(ArgumentMatchers.any());
    }

    @Test
    public void dumpPendingJavaScriptInvocations_registerOneDetachListenerPerNode() {
        StateNode node1 = Mockito.spy(new StateNode(ElementData.class));
        node1.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node1);
        internals.addJavaScriptInvocation(new PendingJavaScriptInvocation(node1,
                new UIInternals.JavaScriptInvocation("1")));

        StateNode node2 = Mockito.spy(new StateNode(ElementData.class));
        node2.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node2);
        internals.addJavaScriptInvocation(new PendingJavaScriptInvocation(node2,
                new UIInternals.JavaScriptInvocation("1")));

        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node1, Mockito.times(1))
                .addDetachListener(ArgumentMatchers.any());
        Mockito.verify(node2, Mockito.times(1))
                .addDetachListener(ArgumentMatchers.any());
    }

    @Test
    public void dumpPendingJavaScriptInvocations_invocationCompletes_pendingListPurged() {
        StateNode node = Mockito.spy(new StateNode(ElementData.class));
        node.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node);

        PendingJavaScriptInvocation invocation = new PendingJavaScriptInvocation(
                node, new UIInternals.JavaScriptInvocation(""));
        internals.addJavaScriptInvocation(invocation);
        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node, Mockito.times(1))
                .addDetachListener(ArgumentMatchers.any());

        invocation.complete(JacksonCodec.encodeWithTypeInfo("OK"));

        assertEquals(0, internals.getPendingJavaScriptInvocations().count());
    }

    @Test
    public void dumpPendingJavaScriptInvocations_invocationFails_pendingListPurged() {
        StateNode node = Mockito.spy(new StateNode(ElementData.class));
        node.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node);

        PendingJavaScriptInvocation invocation = new PendingJavaScriptInvocation(
                node, new UIInternals.JavaScriptInvocation(""));
        internals.addJavaScriptInvocation(invocation);
        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node, Mockito.times(1))
                .addDetachListener(ArgumentMatchers.any());

        invocation.completeExceptionally(
                JacksonCodec.encodeWithTypeInfo("ERROR"));

        assertEquals(0, internals.getPendingJavaScriptInvocations().count());
    }

    @Test
    public void dumpPendingJavaScriptInvocations_invocationCanceled_pendingListPurged() {
        StateNode node = Mockito.spy(new StateNode(ElementData.class));
        node.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node);

        PendingJavaScriptInvocation invocation = new PendingJavaScriptInvocation(
                node, new UIInternals.JavaScriptInvocation(""));
        internals.addJavaScriptInvocation(invocation);
        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node, Mockito.times(1))
                .addDetachListener(ArgumentMatchers.any());

        invocation.cancelExecution();

        assertEquals(0, internals.getPendingJavaScriptInvocations().count());
    }

    @Test
    public void dumpPendingJavaScriptInvocations_nodeDetached_pendingListPurged() {
        StateNode node = Mockito.spy(new StateNode(ElementData.class));
        node.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node);

        PendingJavaScriptInvocation invocation = new PendingJavaScriptInvocation(
                node, new UIInternals.JavaScriptInvocation(""));
        internals.addJavaScriptInvocation(invocation);
        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node, Mockito.times(1))
                .addDetachListener(ArgumentMatchers.any());

        node.setParent(null);

        assertEquals(0, internals.getPendingJavaScriptInvocations().count());
    }

    @Test
    public void dumpPendingJavaScriptInvocations_multipleInvocation_detachListenerRegisteredOnce() {
        StateNode node = Mockito.spy(new StateNode(ElementData.class));
        node.getFeature(ElementData.class).setVisible(false);
        internals.getStateTree().getRootNode()
                .getFeature(ElementChildrenList.class).add(0, node);

        PendingJavaScriptInvocation invocation = Mockito
                .spy(new PendingJavaScriptInvocation(node,
                        new UIInternals.JavaScriptInvocation("")));
        internals.addJavaScriptInvocation(invocation);
        internals.dumpPendingJavaScriptInvocations();
        internals.dumpPendingJavaScriptInvocations();
        internals.dumpPendingJavaScriptInvocations();
        internals.dumpPendingJavaScriptInvocations();

        Mockito.verify(node, Mockito.times(1))
                .addDetachListener(ArgumentMatchers.any());
        Mockito.verify(invocation, Mockito.times(1)).then(
                ArgumentMatchers.any(SerializableConsumer.class),
                ArgumentMatchers.any(SerializableConsumer.class));

        node.setParent(null);
        assertEquals(0, internals.getPendingJavaScriptInvocations().count());
    }

    @Test
    public void isDirty_noPendingJsInvocation_returnsFalse() {
        StateNode node1 = Mockito.spy(new StateNode(ElementData.class));
        StateNode node2 = Mockito.spy(new StateNode(ElementData.class));
        node2.getFeature(ElementData.class).setVisible(false);
        ElementChildrenList childrenList = internals.getStateTree()
                .getRootNode().getFeature(ElementChildrenList.class);
        childrenList.add(0, node1);
        childrenList.add(1, node2);

        assertTrue(internals.isDirty(), "Nodes added, expecting dirty UI");
        internals.getStateTree().collectChanges(node -> {
        });
        internals.dumpPendingJavaScriptInvocations();

        assertFalse(internals.isDirty(),
                "Changes collected, expecting UI not to be dirty");
    }

    @Test
    public void isDirty_pendingJsInvocationReadyToSend_returnsTrue() {
        StateNode node1 = Mockito.spy(new StateNode(ElementData.class));
        StateNode node2 = Mockito.spy(new StateNode(ElementData.class));
        node2.getFeature(ElementData.class).setVisible(false);
        ElementChildrenList childrenList = internals.getStateTree()
                .getRootNode().getFeature(ElementChildrenList.class);
        childrenList.add(0, node1);
        childrenList.add(1, node2);

        internals.addJavaScriptInvocation(new PendingJavaScriptInvocation(node1,
                new UIInternals.JavaScriptInvocation("")));

        assertTrue(internals.isDirty(),
                "Pending JS invocations, expecting dirty UI");
        internals.getStateTree().collectChanges(node -> {
        });
        internals.dumpPendingJavaScriptInvocations();

        assertFalse(internals.isDirty(),
                "No pending JS invocations to send to the client, expecting UI not to be dirty");
    }

    @Test
    public void isDirty_pendingJsInvocationNotReadyToSend_returnsFalse() {
        StateNode node1 = Mockito.spy(new StateNode(ElementData.class));
        StateNode node2 = Mockito.spy(new StateNode(ElementData.class));
        node2.getFeature(ElementData.class).setVisible(false);
        ElementChildrenList childrenList = internals.getStateTree()
                .getRootNode().getFeature(ElementChildrenList.class);
        childrenList.add(0, node1);
        childrenList.add(1, node2);

        internals.addJavaScriptInvocation(new PendingJavaScriptInvocation(node1,
                new UIInternals.JavaScriptInvocation("")));
        internals.addJavaScriptInvocation(new PendingJavaScriptInvocation(node2,
                new UIInternals.JavaScriptInvocation("")));

        assertTrue(internals.isDirty(),
                "Pending JS invocations, expecting dirty UI");
        internals.getStateTree().collectChanges(node -> {
        });
        internals.dumpPendingJavaScriptInvocations();

        assertFalse(internals.isDirty(),
                "No pending JS invocations to send to the client, expecting UI not to be dirty");
    }

    @Test
    public void setTitle_titleAndPendingJsInvocationSetsCorrectTitle() {
        internals.setTitle("new title");
        assertEquals("new title", internals.getTitle());

        assertEquals(1, internals.getPendingJavaScriptInvocations().count(),
                "one pending JavaScript invocation should exist");

        var pendingJavaScriptInvocation = internals
                .getPendingJavaScriptInvocations().findFirst().orElse(null);
        assertNotNull(pendingJavaScriptInvocation,
                "pendingJavaScriptInvocation should not be null");
        assertEquals("new title", pendingJavaScriptInvocation.getInvocation()
                .getParameters().get(0));
        assertTrue(
                pendingJavaScriptInvocation.getInvocation().getExpression()
                        .contains("document.title = $0"),
                "document.title should be set via JavaScript");
        assertTrue(pendingJavaScriptInvocation.getInvocation().getExpression()
                .contains("""
                            if(window?.Vaadin?.documentTitleSignal) {
                                window.Vaadin.documentTitleSignal.value = $0;
                            }
                        """.stripIndent()),
                "window.Vaadin.documentTitleSignal.value should be set conditionally via JavaScript");
    }

    private PushConfiguration setUpInitialPush() {
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        vaadinService.setConfiguration(config);

        PushConfiguration pushConfig = Mockito.mock(PushConfiguration.class);
        Mockito.when(ui.getPushConfiguration()).thenReturn(pushConfig);

        Mockito.when(config.getPushMode()).thenReturn(PushMode.DISABLED);
        return pushConfig;
    }

    @Test
    public void getDeploymentConfiguration() {
        AlwaysLockedVaadinSession session = Mockito
                .mock(AlwaysLockedVaadinSession.class);
        MockVaadinServletService mockVaadinServletService = Mockito
                .mock(MockVaadinServletService.class);

        internals = new UIInternals(ui);
        internals.setSession(session);

        Mockito.when(session.getService()).thenReturn(mockVaadinServletService);
        DeploymentConfiguration config = new MockDeploymentConfiguration();
        Mockito.when(mockVaadinServletService.getDeploymentConfiguration())
                .thenReturn(config);

        DeploymentConfiguration result = internals.getDeploymentConfiguration();

        Mockito.verify(session).getService();
        Mockito.verify(mockVaadinServletService).getDeploymentConfiguration();
        assertEquals(config, result);
    }
}
