package com.vaadin.flow.component;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;

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
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveListener;
import com.vaadin.flow.router.ListenerPriority;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.internal.AfterNavigationHandler;
import com.vaadin.flow.router.internal.BeforeEnterHandler;
import com.vaadin.flow.router.internal.BeforeLeaveHandler;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.MockServletConfig;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class UITest {

    @Route("")
    @Tag(Tag.DIV)
    public static class RootNavigationTarget extends Component {

    }

    @Route("foo/bar")
    @Tag(Tag.DIV)
    public static class FooBarNavigationTarget extends Component {

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
        UI ui = new UI() {
            @Override
            public void doInit(VaadinRequest request, int uiId) {

            }
        };

        return ui;
    }

    private static MockUI createAccessableTestUI() {
        // Needs a service to be able to do service.accessSession
        return new MockUI(
                new MockVaadinSession(new MockVaadinServletService()));
    }

    private static void initUI(UI ui, String initialLocation,
            ArgumentCaptor<Integer> statusCodeCaptor)
            throws InvalidRouteConfigurationException {
        try {
            VaadinServletRequest request = Mockito
                    .mock(VaadinServletRequest.class);
            VaadinResponse response = Mockito.mock(VaadinResponse.class);

            String pathInfo;
            if (initialLocation.isEmpty()) {
                pathInfo = null;
            } else {
                Assert.assertFalse(initialLocation.startsWith("/"));
                pathInfo = "/" + initialLocation;
            }
            Mockito.when(request.getPathInfo()).thenReturn(pathInfo);

            ServletConfig servletConfig = new MockServletConfig();
            VaadinServlet servlet = new VaadinServlet();
            servlet.init(servletConfig);
            VaadinService service = servlet.getService();
            service.setCurrentInstances(request, response);

            MockVaadinSession session = new AlwaysLockedVaadinSession(service);

            DeploymentConfiguration config = Mockito
                    .mock(DeploymentConfiguration.class);
            Mockito.when(config.isProductionMode()).thenReturn(false);

            session.lock();
            session.setConfiguration(config);

            ui.getInternals().setSession(session);

            RouteConfiguration routeConfiguration = RouteConfiguration
                    .forRegistry(ui.getRouter().getRegistry());

            routeConfiguration.update(() -> {
                routeConfiguration.getHandledRegistry().clean();
                Arrays.asList(RootNavigationTarget.class,
                        FooBarNavigationTarget.class)
                        .forEach(routeConfiguration::setAnnotatedRoute);
            });

            ui.doInit(request, 0);
            ui.getRouter().initializeUI(ui, request);

            session.unlock();

            if (statusCodeCaptor != null) {
                Mockito.verify(response).setStatus(statusCodeCaptor.capture());
            }
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
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
        Assert.assertThat(chain.get(0),
                CoreMatchers.instanceOf(FooBarNavigationTarget.class));
    }

    @Test
    public void navigateWithParameters_delegateToRouter() {
        final String route = "params";
        Router router = Mockito.mock(Router.class);
        UI ui = new MockUI() {
            @Override
            public com.vaadin.flow.router.Router getRouter() {
                return router;
            }
        };
        QueryParameters params = QueryParameters
                .simple(Collections.singletonMap("test", "indeed"));

        ArgumentCaptor<Location> location = ArgumentCaptor
                .forClass(Location.class);

        ui.navigate(route, params);

        Mockito.verify(router).navigate(Matchers.eq(ui), location.capture(),
                Matchers.eq(NavigationTrigger.PROGRAMMATIC));

        Location value = location.getValue();
        Assert.assertEquals(route, value.getPath());
        Assert.assertEquals(params, value.getQueryParameters());
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
        Assert.assertThat(errorComponent.get(),
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
    public void unserSession_datachEventIsFiredForElements() {
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
}
