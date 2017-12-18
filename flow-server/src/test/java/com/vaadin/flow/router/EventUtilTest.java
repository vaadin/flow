/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.router.BeforeNavigationEvent;
import com.vaadin.flow.router.BeforeNavigationObserver;
import com.vaadin.flow.router.EventUtil;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import net.jcip.annotations.NotThreadSafe;

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
    public static class Observer extends Component
            implements BeforeNavigationObserver {
        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
        }
    }

    @Tag("nested-locale")
    public static class Locale extends Component
            implements LocaleChangeObserver {
        @Override
        public void localeChange(LocaleChangeEvent event) {

        }
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
    public void collectBeforeNavigationObserversFromElement() throws Exception {
        Element node = new Element("root");
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);
        Component.from(nested, Observer.class);

        List<BeforeNavigationObserver> beforeNavigationObservers = EventUtil
                .collectBeforeNavigationObservers(node);

        Assert.assertEquals("Wrong amount of listener instances found", 1,
                beforeNavigationObservers.size());
    }

    @Test
    public void collectBeforeNavigationObserversFromElement_elementHasVirtualChildren()
            throws Exception {
        Element node = new Element("root");
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendVirtualChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);

        node.getStateProvider().appendVirtualChild(node.getNode(),
                new Element("attached-by-id"), NodeProperties.INJECT_BY_ID,
                "id");

        Component.from(nested, Observer.class);

        List<BeforeNavigationObserver> beforeNavigationObservers = EventUtil
                .collectBeforeNavigationObservers(node);

        Assert.assertEquals("Wrong amount of listener instances found", 1,
                beforeNavigationObservers.size());
    }

    @Test
    public void collectBeforeNavigationObserversFromComponentList()
            throws Exception {
        Foo foo = new Foo();
        foo.getElement().appendChild(new Observer().getElement());
        Bar bar = new Bar();

        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Observer().getElement());

        bar.getElement().appendChild(new Foo().getElement(), nested);

        List<BeforeNavigationObserver> beforeNavigationObservers = EventUtil
                .collectBeforeNavigationObservers(Arrays.asList(foo, bar));

        Assert.assertEquals("Wrong amount of listener instances found", 2,
                beforeNavigationObservers.size());
    }

    @Test
    public void collectBeforeNavigationObserversFromComponentList_elementHasVirtualChildren()
            throws Exception {
        Foo foo = new Foo();
        foo.getElement().getStateProvider().appendVirtualChild(
                foo.getElement().getNode(), new Observer().getElement(),
                NodeProperties.INJECT_BY_ID, "id");
        Bar bar = new Bar();

        Element nested = new Element("nested");
        nested.appendVirtualChild(new Element("nested-child"),
                new Observer().getElement());

        bar.getElement().appendChild(new Foo().getElement(), nested);

        List<BeforeNavigationObserver> beforeNavigationObservers = EventUtil
                .collectBeforeNavigationObservers(Arrays.asList(foo, bar));

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

        EventUtil.inspectHierarchy(node, elements);

        Assert.assertEquals("Missing elements from list.", 6, elements.size());
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

        EventUtil.inspectHierarchy(node, elements);

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
        Component.from(nested, Observer.class);

        List<Element> elements = new ArrayList<>();

        EventUtil.inspectHierarchy(node, elements);

        List<BeforeNavigationObserver> listenerComponents = EventUtil
                .getImplementingComponents(elements.stream(),
                        BeforeNavigationObserver.class)
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
        Component.from(nested, Observer.class);

        List<Element> elements = new ArrayList<>();

        EventUtil.inspectHierarchy(node, elements);

        List<BeforeNavigationObserver> listenerComponents = EventUtil
                .getImplementingComponents(elements.stream(),
                        BeforeNavigationObserver.class)
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
}
