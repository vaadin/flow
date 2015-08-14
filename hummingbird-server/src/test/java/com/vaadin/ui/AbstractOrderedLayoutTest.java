package com.vaadin.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTest;
import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.server.communication.UIInitHandler;

public class AbstractOrderedLayoutTest extends ComponentTestBase {

    AbstractOrderedLayout aol;
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

                if (UIInitHandler.BROWSER_DETAILS_PARAMETER.equals(name)) {
                    return "1";
                } else if (name.equals("v-cw") || name.equals("v-ch")) {
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

        aol = createLayout();
        ui.setContent(aol);

    }

    private AbstractOrderedLayout createLayout() {
        return new AbstractOrderedLayout() {
        };
    }

    @Test
    public void initialState() {
        ElementTest.assertElementEquals(
                ElementTest.parse("<div class='layout flex-children' />"),
                aol.getElement());
    }

    @Test
    public void initialFromDOM() {

        AbstractOrderedLayout aol2 = createLayout();
        setComponentElement(aol2, "<div class='layout flex-children' />");
        ElementTest.assertElementEquals(aol.getElement(), aol2.getElement());
    }

    @Test
    public void attachOnAdd() {
        AtomicInteger attach = new AtomicInteger(0);

        Button b = new Button();
        b.addAttachListener(e -> {
            attach.incrementAndGet();
        });

        aol.addComponent(b);
        Assert.assertEquals(1, attach.get());
    }

    @Test
    public void attachToDetachedLayout() {
        AtomicInteger attach = new AtomicInteger(0);

        AbstractOrderedLayout secondLayout = createLayout();
        Button b = new Button();
        b.addAttachListener(e -> {
            attach.incrementAndGet();
        });

        secondLayout.addComponent(b);
        aol.addComponent(secondLayout);

        Assert.assertEquals(1, attach.get());
    }

    @Test
    public void detachOnRemove() {
        AtomicInteger detach = new AtomicInteger(0);

        Button b = new Button();
        b.addDetachListener(e -> {
            detach.incrementAndGet();
        });

        aol.addComponent(b);
        aol.removeComponent(b);
        Assert.assertEquals(1, detach.get());

    }

    @Test
    public void attachDetachOnMove() {
        AtomicInteger a1 = new AtomicInteger(0);
        AtomicInteger a2 = new AtomicInteger(0);
        AtomicInteger a3 = new AtomicInteger(0);
        AtomicInteger d1 = new AtomicInteger(0);
        AtomicInteger d2 = new AtomicInteger(0);
        AtomicInteger d3 = new AtomicInteger(0);

        Button b1 = new Button();
        Button b2 = new Button();
        Button b3 = new Button();
        aol.addComponent(b1);
        aol.addComponent(b2);
        aol.addComponent(b3);

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
        aol.addComponentAsFirst(b3);

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

        Button b1 = new Button();
        Button b2 = new Button();
        Button b3 = new Button();
        aol.addComponent(b1);
        aol.addComponent(b2);
        aol.addComponent(b3);

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

        aol.addComponentAsFirst(b1);

        Assert.assertEquals(0, a1.get());
        Assert.assertEquals(0, d1.get());
        Assert.assertEquals(0, a2.get());
        Assert.assertEquals(0, d2.get());
        Assert.assertEquals(0, a3.get());
        Assert.assertEquals(0, d3.get());

    }

    @Test
    public void setExpand() {
        Button button = new Button();
        aol.addComponent(button);

        aol.setExpandRatio(button, 1);
        Assert.assertEquals(1, aol.getExpandRatio(button));
        assertHasClass(button.getElement(), "flex-1");
    }

    @Test
    public void changeExpand() {
        Button button = new Button();
        aol.addComponent(button);
        aol.setExpandRatio(button, 1);
        Element domBefore = ElementTest
                .parse(button.getElement().getOuterHTML());
        aol.setExpandRatio(button, 2);
        domBefore.removeClass("flex-1").addClass("flex-2");
        ElementTest.assertElementEquals(domBefore, button.getElement());

        Assert.assertEquals(2, aol.getExpandRatio(button));
        assertNotHasClass(button.getElement(), "flex-1");
        assertHasClass(button.getElement(), "flex-2");
    }

    @Test
    public void removeExpand() {
        Button button = new Button();
        aol.addComponent(button);
        aol.setExpandRatio(button, 1);
        aol.setExpandRatio(button, 0);

        Assert.assertEquals(0, aol.getExpandRatio(button));
        assertNotHasClass(button.getElement(), "flex-1");
        assertNotHasClass(button.getElement(), "flex-0");
        assertHasClass(aol.getElement(), "flex-children");
    }

