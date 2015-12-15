package com.vaadin.hummingbird.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.easymock.EasyMock;

import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.AbstractSimpleDOMComponentContainer;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentTestBase;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractSimpleDOMComponentContainerTest extends ComponentTestBase {

    private AbstractSimpleDOMComponentContainer layout;
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

        setLayout(createLayout());
        ui.setContent(getLayout());

    }

    protected void setLayout(AbstractSimpleDOMComponentContainer c) {
        layout = c;
    }

    protected AbstractSimpleDOMComponentContainer getLayout() {
        return layout;
    }

    protected AbstractSimpleDOMComponentContainer createLayout() {
        return new AbstractSimpleDOMComponentContainer() {
        };
    }

    @Test
    public void attachOnAdd() {
        AtomicInteger attach = new AtomicInteger(0);

        TextField b = new TextField();
        b.addAttachListener(e -> {
            attach.incrementAndGet();
        });

        getLayout().addComponent(b);
        Assert.assertEquals(1, attach.get());
    }

    @Test
    public void attachToDetachedLayout() {
        AtomicInteger attach = new AtomicInteger(0);

        AbstractSimpleDOMComponentContainer secondLayout = createLayout();
        TextField b = new TextField();
        b.addAttachListener(e -> {
            attach.incrementAndGet();
        });

        secondLayout.addComponent(b);
        getLayout().addComponent(secondLayout);

        Assert.assertEquals(1, attach.get());
    }

    @Test
    public void detachOnRemove() {
        AtomicInteger detach = new AtomicInteger(0);

        TextField b = new TextField();
        b.addDetachListener(e -> {
            detach.incrementAndGet();
        });

        getLayout().addComponent(b);
        getLayout().removeComponent(b);
        Assert.assertEquals(1, detach.get());

    }

    @Test
    public void removeAllComponents() {
        TextField b1 = new TextField();
        TextField b2 = new TextField();
        TextField b3 = new TextField();
        getLayout().addComponents(b1, b2, b3);
        getLayout().removeAllComponents();
        Assert.assertEquals(0, getLayout().getComponentCount());
    }

    @Test
    public void attachDetachOnMove() {
        AtomicInteger a1 = new AtomicInteger(0);
        AtomicInteger a2 = new AtomicInteger(0);
        AtomicInteger a3 = new AtomicInteger(0);
        AtomicInteger d1 = new AtomicInteger(0);
        AtomicInteger d2 = new AtomicInteger(0);
        AtomicInteger d3 = new AtomicInteger(0);

        TextField b1 = new TextField();
        TextField b2 = new TextField();
        TextField b3 = new TextField();
        getLayout().addComponent(b1);
        getLayout().addComponent(b2);
        getLayout().addComponent(b3);

        b1.addAttachListener(e -> {
            a1.incrementAndGet();
        });
        b2.addAttachListener(e -> {
            a2.incrementAndGet();
        });
        b3.addAttachListener(e -> {
            a3.incrementAndGet();
        });
        b1.addDetachListener(e -> {
            d1.incrementAndGet();
        });
        b2.addDetachListener(e -> {
            d2.incrementAndGet();
        });
        b3.addDetachListener(e -> {
            d3.incrementAndGet();
        });

        // Should detach and then attach again
        getLayout().addComponentAsFirst(b3);

        Assert.assertEquals(0, a1.get());
        Assert.assertEquals(0, d1.get());
        Assert.assertEquals(0, a2.get());
        Assert.assertEquals(0, d2.get());
        Assert.assertEquals(1, a3.get());
        Assert.assertEquals(1, d3.get());

    }

    @Test
    public void noAttachDetachOnAddingAtSamePosition() {
        AtomicInteger a1 = new AtomicInteger(0);
        AtomicInteger a2 = new AtomicInteger(0);
        AtomicInteger a3 = new AtomicInteger(0);
        AtomicInteger d1 = new AtomicInteger(0);
        AtomicInteger d2 = new AtomicInteger(0);
        AtomicInteger d3 = new AtomicInteger(0);

        TextField b1 = new TextField();
        TextField b2 = new TextField();
        TextField b3 = new TextField();
        getLayout().addComponent(b1);
        getLayout().addComponent(b2);
        getLayout().addComponent(b3);

        b1.addAttachListener(e -> {
            a1.incrementAndGet();
        });
        b2.addAttachListener(e -> {
            a2.incrementAndGet();
        });
        b3.addAttachListener(e -> {
            a3.incrementAndGet();
        });
        b1.addDetachListener(e -> {
            d1.incrementAndGet();
        });
        b2.addDetachListener(e -> {
            d2.incrementAndGet();
        });
        b3.addDetachListener(e -> {
            d3.incrementAndGet();
        });

        getLayout().addComponentAsFirst(b1);

        Assert.assertEquals(0, a1.get());
        Assert.assertEquals(0, d1.get());
        Assert.assertEquals(0, a2.get());
        Assert.assertEquals(0, d2.get());
        Assert.assertEquals(0, a3.get());
        Assert.assertEquals(0, d3.get());

    }

    @Test
    public void replaceComponent() {
        TextField b1 = new TextField();
        TextField b2 = new TextField();

        getLayout().addComponent(b1);
        getLayout().replaceComponent(b1, b2);
        assertChildren(getLayout(), b2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceComponentNotInLayout() {
        TextField b1 = new TextField();
        TextField b2 = new TextField();

        getLayout().replaceComponent(b1, b2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceWithComponentAlreadyInLayout() {
        TextField b1 = new TextField();
        TextField b2 = new TextField();
        getLayout().addComponents(b1, b2);
        getLayout().replaceComponent(b1, b2);
    }

    @Test
    public void addChild() {
        TextField b1 = new TextField();
        getLayout().addComponent(b1);

        assertChildren(getLayout(), b1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addParentInsideChild() {
        AbstractSimpleDOMComponentContainer aol2 = createLayout();
        aol2.addComponent(getLayout());
        getLayout().addComponent(aol2);
    }

    @Test
    public void removeChild() {
        TextField b1 = new TextField();
        getLayout().addComponent(b1);
        getLayout().removeComponent(b1);

        assertChildren(getLayout());
    }

    @Test
    public void addMultipleChildren() {
        TextField b1 = new TextField();
        TextField b2 = new TextField();
        TextField b3 = new TextField();
        getLayout().addComponents(b1, b2, b3);

        assertChildren(getLayout(), b1, b2, b3);
    }

    @Test
    public void moveChildUp() {
        TextField b1 = new TextField();
        TextField b2 = new TextField();
        TextField b3 = new TextField();
        getLayout().addComponents(b1, b2, b3);
        getLayout().addComponentAsFirst(b3);
        assertChildren(getLayout(), b3, b1, b2);
    }

    @Test
    public void moveChildDown() {
        TextField b1 = new TextField();
        TextField b2 = new TextField();
        TextField b3 = new TextField();
        getLayout().addComponents(b1, b2, b3);
        getLayout().addComponent(b1);
        assertChildren(getLayout(), b2, b3, b1);
    }

    private static void assertChildren(
            AbstractSimpleDOMComponentContainer container,
            Component... components) {
        Assert.assertEquals(components.length, container.getComponentCount());

        Iterator<Component> iter = container.iterator();
        for (int i = 0; i < components.length; i++) {
            Assert.assertEquals(
                    "Child " + i + " incorrect when using getComponent",
                    components[i], container.getComponent(i));
            Assert.assertEquals("Child " + i + " incorrect when using iterator",
                    components[i], iter.next());
        }
    }

    protected void assertHasClass(Element element, String className) {
        Assert.assertTrue("Elmement should have the " + className + " class",
                element.hasClass(className));
    }

    protected void assertNotHasClass(Element element, String className) {
        Assert.assertFalse(
                "Elmement should not have the " + className + " class",
                element.hasClass(className));
    }

    protected static KeyValue $(String attribute, String value) {
        return new KeyValue(attribute, value);
    }

    protected static class KeyValue {

        public String key, value;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    protected void assertAttributes(Element element,
            KeyValue... attributesAndValues) {
        Map<String, String> attrs = getAttributeMap(element);
        for (KeyValue t : attributesAndValues) {
            Assert.assertEquals(
                    "Value for attribute \"" + t.key + "\" is incorrect",
                    attrs.get(t.key), t.value);
            attrs.remove(t.key);
        }
        Assert.assertTrue("Element has extra attributes: " + getString(attrs),
                attrs.isEmpty());
    }

    protected String getString(Map<String, String> attrs) {
        String res = "";
        for (String key : attrs.keySet()) {
            res += key;
            res += "=\"";
            res += attrs.get(key);
            res += "\" ";
        }
        return res.trim();
    }

    protected Map<String, String> getAttributeMap(Element element) {
        Map<String, String> attributes = new HashMap<>();
        for (String attr : element.getAttributeNames()) {
            attributes.put(attr, element.getAttribute(attr));
        }
        return attributes;

    }

}
