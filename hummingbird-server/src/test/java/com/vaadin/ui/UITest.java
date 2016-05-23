package com.vaadin.ui;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.server.MockServletConfig;
import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.ui.History.HistoryStateChangeEvent;

public class UITest {

    @After
    public void tearDown() {
        VaadinService service = VaadinServletService.getCurrent();
        if (service != null) {
            service.setCurrentInstances(null, null);
        }
    }

    @Test
    public void elementIsBody() {
        UI ui = new UI();

        Assert.assertEquals("body", ui.getElement().getTag());
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

            ServletConfig servletConfig = new MockServletConfig();
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

        Assert.assertEquals("foo/bar", ui.getActiveViewLocation().getPath());
    }

    @Test
    public void locationAfterServerNavigation() {
        UI ui = createAndInitTestUI("");

        ui.navigateTo("foo/bar");

        Assert.assertEquals("foo/bar", ui.getActiveViewLocation().getPath());
    }

    @Test
    public void locationAfterClientNavigation() {
        UI ui = createAndInitTestUI("");

        History history = ui.getPage().getHistory();

        history.getHistoryStateChangeHandler().onHistoryStateChange(
                new HistoryStateChangeEvent(history, null, "foo/bar"));

        Assert.assertEquals("foo/bar", ui.getActiveViewLocation().getPath());
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
                getElement().setTextContent("UI.init");
            }
        };

        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor
                .forClass(Integer.class);

        initUI(ui, "", statusCodeCaptor);

        Assert.assertTrue(ui.getElement().getTextContent().contains("404"));
        Assert.assertFalse(
                ui.getElement().getTextContent().contains("UI.init"));
        Assert.assertEquals(Integer.valueOf(404), statusCodeCaptor.getValue());
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

        Assert.assertEquals(1, events.size());
        Assert.assertEquals(ui, events.get(0).getSource());
    }

    @Test
    public void unsetSession_dettachEventIsFired() {
        UI ui = new UI();
        List<DetachEvent> events = new ArrayList<>();
        ui.addDetachListener(events::add);
        initUI(ui, "", null);

        ui.getSession().access(() -> ui.getInternals().setSession(null));
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(ui, events.get(0).getSource());
    }
}
