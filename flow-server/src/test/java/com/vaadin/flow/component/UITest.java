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

package com.vaadin.flow.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;

import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementDetachEvent;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.dom.NodeVisitor;
import com.vaadin.flow.dom.impl.AbstractTextElementStateProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationListener;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveListener;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.ListenerPriority;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.AfterNavigationHandler;
import com.vaadin.flow.router.internal.BeforeEnterHandler;
import com.vaadin.flow.router.internal.BeforeLeaveHandler;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.frontend.MockLogger;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

public class UITest {

    @Route("")
    @Tag(Tag.DIV)
    public static class RootNavigationTarget extends Component {

    }

    @Route("foo/bar")
    @Tag(Tag.DIV)
    public static class FooBarNavigationTarget extends Component {

    }

    @Route(value = ":barParam/bar", layout = FooBarParamParentNavigationTarget.class)
    @Tag(Tag.DIV)
    public static class FooBarParamNavigationTarget extends Component {

    }

    @RoutePrefix("foo/:fooParam")
    @Tag(Tag.DIV)
    public static class FooBarParamParentNavigationTarget extends Component
            implements RouterLayout {

    }

    @Route("foo-bar")
    @Tag(Tag.DIV)
    public static class Parameterized extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }

    }

    @Tag(Tag.DIV)
    public static class ParameterizedNotRoute extends Component
            implements HasUrlParameter<Integer> {

        @Override
        public void setParameter(BeforeEvent event, Integer parameter) {
        }

    }

    private static class AttachableComponent extends Component {
        public AttachableComponent() {
            super(new Element("div"));
        }
    }

    private static class AttachedElementStateProvider
            extends AbstractTextElementStateProvider {

        @Override
        public boolean supports(StateNode node) {
            return true;
        }

        @Override
        public Node getParent(StateNode node) {
            return null;
        }

        @Override
        public String getTextContent(StateNode node) {
            return null;
        }

        @Override
        public void setTextContent(StateNode node, String textContent) {
        }

        @Override
        public void visit(StateNode node, NodeVisitor visitor) {
        }
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void elementIsBody() {
        UI ui = new UI();

        assertEquals("body", ui.getElement().getTag());
    }

    private static UI createTestUI() {
        MockLogger mockLogger = new MockLogger();
        UI ui = new UI() {
            @Override
            public void doInit(VaadinRequest request, int uiId, String appId) {

            }

            @Override
            Logger getLogger() {
                return mockLogger;
            }
        };

        return ui;
    }

    private static MockUI createAccessableTestUI() {
        // Needs a service to be able to do service.accessSession
        MockVaadinSession session = new MockVaadinSession(
                new MockVaadinServletService());
        session.lock();
        MockUI ui = new MockUI(session);
        session.unlock();
        return ui;
    }

    private static void initUI(UI ui, String initialLocation,
            ArgumentCaptor<Integer> statusCodeCaptor)
            throws InvalidRouteConfigurationException {
        VaadinServletRequest request = Mockito.mock(VaadinServletRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);

        String pathInfo;
        if (initialLocation.isEmpty()) {
            pathInfo = null;
        } else {
            Assert.assertFalse(initialLocation.startsWith("/"));
            pathInfo = "/" + initialLocation;
        }
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);

        VaadinService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        service.setCurrentInstances(request, response);

        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(config.isProductionMode()).thenReturn(false);
        Mockito.when(config.getFrontendFolder()).thenReturn(new File("front"));
        Mockito.when(config.getProjectFolder()).thenReturn(new File("./"));
        Mockito.when(config.getBuildFolder()).thenReturn("build");

        session.lock();
        ((MockVaadinServletService) service).setConfiguration(config);
        ui.getInternals().setSession(session);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(ui.getInternals().getRouter().getRegistry());

        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            Arrays.asList(RootNavigationTarget.class,
                    FooBarNavigationTarget.class, Parameterized.class,
                    FooBarParamNavigationTarget.class)
                    .forEach(routeConfiguration::setAnnotatedRoute);
        });

        ui.doInit(request, 0, "foo");
        ui.getInternals().getRouter().initializeUI(ui,
                requestToLocation(request));

        session.unlock();

        if (statusCodeCaptor != null) {
            Mockito.verify(response).setStatus(statusCodeCaptor.capture());
        }
    }

    public static Location requestToLocation(VaadinRequest request) {
        return new Location(request.getPathInfo(),
                QueryParameters.full(request.getParameterMap()));
    }

    @Test
    public void scrollAttribute() {
        UI ui = new UI();
        Assert.assertNull(
                "'scroll' attribute shouldn't be set for the "
                        + "UI element which represents 'body' tag",
                ui.getElement().getAttribute("scroll"));
    }

    @Test
    public void testInitialLocation()
            throws InvalidRouteConfigurationException {
        UI ui = new UI();
        initUI(ui, "", null);

        assertEquals("", ui.getInternals().getActiveViewLocation().getPath());
    }

    @Test
    public void locationAfterServerNavigation()
            throws InvalidRouteConfigurationException {
        UI ui = new UI();
        initUI(ui, "", null);

        ui.navigate("foo/bar");

        assertEquals("foo/bar",
                ui.getInternals().getActiveViewLocation().getPath());
        List<HasElement> chain = ui.getInternals()
                .getActiveRouterTargetsChain();
        Assert.assertEquals(1, chain.size());
        Component currentRoute = ui.getCurrentView();
        MatcherAssert.assertThat(currentRoute,
                CoreMatchers.instanceOf(FooBarNavigationTarget.class));
    }

    @Test
    @Ignore("Check what is the new Router.navigate for JavaScriptUI")
    public void navigateWithParameters_delegateToRouter() {
        final String route = "params";
        Router router = Mockito.mock(Router.class);
        UI ui = new MockUI(router);

        QueryParameters params = QueryParameters
                .simple(Collections.singletonMap("test", "indeed"));

        ArgumentCaptor<Location> location = ArgumentCaptor
                .forClass(Location.class);

        ui.navigate(route, params);

        Mockito.verify(router).navigate(ArgumentMatchers.eq(ui),
                location.capture(),
                ArgumentMatchers.eq(NavigationTrigger.UI_NAVIGATE));

        Location value = location.getValue();
        Assert.assertEquals(route, value.getPath());
        Assert.assertEquals(params, value.getQueryParameters());
    }

    @Test
    public void navigateWithParameters_afterServerNavigation()
            throws InvalidRouteConfigurationException {
        UI ui = new UI();
        initUI(ui, "", null);

        Optional<FooBarParamNavigationTarget> newView = ui.navigate(
                FooBarParamNavigationTarget.class,
                new RouteParameters(new RouteParam("fooParam", "flu"),
                        new RouteParam("barParam", "beer")));

        assertEquals(FooBarParamNavigationTarget.class,
                newView.get().getClass());

        assertEquals("foo/flu/beer/bar",
                ui.getInternals().getActiveViewLocation().getPath());
        List<HasElement> chain = ui.getInternals()
                .getActiveRouterTargetsChain();
        Assert.assertEquals(2, chain.size());
        MatcherAssert.assertThat(chain.get(0),
                CoreMatchers.instanceOf(FooBarParamNavigationTarget.class));
        MatcherAssert.assertThat(chain.get(1), CoreMatchers
                .instanceOf(FooBarParamParentNavigationTarget.class));
    }

    @Test
    public void navigateWithQueryAndRouteParameters_afterServerNavigation()
            throws InvalidRouteConfigurationException {
        UI ui = new UI();
        initUI(ui, "", null);

        Optional<FooBarParamNavigationTarget> newView = ui.navigate(
                FooBarParamNavigationTarget.class,
                new RouteParameters(new RouteParam("fooParam", "flu"),
                        new RouteParam("barParam", "beer")),
                QueryParameters.of("bigBeer", "forMePlease"));

        assertEquals(FooBarParamNavigationTarget.class,
                newView.get().getClass());

        assertEquals("foo/flu/beer/bar?bigBeer=forMePlease", ui.getInternals()
                .getActiveViewLocation().getPathWithQueryParameters());
        List<HasElement> chain = ui.getInternals()
                .getActiveRouterTargetsChain();
        Assert.assertEquals(2, chain.size());
        MatcherAssert.assertThat(chain.get(0),
                CoreMatchers.instanceOf(FooBarParamNavigationTarget.class));
        MatcherAssert.assertThat(chain.get(1), CoreMatchers
                .instanceOf(FooBarParamParentNavigationTarget.class));
    }

    @Test
    public void localeSet_directionUpdated() {
        MockUI ui = new MockUI();

        ui.setDirection(Direction.RIGHT_TO_LEFT);

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<PendingJavaScriptInvocation> pendingJavaScriptInvocations = ui
                .dumpPendingJsInvocations();

        Assert.assertEquals(1, pendingJavaScriptInvocations.size());
        Assert.assertEquals("rtl", pendingJavaScriptInvocations.get(0)
                .getInvocation().getParameters().get(0));
    }

    @Test
    public void locationAfterClientNavigation()
            throws InvalidRouteConfigurationException {
        UI ui = new UI();
        initUI(ui, "", null);

        History history = ui.getPage().getHistory();

        history.getHistoryStateChangeHandler()
                .onHistoryStateChange(new HistoryStateChangeEvent(history, null,
                        new Location("foo/bar"), NavigationTrigger.HISTORY));

        assertEquals("foo/bar",
                ui.getInternals().getActiveViewLocation().getPath());
    }

    @Test
    public void noRouteMatches_404ViewAndCodeReturned()
            throws InvalidRouteConfigurationException {
        UI ui = new UI();

        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor
                .forClass(Integer.class);

        initUI(ui, "baz", statusCodeCaptor);

        Assert.assertEquals(1, ui.getChildren().count());
        Optional<Component> errorComponent = ui.getChildren().findFirst();
        MatcherAssert.assertThat(errorComponent.get(),
                CoreMatchers.instanceOf(RouteNotFoundError.class));
        assertEquals(Integer.valueOf(404), statusCodeCaptor.getValue());
    }

    @Test
    public void addComponent() {
        UI ui = new UI();
        Text text = new Text("foo");
        ui.add(text);
        ComponentTest.assertChildren(ui, text);
    }

    @Test
    public void addComponents() {
        UI ui = new UI();
        Text text = new Text("foo");
        Html html = new Html("<div>foobar</div>");
        ui.add(text, html);
        ComponentTest.assertChildren(ui, text, html);
    }

    @Test
    public void removeComponent() {
        UI ui = new UI();
        Text text = new Text("foo");
        ui.add(text);
        ui.remove(text);

        ComponentTest.assertChildren(ui);
    }

    @Test
    public void setLastHeartbeatTimestamp_heartbeatEventIsFired() {
        UI ui = new UI();
        initUI(ui, "", null);

        long heartbeatTimestamp = System.currentTimeMillis();
        List<HeartbeatEvent> events = new ArrayList<>();
        ui.addHeartbeatListener(events::add);

        ui.getInternals().setLastHeartbeatTimestamp(heartbeatTimestamp);

        assertEquals(1, events.size());
        assertEquals(ui, events.get(0).getSource());
        assertEquals(heartbeatTimestamp, events.get(0).getHeartbeatTime());
    }

    @Test
    public void setLastHeartbeatTimestamp_multipleHeartbeatListenerRegistered_eachHeartbeatListenerIsCalled() {
        UI ui = new UI();
        initUI(ui, "", null);

        List<HeartbeatEvent> events = new ArrayList<>();
        ui.addHeartbeatListener(events::add);
        ui.addHeartbeatListener(events::add);

        ui.getInternals().setLastHeartbeatTimestamp(System.currentTimeMillis());

        assertEquals(2, events.size());
    }

    @Test
    public void setLastHeartbeatTimestamp_heartbeatListenerRemoved_listenerNotRun() {
        UI ui = new UI();
        initUI(ui, "", null);

        AtomicReference<Registration> reference = new AtomicReference<>();
        AtomicInteger runCount = new AtomicInteger();

        Registration registration = ui.addHeartbeatListener(event -> {
            runCount.incrementAndGet();
            reference.get().remove(); // removes the listener on the first
                                      // invocation
        });
        reference.set(registration);

        ui.getInternals().setLastHeartbeatTimestamp(System.currentTimeMillis());
        assertEquals("Listener should have been run once", 1, runCount.get());

        ui.getInternals().setLastHeartbeatTimestamp(System.currentTimeMillis());
        assertEquals(
                "Listener should not have been run again since it was removed",
                1, runCount.get());
    }

    @Test
    public void setSession_attachEventIsFired()
            throws InvalidRouteConfigurationException {
        UI ui = new UI();
        List<AttachEvent> events = new ArrayList<>();
        ui.addAttachListener(events::add);
        initUI(ui, "", null);

        assertEquals(1, events.size());
        assertEquals(ui, events.get(0).getSource());
    }

    @Test
    public void unsetSession_detachEventIsFired()
            throws InvalidRouteConfigurationException {
        UI ui = createTestUI();
        List<DetachEvent> events = new ArrayList<>();
        ui.addDetachListener(events::add);
        initUI(ui, "", null);

        ui.getSession().access(() -> ui.getInternals().setSession(null));

        // Unlock to run pending access tasks
        ui.getSession().unlock();

        assertEquals(1, events.size());
        assertEquals(ui, events.get(0).getSource());
    }

    @Test
    public void unsetSession_detachEventIsFiredForUIChildren()
            throws InvalidRouteConfigurationException {
        UI ui = createTestUI();
        List<DetachEvent> events = new ArrayList<>();
        initUI(ui, "", null);

        Component childComponent = new AttachableComponent();
        ui.add(childComponent);
        childComponent.addDetachListener(events::add);

        ui.getSession().access(() -> ui.getInternals().setSession(null));

        // Unlock to run pending access tasks
        ui.getSession().unlock();

        assertEquals(1, events.size());
        assertEquals(childComponent, events.get(0).getSource());
    }

    @Test
    public void unsetSession_detachEventIsFiredForElements() {
        UI ui = createTestUI();

        List<ElementDetachEvent> events = new ArrayList<>();

        ui.getElement().addDetachListener(events::add);
        initUI(ui, "", null);

        Component childComponent = new AttachableComponent();
        ui.add(childComponent);
        childComponent.getElement().addDetachListener(events::add);

        ui.getSession().access(() -> ui.getInternals().setSession(null));

        // Unlock to run pending access tasks
        ui.getSession().unlock();

        assertEquals(2, events.size());
        assertEquals(childComponent.getElement(), events.get(0).getSource());
        assertEquals(ui.getElement(), events.get(1).getSource());
    }

    @Test
    public void unsetSession_accessErrorHandlerStillWorks() throws IOException {
        UI ui = createTestUI();
        initUI(ui, "", null);

        ui.getSession().access(() -> ui.getInternals().setSession(null));
        ui.access(() -> {
            Assert.fail("We should never get here because the UI is detached");
        });

        // Unlock to run pending access tasks
        ui.getSession().unlock();

        String logOutput = ((MockLogger) ui.getLogger()).getLogs();
        String logOutputNoDebug = logOutput.replaceAll("^\\[Debug\\].*", "");

        Assert.assertFalse(
                "No NullPointerException should be logged but got: "
                        + logOutput,
                logOutput.contains("NullPointerException"));
        Assert.assertFalse(
                "No UIDetachedException should be logged but got: "
                        + logOutputNoDebug,
                logOutputNoDebug.contains("UIDetachedException"));
    }

    @Test
    public void access_currentUIFilledInErrorHandler() {
        UI ui = createTestUI();
        initUI(ui, "", null);
        final AtomicReference<UI> uiInErrorHandler = new AtomicReference<>();
        final AtomicBoolean errorHandlerCalled = new AtomicBoolean();
        ui.getSession().setErrorHandler((ErrorHandler) event -> {
            errorHandlerCalled.set(true);
            uiInErrorHandler.set(UI.getCurrent());
        });
        ui.access(() -> {
            throw new RuntimeException("Simulated");
        });

        // Unlock to run pending access tasks
        ui.getSession().unlock();

        Assert.assertTrue(errorHandlerCalled.get());
        Assert.assertEquals(ui, uiInErrorHandler.get());
    }

    @Test
    public void beforeClientResponse_regularOrder() {
        UI ui = createTestUI();
        Component rootComponent = new AttachableComponent();
        ui.add(rootComponent);

        List<Integer> results = new ArrayList<>();

        ui.beforeClientResponse(rootComponent, context -> results.add(0));
        ui.beforeClientResponse(rootComponent, context -> results.add(1));
        ui.beforeClientResponse(rootComponent, context -> results.add(2));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        Assert.assertTrue("There should be 3 results in the list",
                results.size() == 3);

        for (int i = 0; i < results.size(); i++) {
            Assert.assertEquals(
                    "The result at index '" + i + "' should be " + i, i,
                    results.get(i).intValue());
        }
    }

    @Test
    public void beforeClientResponse_withInnerRunnables() {
        UI ui = createTestUI();
        Component rootComponent = new AttachableComponent();
        ui.add(rootComponent);

        List<Integer> results = new ArrayList<>();

        ui.beforeClientResponse(rootComponent, context -> results.add(0));
        ui.beforeClientResponse(rootComponent, context -> {
            results.add(1);
            ui.beforeClientResponse(rootComponent, context2 -> results.add(3));
            ui.beforeClientResponse(rootComponent, context2 -> results.add(4));
        });
        ui.beforeClientResponse(rootComponent, context -> results.add(2));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        Assert.assertTrue("There should be 5 results in the list",
                results.size() == 5);

        for (int i = 0; i < results.size(); i++) {
            Assert.assertEquals(
                    "The result at index '" + i + "' should be " + i, i,
                    results.get(i).intValue());
        }
    }

    @Test
    public void beforeClientResponse_withUnattachedNodes() {
        UI ui = createTestUI();
        Component rootComponent = new AttachableComponent();
        ui.add(rootComponent);
        Component emptyComponent = new AttachableComponent();

        List<Integer> results = new ArrayList<>();

        ui.beforeClientResponse(emptyComponent, context -> results.add(0));
        ui.beforeClientResponse(rootComponent, context -> results.add(1));
        ui.beforeClientResponse(emptyComponent, context -> results.add(2));
        ui.beforeClientResponse(rootComponent, context -> results.add(3));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        Assert.assertTrue("There should be 2 results in the list",
                results.size() == 2);

        Assert.assertEquals("The result at index '0' should be " + 1, 1,
                results.get(0).intValue());
        Assert.assertEquals("The result at index '1' should be " + 3, 3,
                results.get(1).intValue());
    }

    @Test
    public void beforeClientResponse_withAttachedNodesDuringExecution() {
        UI ui = createTestUI();
        Component rootComponent = new AttachableComponent();
        ui.add(rootComponent);
        AttachableComponent emptyComponent1 = new AttachableComponent();
        AttachableComponent emptyComponent2 = new AttachableComponent();

        List<Integer> results = new ArrayList<>();

        ui.beforeClientResponse(emptyComponent1, context -> {
            results.add(0);
            ui.add(emptyComponent2);
        });
        ui.beforeClientResponse(rootComponent, context -> {
            results.add(1);
            ui.add(emptyComponent1);
        });
        ui.beforeClientResponse(emptyComponent2, context -> results.add(2));
        ui.beforeClientResponse(rootComponent, context -> results.add(3));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        Assert.assertTrue("There should be 4 results in the list",
                results.size() == 4);

        Assert.assertEquals("The result at index '0' should be 1", 1,
                results.get(0).intValue());
        Assert.assertEquals("The result at index '1' should be 3", 3,
                results.get(1).intValue());
        Assert.assertEquals("The result at index '2' should be 0", 0,
                results.get(2).intValue());
        Assert.assertEquals("The result at index '3' should be 2", 2,
                results.get(3).intValue());
    }

    @Test
    public void beforeClientResponse_withReattachedNodes() {
        UI ui = createTestUI();
        Component root = new AttachableComponent();
        ui.add(root);
        ui.getInternals().getStateTree().collectChanges(change -> {
        });
        AttachableComponent leaf = new AttachableComponent();
        ui.add(leaf);

        AtomicInteger callCounter = new AtomicInteger();

        ui.beforeClientResponse(root, context -> {
            Assert.assertTrue(
                    "Root component should be marked as 'clientSideInitialized'",
                    context.isClientSideInitialized());
            callCounter.incrementAndGet();

        });
        ui.beforeClientResponse(leaf, context -> {
            Assert.assertFalse(
                    "Leaf component should NOT be marked as 'clientSideInitialized'",
                    context.isClientSideInitialized());
            callCounter.incrementAndGet();
        });
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ui.remove(root);
        ui.add(root);
        ui.beforeClientResponse(root, context -> {
            Assert.assertTrue(
                    "Reattached root component (in the same request) should be marked as 'clientSideInitialized'",
                    context.isClientSideInitialized());
            callCounter.incrementAndGet();
        });
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ui.remove(root);
        ui.getInternals().getStateTree().collectChanges(change -> {
        });
        ui.add(root);
        ui.beforeClientResponse(root, context -> {
            Assert.assertFalse(
                    "Reattached root component (in different requests) should NOT be marked as 'clientSideInitialized'",
                    context.isClientSideInitialized());
            callCounter.incrementAndGet();
        });
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        Assert.assertEquals("There should be 4 invocations", 4,
                callCounter.get());
    }

    @Test
    public void beforeClientResponse_componentNotAttachedToUi_noException() {
        UI ui = createTestUI();
        Component component = new AttachableComponent();
        ui.beforeClientResponse(component, context -> {
        });
    }

    @Test()
    public void beforeClientResponse_componentBelongsToAnotherUI_throws() {
        UI firstUI = createTestUI();
        UI anotherUI = createTestUI();
        Component component = new AttachableComponent();
        anotherUI.add(component);

        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> firstUI.beforeClientResponse(component, context -> {
                }));

        Assert.assertEquals(
                "The given component doesn't belong to the UI the task to be executed on",
                exception.getMessage());
    }

    @ListenerPriority(5)
    private static class BeforeEnterListenerFirst
            implements BeforeEnterListener {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
        }
    }

    private static class BeforeEnterListenerSecond
            implements BeforeEnterListener {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
        }
    }

    @ListenerPriority(-5)
    private static class BeforeEnterListenerThird
            implements BeforeEnterListener {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
        }
    }

    @Test
    public void before_enter_listener_priority_should_dictate_sort_order()
            throws InvalidRouteConfigurationException {
        UI ui = createTestUI();
        initUI(ui, "", null);

        ui.addBeforeEnterListener(new BeforeEnterListenerThird());
        ui.addBeforeEnterListener(new BeforeEnterListenerThird());
        ui.addBeforeEnterListener(new BeforeEnterListenerFirst());
        ui.addBeforeEnterListener(new BeforeEnterListenerSecond());

        final List<BeforeEnterHandler> beforeEnterListeners = ui
                .getNavigationListeners(BeforeEnterHandler.class);

        assertEquals(4, beforeEnterListeners.size());

        assertTrue(beforeEnterListeners
                .get(0) instanceof BeforeEnterListenerFirst);
        assertTrue(beforeEnterListeners
                .get(1) instanceof BeforeEnterListenerSecond);
        assertTrue(beforeEnterListeners
                .get(2) instanceof BeforeEnterListenerThird);
        assertTrue(beforeEnterListeners
                .get(3) instanceof BeforeEnterListenerThird);
    }

    @ListenerPriority(5)
    private static class BeforeLeaveListenerFirst
            implements BeforeLeaveListener {
        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
        }
    }

    private static class BeforeLeaveListenerSecond
            implements BeforeLeaveListener {
        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
        }
    }

    @ListenerPriority(-5)
    private static class BeforeLeaveListenerThird
            implements BeforeLeaveListener {
        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
        }
    }

    @Test
    public void before_Leave_listener_priority_should_dictate_sort_order()
            throws InvalidRouteConfigurationException {
        UI ui = createTestUI();
        initUI(ui, "", null);

        ui.addBeforeLeaveListener(new BeforeLeaveListenerFirst());
        ui.addBeforeLeaveListener(new BeforeLeaveListenerThird());
        ui.addBeforeLeaveListener(new BeforeLeaveListenerSecond());
        ui.addBeforeLeaveListener(new BeforeLeaveListenerThird());

        final List<BeforeLeaveHandler> beforeLeaveListeners = ui
                .getNavigationListeners(BeforeLeaveHandler.class);

        assertEquals(4, beforeLeaveListeners.size());

        assertTrue(beforeLeaveListeners
                .get(0) instanceof BeforeLeaveListenerFirst);
        assertTrue(beforeLeaveListeners
                .get(1) instanceof BeforeLeaveListenerSecond);
        assertTrue(beforeLeaveListeners
                .get(2) instanceof BeforeLeaveListenerThird);
        assertTrue(beforeLeaveListeners
                .get(3) instanceof BeforeLeaveListenerThird);
    }

    @ListenerPriority(5)
    private static class AfterNavigationListenerFirst
            implements AfterNavigationListener {
        @Override
        public void afterNavigation(AfterNavigationEvent event) {
        }
    }

    private static class AfterNavigationListenerSecond
            implements AfterNavigationListener {
        @Override
        public void afterNavigation(AfterNavigationEvent event) {
        }
    }

    @ListenerPriority(-5)
    private static class AfterNavigationListenerThird
            implements AfterNavigationListener {
        @Override
        public void afterNavigation(AfterNavigationEvent event) {
        }
    }

    @Test
    public void after_navigation_listener_priority_should_dictate_sort_order()
            throws InvalidRouteConfigurationException {
        UI ui = createTestUI();
        initUI(ui, "", null);

        ui.addAfterNavigationListener(new AfterNavigationListenerThird());
        ui.addAfterNavigationListener(new AfterNavigationListenerThird());
        ui.addAfterNavigationListener(new AfterNavigationListenerFirst());
        ui.addAfterNavigationListener(new AfterNavigationListenerSecond());

        final List<AfterNavigationHandler> AfterNavigationListeners = ui
                .getNavigationListeners(AfterNavigationHandler.class);

        assertEquals(4, AfterNavigationListeners.size());

        assertTrue(AfterNavigationListeners
                .get(0) instanceof AfterNavigationListenerFirst);
        assertTrue(AfterNavigationListeners
                .get(1) instanceof AfterNavigationListenerSecond);
        assertTrue(AfterNavigationListeners
                .get(2) instanceof AfterNavigationListenerThird);
        assertTrue(AfterNavigationListeners
                .get(3) instanceof AfterNavigationListenerThird);
    }

    @Test(expected = NullPointerException.class)
    public void accessLaterRunnable_nullHandler_exception() {
        UI ui = createAccessableTestUI();

        ui.accessLater((SerializableRunnable) null, () -> {
        });
    }

    @Test
    public void accessLaterRunnable_attachedUnlockedUi_runnableIsRun() {
        AtomicInteger runCount = new AtomicInteger();

        UI ui = createAccessableTestUI();
        CurrentInstance.clearAll();

        SerializableRunnable wrapped = ui.accessLater(() -> {
            assertSame("Current UI should be defined", ui, UI.getCurrent());
            runCount.incrementAndGet();
        }, null);

        assertNull("Should not have a current UI outside the caller",
                UI.getCurrent());
        assertEquals("Task should not yet have run", 0, runCount.get());

        wrapped.run();

        assertNull("Should not have a current UI outside the caller",
                UI.getCurrent());
        assertEquals("Task should have run once", 1, runCount.get());
    }

    @Test(expected = UIDetachedException.class)
    public void accessLaterRunnable_detachedUiNoHandler_throws() {
        UI ui = createTestUI();

        SerializableRunnable wrapped = ui.accessLater(
                () -> Assert.fail("Action should never run"), null);
        wrapped.run();
    }

    @Test
    public void accessLaterRunnable_detachedUi_detachHandlerCalled() {
        AtomicInteger runCount = new AtomicInteger();

        UI ui = createTestUI();

        SerializableRunnable wrapped = ui.accessLater(
                () -> Assert.fail("Action should never run"),
                runCount::incrementAndGet);

        assertEquals("Handler should not yet have run", 0, runCount.get());

        wrapped.run();

        assertEquals("Handler should have run once", 1, runCount.get());
    }

    @Test
    public void csrfToken_differentUIs_shouldBeUnique() {
        String token1 = new UI().getCsrfToken();
        String token2 = new UI().getCsrfToken();

        Assert.assertNotEquals("Each UI should have a unique CSRF token",
                token1, token2);
    }

    @Test
    public void csrfToken_sameUI_shouldBeSame() {
        UI ui = new UI();
        String token1 = ui.getCsrfToken();
        String token2 = ui.getCsrfToken();

        Assert.assertEquals(
                "getCsrfToken() should always return the same value for the same UI",
                token1, token2);
    }

    @Test(expected = NullPointerException.class)
    public void accessLaterConsumer_nullHandler_exception() {
        UI ui = createAccessableTestUI();

        ui.accessLater((SerializableConsumer<Object>) null, () -> {
        });
    }

    @Test
    public void accessLaterConsumer_attachedUnlockedUi_runnableIsRun() {
        AtomicInteger sum = new AtomicInteger();

        UI ui = createAccessableTestUI();
        CurrentInstance.clearAll();

        SerializableConsumer<Integer> wrapped = ui.accessLater(value -> {
            assertSame("Current UI should be defined", ui, UI.getCurrent());
            sum.addAndGet(value.intValue());
        }, null);

        assertNull("Should not have a current UI outside the caller",
                UI.getCurrent());
        assertEquals("Task should not yet have run", 0, sum.get());

        wrapped.accept(Integer.valueOf(5));

        assertNull("Should not have a current UI outside the caller",
                UI.getCurrent());
        assertEquals("Task should have run once", 5, sum.get());
    }

    @Test(expected = UIDetachedException.class)
    public void accessLaterConsumer_detachedUiNoHandler_throws() {
        UI ui = createTestUI();

        SerializableConsumer<Object> wrapped = ui.accessLater(
                value -> Assert.fail("Action should never run"), null);
        wrapped.accept(null);
    }

    @Test
    public void accessLaterConsumer_detachedUi_detachHandlerCalled() {
        AtomicInteger runCount = new AtomicInteger();

        UI ui = createTestUI();

        SerializableConsumer<Object> wrapped = ui.accessLater(
                value -> Assert.fail("Action should never run"),
                runCount::incrementAndGet);

        assertEquals("Handler should not yet have run", 0, runCount.get());

        wrapped.accept(null);

        assertEquals("Handler should have run once", 1, runCount.get());
    }

    @Test
    public void navigate_useParameterizedTarget_noOptionalAnnotation_navigationSucceded() {
        AtomicReference<String> loc = new AtomicReference<>();
        UI ui = new UI() {
            @Override
            public void navigate(String location) {
                loc.set(location);
            }
        };
        initUI(ui, "", null);

        ui.navigate(Parameterized.class, "baz");
        Assert.assertEquals("foo-bar/baz", loc.get());
    }

    @Test
    public void navigate_throws_illegal_argument_exception() {
        UI ui = new UI();
        initUI(ui, "", null);

        try {
            ui.navigate(Parameterized.class);
            Assert.fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().endsWith("requires a parameter."));
        }

        try {
            ui.navigate(Parameterized.class, (String) null);
            Assert.fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().endsWith("requires a parameter."));
        }

        try {
            ui.navigate(Parameterized.class, RouteParameters.empty());
            Assert.fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().endsWith("requires a parameter."));
        }

        try {
            ui.navigate(Parameterized.class,
                    new RouteParameters("some", "value"));
            Assert.fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().endsWith("requires a parameter."));
        }
    }

    @Test
    public void navigate_throws_null_pointer_exception() {
        UI ui = new UI();
        initUI(ui, "", null);

        try {
            ui.navigate((String) null);
            Assert.fail("NullPointerException expected.");
        } catch (NullPointerException e) {
            Assert.assertEquals("Location must not be null", e.getMessage());
        }

        try {
            ui.navigate((String) null, QueryParameters.empty());
            Assert.fail("NullPointerException expected.");
        } catch (NullPointerException e) {
            Assert.assertEquals("Location must not be null", e.getMessage());
        }

        try {
            ui.navigate("foo-bar", null);
            Assert.fail("NullPointerException expected.");
        } catch (NullPointerException e) {
            Assert.assertEquals("Query parameters must not be null",
                    e.getMessage());
        }
    }

    @Test
    public void navigate_throws_not_found_exception() {
        UI ui = new UI();
        initUI(ui, "", null);

        try {
            ui.navigate(FooBarParamNavigationTarget.class);
            Assert.fail("NotFoundException expected.");
        } catch (NotFoundException e) {
        }

        try {
            ui.navigate(ParameterizedNotRoute.class, 1);
            Assert.fail("NotFoundException expected.");
        } catch (NotFoundException e) {
        }

        try {
            ui.navigate(FooBarParamNavigationTarget.class,
                    new RouteParameters("fooParam", "123"));
            Assert.fail("NotFoundException expected.");
        } catch (NotFoundException e) {
        }
    }

    @Test
    public void modalComponent_addedAndRemoved_hasModalReturnsCorrectValue() {
        final TestFixture fixture = new TestFixture();
        Assert.assertTrue("Fixture should have set a modal component",
                fixture.ui.hasModalComponent());

        fixture.ui.setChildComponentModal(fixture.modalComponent, false);

        Assert.assertFalse(
                "Setting modal to false should have removed all modality",
                fixture.ui.hasModalComponent());
    }

    @Test
    public void modalComponentPresent_getActiveModalComponent_returnsExpectedComponent() {
        final TestFixture fixture = new TestFixture();
        Assert.assertEquals("modalComponent should be modal",
                fixture.modalComponent,
                fixture.ui.getInternals().getActiveModalComponent());

        fixture.ui.setChildComponentModal(fixture.routingComponent, true);

        Assert.assertEquals(
                "routingComponent should override modalComponent as active modal component",
                fixture.routingComponent,
                fixture.ui.getInternals().getActiveModalComponent());

        fixture.ui.setChildComponentModal(fixture.routingComponent, false);

        Assert.assertEquals(
                "modalComponent should return to active modal component when routingComponent made non modal",
                fixture.modalComponent,
                fixture.ui.getInternals().getActiveModalComponent());

    }

    @Test
    public void addToModalComponent_newComponentAdded_isAddedCorrectlyAsChild() {
        final TestFixture fixture = new TestFixture();
        Component test = new AttachableComponent();
        fixture.ui.addToModalComponent(test);

        final Optional<Component> testComponentParent = test.getParent();
        Assert.assertTrue("test component was not attached",
                testComponentParent.isPresent());
        Assert.assertEquals(
                "test component should have been attached to modalComponent",
                fixture.ui.getInternals().getActiveModalComponent(),
                testComponentParent.get());
    }

    @Test
    public void routingComponentVisible_modalComponentAdded_routingComponentInert() {
        final TestFixture fixture = new TestFixture();

        verifyInert(fixture.ui, false);
        verifyInert(fixture.routingComponent, false);
        verifyInert(fixture.modalComponent, false);

        fixture.collectUiChanges(); // the modal add will be visible

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);

        fixture.ui.remove(fixture.modalComponent);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, false);
        verifyInert(fixture.routingComponent, false);
        verifyInert(fixture.modalComponent, false);

        fixture.ui.addModal(fixture.modalComponent);
        fixture.collectUiChanges(); // the modal add will be visible

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);
    }

    @Test
    public void routingComponentAndModalComponentVisible_modalComponentAdded_anotherModalComponentInert() {
        final TestFixture fixture = new TestFixture();
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);

        final AttachableComponent secondModal = new AttachableComponent();
        fixture.ui.addModal(secondModal);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, true);
        verifyInert(secondModal, false);

        fixture.ui.remove(secondModal);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);
    }

    @Test
    public void modalComponentPresent_modalityChanged_routingComponentNotInert() {
        final TestFixture fixture = new TestFixture();
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);

        fixture.ui.setChildComponentModal(fixture.modalComponent, false);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, false);
        verifyInert(fixture.routingComponent, false);
        verifyInert(fixture.modalComponent, false);

        fixture.ui.setChildComponentModal(fixture.modalComponent, true);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);
    }

    @Test
    public void modalComponentsPresent_newComponentAdded_isInert() {
        final TestFixture fixture = new TestFixture();
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);

        final AttachableComponent component = new AttachableComponent();
        fixture.ui.add(component);

        // inert state inherited from UI immediately
        verifyInert(component, true);

        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);
        verifyInert(component, true);
    }

    @Test
    public void modalComponent_addedAndRemovedBeforeResponse_noInertChanged() {
        final TestFixture fixture = new TestFixture();

        verifyInert(fixture.ui, false);
        verifyInert(fixture.routingComponent, false);
        verifyInert(fixture.modalComponent, false);

        fixture.ui.remove(fixture.modalComponent);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, false);
        verifyInert(fixture.routingComponent, false);
        verifyInert(fixture.modalComponent, false);
    }

    @Test
    public void modalComponentsPresent_componentMoved_notModal() {
        final TestFixture fixture = new TestFixture();
        fixture.collectUiChanges();

        fixture.ui.add(fixture.modalComponent);

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);

        fixture.collectUiChanges();

        verifyInert(fixture.ui, false);
        verifyInert(fixture.routingComponent, false);
        verifyInert(fixture.modalComponent, false);
    }

    @Test
    public void modalComponentPresent_sameModalAddedAgain_modeless() {
        final TestFixture fixture = new TestFixture();
        fixture.collectUiChanges();

        fixture.ui.add(fixture.modalComponent);

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);

        fixture.collectUiChanges();

        verifyInert(fixture.ui, false);
        verifyInert(fixture.routingComponent, false);
        verifyInert(fixture.modalComponent, false);
    }

    @Test
    public void modalComponentPresent_toggleTopModalAgain_noChanges() {
        final TestFixture fixture = new TestFixture();
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);

        fixture.ui.setChildComponentModal(fixture.modalComponent, true);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);
    }

    @Test
    public void modelessComponentPresent_toggleModelessAgain_noChanges() {
        final TestFixture fixture = new TestFixture();
        fixture.ui.setChildComponentModal(fixture.modalComponent, false);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, false);
        verifyInert(fixture.routingComponent, false);
        verifyInert(fixture.modalComponent, false);

        fixture.ui.setChildComponentModal(fixture.modalComponent, true);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);
    }

    @Test
    public void twoModalComponents_lowerComponentModelssAndTopMostRemoved_routingComponentNotInert() {
        final TestFixture fixture = new TestFixture();
        final AttachableComponent secondModal = new AttachableComponent();
        fixture.ui.addModal(secondModal);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, true);
        verifyInert(secondModal, false);

        // (not a typical use case but tested anyway)
        // the change of modality has no effect due to another modal component
        // on top
        fixture.ui.setChildComponentModal(fixture.modalComponent, false);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, true);
        verifyInert(secondModal, false);

        fixture.ui.remove(secondModal);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, false);
        verifyInert(fixture.routingComponent, false);
        verifyInert(fixture.modalComponent, false);
    }

    @Test
    public void twoModalComponents_topComponentMoved_modalComponentSwitches() {
        final TestFixture fixture = new TestFixture();
        final AttachableComponent secondModal = new AttachableComponent();
        fixture.ui.addModal(secondModal);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, true);
        verifyInert(secondModal, false);

        fixture.ui.add(secondModal);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);
        verifyInert(secondModal, true);
    }

    @Test
    public void twoModalComponents_lowerComponentModalAgain_topComponentInert() {
        final TestFixture fixture = new TestFixture();
        final AttachableComponent secondModal = new AttachableComponent();
        fixture.ui.addModal(secondModal);
        fixture.collectUiChanges();

        fixture.ui.setChildComponentModal(fixture.modalComponent, true);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, false);
        verifyInert(secondModal, true);

        fixture.ui.remove(fixture.modalComponent);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(secondModal, false);
    }

    @Test
    public void threeModalComponents_topComponentRemoved_onlyTopMostNotInert() {
        final TestFixture fixture = new TestFixture();
        final AttachableComponent secondModal = new AttachableComponent();
        final AttachableComponent thirdModal = new AttachableComponent();
        fixture.ui.addModal(secondModal);
        fixture.ui.addModal(thirdModal);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, true);
        verifyInert(secondModal, true);
        verifyInert(thirdModal, false);

        fixture.ui.remove(thirdModal);
        fixture.collectUiChanges();

        verifyInert(fixture.ui, true);
        verifyInert(fixture.routingComponent, true);
        verifyInert(fixture.modalComponent, true);
        verifyInert(secondModal, false);
    }

    @Test
    public void getCurrentView_routingInitialized_getsCurrentRouteComponent()
            throws InvalidRouteConfigurationException {
        UI ui = new UI();
        initUI(ui, "", null);
        Component currentRoute = ui.getCurrentView();
        MatcherAssert.assertThat(currentRoute,
                CoreMatchers.instanceOf(RootNavigationTarget.class));

        ui.navigate("foo/bar");
        currentRoute = ui.getCurrentView();
        MatcherAssert.assertThat(currentRoute,
                CoreMatchers.instanceOf(FooBarNavigationTarget.class));
    }

    @Test
    public void getCurrentView_routingNotInitialized_throws()
            throws InvalidRouteConfigurationException {
        UI ui = new UI();
        Assert.assertThrows(IllegalStateException.class, ui::getCurrentView);
    }

    private void verifyInert(Component component, boolean inert) {
        Assert.assertEquals("Invalid inert state", inert,
                component.getElement().getNode().isInert());
    }

    private static class TestFixture {
        public final UI ui;
        public final Component routingComponent;
        public final Component modalComponent;

        public TestFixture() {
            ui = createTestUI();
            initUI(ui, "", null);
            routingComponent = ui.getChildren().findFirst().get();

            modalComponent = new AttachableComponent();
            ui.addModal(modalComponent);
        }

        public void collectUiChanges() {
            ui.getInternals().getStateTree().collectChanges(nodeChange -> {
            });
        }
    }
}