    @Test
    public void allExpandInitially() {
        Button b1 = new Button();
        Button b2 = new Button();
        Button b3 = new Button();
        aol.addComponents(b1, b2, b3);

        Assert.assertEquals(0, aol.getExpandRatio(b1));
        Assert.assertEquals(0, aol.getExpandRatio(b2));
        Assert.assertEquals(0, aol.getExpandRatio(b3));
        assertHasClass(aol.getElement(),
                AbstractOrderedLayout.CLASS_FLEX_CHILDREN);
    }

    @Test
    public void setAlignment() {
        Button button = new Button();
        aol.addComponent(button);

        aol.setComponentAlignment(button, Alignment.BOTTOM_RIGHT);
        Assert.assertEquals(Alignment.BOTTOM_RIGHT,
                aol.getComponentAlignment(button));
        assertHasClass(button.getElement(), "bottom-right");
    }

    @Test
    public void changeAlignment() {
        Button button = new Button();
        aol.addComponent(button);
        aol.setComponentAlignment(button, Alignment.BOTTOM_CENTER);
        Element domBefore = ElementTest
                .parse(button.getElement().getOuterHTML());
        aol.setComponentAlignment(button, Alignment.MIDDLE_LEFT);
        domBefore.removeClass(Alignment.BOTTOM_CENTER.getClassName())
                .addClass(Alignment.MIDDLE_LEFT.getClassName());
        ElementTest.assertElementEquals(domBefore, button.getElement());

        Assert.assertEquals(Alignment.MIDDLE_LEFT,
                aol.getComponentAlignment(button));
        assertNotHasClass(button.getElement(), "bottom-center");
        assertHasClass(button.getElement(), "middle-left");
    }

    @Test
    public void removeAlignment() {
        Button button = new Button();
        aol.addComponent(button);
        aol.setComponentAlignment(button, Alignment.BOTTOM_CENTER);
        aol.setComponentAlignment(button,
                AbstractOrderedLayout.ALIGNMENT_DEFAULT);

        Assert.assertEquals(AbstractOrderedLayout.ALIGNMENT_DEFAULT,
                aol.getComponentAlignment(button));
        assertNotHasClass(button.getElement(), "bottom-center");
        assertNotHasClass(button.getElement(), "top-left");
    }

    @Test
    public void replaceComponent() {
        Button b1 = new Button("First");
        Button b2 = new Button("Second");

        aol.addComponent(b1);
        aol.replaceComponent(b1, b2);
        assertChildren(aol, b2);
    }

    @Test
    public void replaceComponentRetainsExpand() {
        Button b1 = new Button("First");
        Button b2 = new Button("Second");

        aol.addComponent(b1);
        aol.setExpandRatio(b1, 5);

        aol.replaceComponent(b1, b2);

        Assert.assertEquals(5, aol.getExpandRatio(b2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceComponentNotInLayout() {
        Button b1 = new Button("First");
        Button b2 = new Button("Second");

        aol.replaceComponent(b1, b2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceWithComponentAlreadyInLayout() {
        Button b1 = new Button("First");
        Button b2 = new Button("Second");
        aol.addComponents(b1, b2);
        aol.replaceComponent(b1, b2);
    }

    @Test
    public void setExpandWhenChildHasClass() {
        Button button = new Button();
        button.getElement().setAttribute("class", "buttonclass");
        aol.addComponent(button);
        aol.setExpandRatio(button, 1);

        assertHasClass(button.getElement(), "buttonclass");
        assertHasClass(button.getElement(), "flex-1");
    }

    @Test
    public void addChild() {
        Button b1 = new Button();
        aol.addComponent(b1);

        assertChildren(aol, b1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addParentInsideChild() {
        AbstractOrderedLayout aol2 = createLayout();
        aol2.addComponent(aol);
        aol.addComponent(aol2);
    }

    @Test
    public void removeChild() {
        Button b1 = new Button();
        aol.addComponent(b1);
        aol.removeComponent(b1);

        assertChildren(aol);
    }

    @Test
    public void addMultipleChildren() {
        Button b1 = new Button();
        Button b2 = new Button();
        Button b3 = new Button();
        aol.addComponents(b1, b2, b3);

        assertChildren(aol, b1, b2, b3);
    }

    @Test
    public void moveChildUp() {
        Button b1 = new Button();
        Button b2 = new Button();
        Button b3 = new Button();
        aol.addComponents(b1, b2, b3);
        aol.addComponentAsFirst(b3);
        assertChildren(aol, b3, b1, b2);
    }

    @Test
    public void moveChildDown() {
        Button b1 = new Button();
        Button b2 = new Button();
        Button b3 = new Button();
        aol.addComponents(b1, b2, b3);
        aol.addComponent(b1);
        assertChildren(aol, b2, b3, b1);
    }

    private static void assertChildren(AbstractOrderedLayout container,
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

    private void assertHasClass(Element element, String className) {
        Assert.assertTrue("Elmement should have the " + className + " class",
                element.hasClass(className));
    }

    private void assertNotHasClass(Element element, String className) {
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
