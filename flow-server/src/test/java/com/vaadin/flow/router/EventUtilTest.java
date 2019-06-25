/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.mockito.Mockito.when;

/**
 * Test event util functionality.
 */
@NotThreadSafe
public class EventUtilTest {

    @Tag(Tag.DIV)
    public static class Foo extends Component {
    }

    @Tag(Tag.DIV)
    public static class Bar extends Component {
    }

    @Tag("nested")
    public static class LeaveObserver extends Component
            implements BeforeLeaveObserver {
        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
        }
    }

    @Tag("nested")
    public static class EnterObserver extends Component
            implements BeforeEnterObserver {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
        }
    }

    @Tag("nested")
    public static class AfterObserver extends Component
            implements AfterNavigationObserver {
        @Override
        public void afterNavigation(AfterNavigationEvent event) {
        }
    }

    @Tag("nested-locale")
    public static class Locale extends Component
            implements LocaleChangeObserver {
        @Override
        public void localeChange(LocaleChangeEvent event) {

        }
    }

    public static class CompositeWrapper extends Composite<EnterObserver> {

    }

    @Before
    public void setUp() {
        VaadinSession session = Mockito.mock(VaadinSession.class);
        UI ui = new UI() {
            @Override
            public VaadinSession getSession() {
                return session;
            }
        };
        VaadinService service = Mockito.mock(VaadinService.class);
        when(session.getService()).thenReturn(service);
        DefaultInstantiator instantiator = new DefaultInstantiator(service);
        when(service.getInstantiator()).thenReturn(instantiator);
        UI.setCurrent(ui);
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    public void collectBeforeNavigationObserversFromUI() throws Exception {
        UI ui = UI.getCurrent();
        Element node = ui.getElement();
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);
        Component.from(nested, LeaveObserver.class);

        List<BeforeLeaveObserver> beforeNavigationObservers = EventUtil
                .collectBeforeLeaveObservers(ui);

        Assert.assertEquals("Wrong amount of listener instances found", 1,
                beforeNavigationObservers.size());
    }

    @Test
    public void collectBeforeNavigationObserversFromUI_elementHasVirtualChildren()
            throws Exception {
        UI ui = UI.getCurrent();
        Element node = ui.getElement();
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendVirtualChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);

        node.getStateProvider().appendVirtualChild(node.getNode(),
                new Element("attached-by-id"), NodeProperties.INJECT_BY_ID,
                "id");

        Component.from(nested, LeaveObserver.class);

        List<BeforeLeaveObserver> beforeNavigationObservers = EventUtil
                .collectBeforeLeaveObservers(ui);

        Assert.assertEquals("Wrong amount of listener instances found", 1,
                beforeNavigationObservers.size());
    }

    @Test
    public void collectBeforeNavigationObserversFromChains() throws Exception {
        Foo foo = new Foo();
        EnterObserver toBeDetached = new EnterObserver();
        foo.getElement().appendChild(new EnterObserver().getElement(),
                toBeDetached.getElement());
        Bar bar = new Bar();

        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new EnterObserver().getElement());

        bar.getElement().appendChild(new Foo().getElement(), nested);

        EnterObserver toBeAttached = new EnterObserver();

        Collection<? extends HasElement> oldChain = Arrays.asList(foo,
                toBeDetached);
        Collection<? extends HasElement> newChain = Arrays.asList(foo,
                toBeAttached);
        List<BeforeEnterObserver> beforeNavigationObservers = EventUtil
                .collectBeforeEnterObservers(oldChain, newChain);

        Assert.assertEquals("Wrong amount of listener instances found", 2,
                beforeNavigationObservers.size());
    }

    @Test
    public void collectAfterNavigationObservers() {
        UI ui = UI.getCurrent();

        Element menu = new Element("menu");
        menu.appendChild(new AfterObserver().getElement());

        Element node = ui.getElement();
        node.appendChild(new Element("main"), menu);
        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);
        Component.from(nested, AfterObserver.class);

        List<AfterNavigationObserver> beforeNavigationObservers = EventUtil
                .collectAfterNavigationObservers(ui);

        Assert.assertEquals("Wrong amount of listener instances found", 2,
                beforeNavigationObservers.size());
    }

    @Test
    public void inspectChildrenHierarchy() throws Exception {
        Element node = new Element("root");
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);

        List<Element> elements = new ArrayList<>();

        EventUtil.inspectHierarchy(node, elements, element -> true);

        Assert.assertEquals("Missing elements from list.", 6, elements.size());
    }

    @Test
    public void inspectChildrenHierarchy_selective() throws Exception {
        Element node = new Element("root");
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);

        List<Element> elements = new ArrayList<>();

        EventUtil.inspectHierarchy(node, elements,
                element -> !nested.equals(element));

        Assert.assertEquals("Missing elements from list.", 3, elements.size());
    }

    @Test
    public void inspectMixedChildrenHierarchy() throws Exception {
        Element node = new Element("root");
        node.appendVirtualChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendVirtualChild(new Element("nested-virtual-child"),
                new Element("nested-virtual-child-2"));
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        nested.getStateProvider().appendVirtualChild(nested.getNode(),
                new Element("attached-by-id"), NodeProperties.INJECT_BY_ID,
                "id");
        nested.getStateProvider().appendVirtualChild(nested.getNode(),
                new Element("attached-by-id"),
                NodeProperties.TEMPLATE_IN_TEMPLATE, "");

        node.appendChild(nested);

        List<Element> elements = new ArrayList<>();

        EventUtil.inspectHierarchy(node, elements, element -> true);

        Assert.assertEquals("Missing elements from list.", 10, elements.size());
    }

    @Test
    public void getImplementingComponents() throws Exception {
        Element node = new Element("root");
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);
        Component.from(nested, EnterObserver.class);

        List<Element> elements = new ArrayList<>();

        EventUtil.inspectHierarchy(node, elements, element -> true);

        List<BeforeEnterObserver> listenerComponents = EventUtil
                .getImplementingComponents(elements.stream(),
                        BeforeEnterObserver.class)
                .collect(Collectors.toList());

        Assert.assertEquals("Wrong amount of listener instances found", 1,
                listenerComponents.size());
    }

    @Test
    public void getImplementingComponents_elementHasVirtualChildren()
            throws Exception {
        Element node = new Element("root");
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.getStateProvider().appendVirtualChild(node.getNode(), nested,
                NodeProperties.TEMPLATE_IN_TEMPLATE, "");
        Component.from(nested, EnterObserver.class);

        List<Element> elements = new ArrayList<>();

        EventUtil.inspectHierarchy(node, elements, element -> true);

        List<BeforeEnterObserver> listenerComponents = EventUtil
                .getImplementingComponents(elements.stream(),
                        BeforeEnterObserver.class)
                .collect(Collectors.toList());

        Assert.assertEquals("Wrong amount of listener instances found", 1,
                listenerComponents.size());
    }

    @Test
    public void collectLocaleChangeObserverFromElement() throws Exception {
        Element node = new Element("root");
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested-locale");
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);
        Component.from(nested, Locale.class);

        List<LocaleChangeObserver> beforeNavigationObservers = EventUtil
                .collectLocaleChangeObservers(node);

        Assert.assertEquals("Wrong amount of listener instances found", 1,
                beforeNavigationObservers.size());
    }

    @Test
    public void collectLocaleChangeObserverFromElement_elementHasVirtualChildren()
            throws Exception {
        Element node = new Element("root");
        node.appendChild(new Element("main"), new Element("menu"));
        node.appendVirtualChild(new Element("main-virtual"),
                new Element("menu-virtual"));
        Element nested = new Element("nested-locale");
        nested.appendVirtualChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendVirtualChild(nested);
        Component.from(nested, Locale.class);

        List<LocaleChangeObserver> beforeNavigationObservers = EventUtil
                .collectLocaleChangeObservers(node);

        Assert.assertEquals("Wrong amount of listener instances found", 1,
                beforeNavigationObservers.size());
    }

    @Test
    public void collectLocaleChangeObserverFromComponentList()
            throws Exception {
        Foo foo = new Foo();
        foo.getElement().appendChild(new Locale().getElement());
        Bar bar = new Bar();

        Element nested = new Element("nested-locale");
        nested.appendChild(new Element("nested-child"),
                new Locale().getElement());

        bar.getElement().appendChild(new Foo().getElement(), nested);

        List<LocaleChangeObserver> beforeNavigationObservers = EventUtil
                .collectLocaleChangeObservers(Arrays.asList(foo, bar));

        Assert.assertEquals("Wrong amount of listener instances found", 2,
                beforeNavigationObservers.size());
    }

    @Test
    public void collectLocaleChangeObserverFromComponentList_elementHasVirtualChildren()
            throws Exception {
        Foo foo = new Foo();
        foo.getElement().appendChild(new Locale().getElement());
        Bar bar = new Bar();

        Element nested = new Element("nested-locale");
        nested.appendChild(new Element("nested-child"),
                new Locale().getElement());

        bar.getElement().appendChild(new Foo().getElement(), nested);

        List<LocaleChangeObserver> beforeNavigationObservers = EventUtil
                .collectLocaleChangeObservers(Arrays.asList(foo, bar));

        Assert.assertEquals("Wrong amount of listener instances found", 2,
                beforeNavigationObservers.size());
    }

    @Test
    public void getImplementingComponents_hasComposite_originalComponentIsReturned() {
        CompositeWrapper wrapper = new CompositeWrapper();
        List<BeforeEnterObserver> components = EventUtil
                .getImplementingComponents(Stream.of(wrapper.getElement()),
                        BeforeEnterObserver.class)
                .distinct().collect(Collectors.toList());
        Assert.assertEquals(1, components.size());
        Assert.assertEquals(EnterObserver.class, components.get(0).getClass());

    }
}
