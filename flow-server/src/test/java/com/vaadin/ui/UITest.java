package com.vaadin.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.dom.NodeVisitor;
import com.vaadin.flow.dom.impl.AbstractTextElementStateProvider;
import com.vaadin.flow.util.CurrentInstance;
import com.vaadin.router.Location;
import com.vaadin.router.NavigationTrigger;
import com.vaadin.router.QueryParameters;
import com.vaadin.server.Constants;
import com.vaadin.server.MockServletConfig;
import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.History.HistoryStateChangeEvent;
import com.vaadin.ui.event.AttachEvent;
import com.vaadin.ui.event.DetachEvent;

public class UITest {

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
        public void visit(StateNode node, NodeVisitor visitor,
                boolean visitDescendants) {
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

    private static UI createAndInitTestUI(String initialLocation) {
        UI ui = new UI();

        initUI(ui, initialLocation, null);

        return ui;
    }

    private static void initUI(UI ui, String initialLocation,
            ArgumentCaptor<Integer> statusCodeCaptor) {
        try {
            VaadinRequest request = Mockito.mock(VaadinRequest.class);
            VaadinResponse response = Mockito.mock(VaadinResponse.class);

            String pathInfo;
            if (initialLocation.isEmpty()) {
                pathInfo = null;
            } else {
                Assert.assertFalse(initialLocation.startsWith("/"));
                pathInfo = "/" + initialLocation;
            }
            Mockito.when(request.getPathInfo()).thenReturn(pathInfo);

            Properties initParams = new Properties();
            initParams.setProperty(
                    Constants.SERVLET_PARAMETER_USING_NEW_ROUTING, "false");
            ServletConfig servletConfig = new MockServletConfig(initParams);
            VaadinServlet servlet = new VaadinServlet();
            servlet.init(servletConfig);
            VaadinService service = servlet.getService();
            service.setCurrentInstances(request, response);

            service.getRouter().reconfigure(c -> {
            });

            MockVaadinSession session = new MockVaadinSession(service);

            ui.getInternals().setSession(session);

            ui.doInit(request, 0);

            if (statusCodeCaptor != null) {
                Mockito.verify(response).setStatus(statusCodeCaptor.capture());
            }
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testInitialLocation() {
        UI ui = createAndInitTestUI("foo/bar");

        assertEquals("foo/bar",
                ui.getInternals().getActiveViewLocation().getPath());
    }

    @Test
    public void locationAfterServerNavigation() {
        UI ui = createAndInitTestUI("");

        ui.navigateTo("foo/bar");

        assertEquals("foo/bar",
                ui.getInternals().getActiveViewLocation().getPath());
    }

    private boolean requestHandled;

    @Test
    public void navigateWithParameters() {
        requestHandled = false;
        final String route = "params";
        UI ui = createAndInitTestUI(route);
        QueryParameters params = QueryParameters
                .simple(Collections.singletonMap("test", "indeed"));

        ui.getRouterInterface().get()
                .reconfigure(c -> c.setRoute(route, event -> {
                    assertEquals(params.getParameters(), event.getLocation()
                            .getQueryParameters().getParameters());
                    requestHandled = true;
                    return HttpServletResponse.SC_OK;
                }));

        ui.navigateTo(route, params);

        assertEquals(route,
                ui.getInternals().getActiveViewLocation().getPath());
        assertTrue("Request with QueryParameters was not handled.",
                requestHandled);
    }

    @Test
    public void locationAfterClientNavigation() {
        UI ui = createAndInitTestUI("");

        History history = ui.getPage().getHistory();

        history.getHistoryStateChangeHandler()
                .onHistoryStateChange(new HistoryStateChangeEvent(history, null,
                        new Location("foo/bar"), NavigationTrigger.HISTORY));

        assertEquals("foo/bar",
                ui.getInternals().getActiveViewLocation().getPath());
    }

    @Test
    public void testInvalidNavigationTargets() {
        String[] invalidTargets = { null, "/foo", "foo/bar/.." };
        for (String invalidTarget : invalidTargets) {
            UI ui = createAndInitTestUI("");
            try {
                ui.navigateTo(invalidTarget);
                Assert.fail("Navigation target should cause exception: "
                        + invalidTarget);
            } catch (IllegalArgumentException expected) {
                // All is fine
            }
        }
    }

    @Test
    public void testUiInitWithConfiguredRouter_noRouteMatches_404ViewAndCodeReturned() {
        UI ui = new UI() {
            @Override
            protected void init(VaadinRequest request) {
                getElement().setText("UI.init");
            }
        };

        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor
                .forClass(Integer.class);

        initUI(ui, "", statusCodeCaptor);

        Assert.assertTrue(ui.getElement().getTextRecursively().contains("404"));
        Assert.assertFalse(
                ui.getElement().getTextRecursively().contains("UI.init"));
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

    @Test(expected = IllegalArgumentException.class)
    public void removeNotChildComponent() {
        UI ui = new UI();
        Text text = new Text("foo");
        ui.remove(text);
    }

    @Test
    public void setSession_attachEventIsFired() {
        UI ui = new UI();
        List<AttachEvent> events = new ArrayList<>();
        ui.addAttachListener(events::add);
        initUI(ui, "", null);

        assertEquals(1, events.size());
        assertEquals(ui, events.get(0).getSource());
    }

    @Test
    public void unsetSession_dettachEventIsFired() {
        UI ui = new UI();
        List<DetachEvent> events = new ArrayList<>();
        ui.addDetachListener(events::add);
        initUI(ui, "", null);

        ui.getSession().access(() -> ui.getInternals().setSession(null));
        assertEquals(1, events.size());
        assertEquals(ui, events.get(0).getSource());
    }

    @Test
    public void beforeClientResponse_regularOrder() {
        UI ui = createAndInitTestUI("");
        Component rootComponent = new AttachableComponent();
        ui.add(rootComponent);

        List<Integer> results = new ArrayList<>();

        ui.beforeClientResponse(rootComponent, () -> results.add(0));
        ui.beforeClientResponse(rootComponent, () -> results.add(1));
        ui.beforeClientResponse(rootComponent, () -> results.add(2));

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
        UI ui = createAndInitTestUI("");
        Component rootComponent = new AttachableComponent();
        ui.add(rootComponent);

        List<Integer> results = new ArrayList<>();

        ui.beforeClientResponse(rootComponent, () -> results.add(0));
        ui.beforeClientResponse(rootComponent, () -> {
            results.add(1);
            ui.beforeClientResponse(rootComponent, () -> results.add(3));
            ui.beforeClientResponse(rootComponent, () -> results.add(4));
        });
        ui.beforeClientResponse(rootComponent, () -> results.add(2));

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
        UI ui = createAndInitTestUI("");
        Component rootComponent = new AttachableComponent();
        ui.add(rootComponent);
        Component emptyComponent = new AttachableComponent();

        List<Integer> results = new ArrayList<>();

        ui.beforeClientResponse(emptyComponent, () -> results.add(0));
        ui.beforeClientResponse(rootComponent, () -> results.add(1));
        ui.beforeClientResponse(emptyComponent, () -> results.add(2));
        ui.beforeClientResponse(rootComponent, () -> results.add(3));

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
        UI ui = createAndInitTestUI("");
        Component rootComponent = new AttachableComponent();
        ui.add(rootComponent);
        AttachableComponent emptyComponent1 = new AttachableComponent();
        AttachableComponent emptyComponent2 = new AttachableComponent();

        List<Integer> results = new ArrayList<>();

        ui.beforeClientResponse(emptyComponent1, () -> {
            results.add(0);
            ui.add(emptyComponent2);
        });
        ui.beforeClientResponse(rootComponent, () -> {
            results.add(1);
            ui.add(emptyComponent1);
        });
        ui.beforeClientResponse(emptyComponent2, () -> results.add(2));
        ui.beforeClientResponse(rootComponent, () -> results.add(3));

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
}
