package com.vaadin.ui;

import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.easymock.EasyMock;

import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.server.WrappedSession;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractSingleComponentContainerTest extends ComponentTestBase {

    private AbstractSingleComponentContainer c;
    UI ui;
    VaadinSession session;
    private VaadinServletService service;
    private VaadinServletRequest vaadinRequest;
    private WrappedHttpSession wrappedSession;
    private HttpSession httpSession;

    @SuppressWarnings("serial")
    @Before
    public void setup() {
        UI ui = new UI() {
            @Override
            protected void init(VaadinRequest request) {
            }
        };
        service = EasyMock.createMock(VaadinServletService.class);
        session = new MockVaadinSession(service);
        session.lock();
        httpSession = EasyMock.createMock(HttpSession.class);
        wrappedSession = new WrappedHttpSession(httpSession);

        vaadinRequest = new VaadinServletRequest(
                EasyMock.createMock(HttpServletRequest.class), service) {
            @Override
            public String getParameter(String name) {

                if (name.equals("v-cw") || name.equals("v-ch")) {
                    return "1000";
                }
                return "";
            }

            @Override
            public String getMethod() {
                return "POST";
            }

            @Override
            public WrappedSession getWrappedSession(
                    boolean allowSessionCreation) {
                return wrappedSession;
            }

        };

        ui.setSession(session);
        ui.doInit(vaadinRequest, session.getNextUIid(), null);
        session.addUI(ui);

        setComponent(createComponent());
        ui.setContent(getComponent());

    }

    protected void setComponent(AbstractSingleComponentContainer c) {
        this.c = c;
    }

    protected AbstractSingleComponentContainer getComponent() {
        return c;
    }

    protected AbstractSingleComponentContainer createComponent() {
        return new AbstractSingleComponentContainer() {
        };
    }

    @Test
    public void attachOnAdd() {
        AtomicInteger attach = new AtomicInteger(0);

        Button b = new Button();
        b.addAttachListener(e -> {
            attach.incrementAndGet();
        });

        getComponent().setContent(b);
        Assert.assertEquals(1, attach.get());
    }

    @Test
    public void attachToDetachedContainer() {
        AtomicInteger attach = new AtomicInteger(0);

        AbstractSingleComponentContainer secondLayout = createComponent();
        Button b = new Button();
        b.addAttachListener(e -> {
            attach.incrementAndGet();
        });

        secondLayout.setContent(b);
        getComponent().setContent(secondLayout);

        Assert.assertEquals(1, attach.get());
    }

    @Test
    public void detachOnRemove() {
        AtomicInteger detach = new AtomicInteger(0);

        Button b = new Button();
        b.addDetachListener(e -> {
            detach.incrementAndGet();
        });

        getComponent().setContent(b);
        getComponent().setContent(null);
        Assert.assertEquals(1, detach.get());

    }

    @Test
    public void setContent() {
        Button b1 = new Button();
        getComponent().setContent(b1);

        assertContent(getComponent(), b1);
    }

    private void assertContent(AbstractSingleComponentContainer component,
            Component c) {
        if (c == null) {
            Assert.assertNull(
                    "Container does not have any child, expected " + c,
                    component.getContent());
        } else {
            Assert.assertEquals(c, component.getContent());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void addParentInsideChild() {
        AbstractSingleComponentContainer other = createComponent();
        other.setContent(getComponent());
        getComponent().setContent(other);
    }

    @Test
    public void removeChild() {
        Button b1 = new Button();
        getComponent().setContent(b1);
        getComponent().setContent(null);

        assertContent(getComponent(), null);
    }

}
