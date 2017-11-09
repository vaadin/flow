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
package com.vaadin.flow.router.event;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.router.event.BeforeNavigationObserver;
import com.vaadin.router.event.EventUtil;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;
import com.vaadin.ui.i18n.LocaleChangeEvent;
import com.vaadin.ui.i18n.LocaleChangeObserver;

/**
 * Test event util functionality.
 */
public class EventUtilTest {

    @Tag(Tag.DIV)
    public static class Foo extends Component {
    }

    @Tag(Tag.DIV)
    public static class Bar extends Component {
    }

    @Tag("nested")
    public static class Listener extends Component
            implements BeforeNavigationObserver {
        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
        }
    }

    @Tag("nested-locale")
    public static class Locale extends Component implements LocaleChangeObserver {
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
        when(service.getInstantiator())
                .thenReturn(instantiator);
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
        Component.from(nested, Listener.class);

        List<BeforeNavigationObserver> beforeNavigationObservers = EventUtil
                .collectBeforeNavigationObservers(node);

        Assert.assertEquals("Wrong amount of listener instances found", 1,
                beforeNavigationObservers.size());
    }

    @Test
    public void collectBeforeNavigationObserversFromComponentList()
            throws Exception {
        Foo foo = new Foo();
        foo.getElement().appendChild(new Listener().getElement());
        Bar bar = new Bar();

        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Listener().getElement());

        bar.getElement().appendChild(new Foo().getElement(), nested);

        List<BeforeNavigationObserver> beforeNavigationObservers = EventUtil
                .collectBeforeNavigationObservers(Arrays.asList(foo, bar));

        Assert.assertEquals("Wrong amount of listener instances found", 2,
                beforeNavigationObservers.size());
    }

    @Test
    public void flattenChildren() throws Exception {
        Element node = new Element("root");
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);

        List<Element> elements = EventUtil.flattenChildren(node)
                .collect(Collectors.toList());

        Assert.assertEquals("Missing elements from list.", 6, elements.size());
    }

    @Test
    public void getImplementingComponents() throws Exception {
        Element node = new Element("root");
        node.appendChild(new Element("main"), new Element("menu"));
        Element nested = new Element("nested");
        nested.appendChild(new Element("nested-child"),
                new Element("nested-child-2"));

        node.appendChild(nested);
        Component.from(nested, Listener.class);

        List<BeforeNavigationObserver> listenerComponents = EventUtil
                .getImplementingComponents(EventUtil.flattenChildren(node),
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
}
