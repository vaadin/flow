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
package com.vaadin.flow.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.TestRouteRegistry;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

public class LocationObserverTest {

    protected Router router;
    private UI ui;

    private static List<String> eventCollector = new ArrayList<>(0);

    @Route("")
    @Tag(Tag.DIV)
    public static class Translations extends Component
            implements LocaleChangeObserver {

        @Override
        public void localeChange(LocaleChangeEvent event) {
            eventCollector.add("Received locale change event for locale: "
                    + event.getLocale().getDisplayName());
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class RootComponent extends Component {
    }

    public static class RouterTestUI extends MockUI {
        final Router router;

        public RouterTestUI(Router router) {
            super(createMockSession());
            this.router = router;
        }

        private static VaadinSession createMockSession() {
            MockVaadinServletService service = new MockVaadinServletService();
            service.init();
            VaadinSession session = new AlwaysLockedVaadinSession(service);
            session.setConfiguration(service.getDeploymentConfiguration());
            return session;
        }

        @Override
        public Router getRouter() {
            return router;
        }

        @Override
        public void accessSynchronously(Command command)
                throws UIDetachedException {
            // NOOP
        }
    }

    @Before
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        ui = new UI();
        eventCollector.clear();
    }

    @Test
    public void navigation_and_locale_change_should_fire_locale_change_observer()
            throws InvalidRouteConfigurationException {
        router = new Router(new TestRouteRegistry());
        ui = new RouterTestUI(router);

        RouteConfiguration.forRegistry(router.getRegistry()).setAnnotatedRoute(Translations.class);

        ui.navigate("");

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals(
                "Received locale change event for locale: "
                        + Locale.getDefault().getDisplayName(),
                eventCollector.get(0));

        ui.setLocale(Locale.CANADA);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals(
                "Received locale change event for locale: "
                        + Locale.CANADA.getDisplayName(),
                eventCollector.get(1));
    }

    @Test
    public void location_change_should_only_fire_if_location_actually_changed() {
        ui.add(new Translations());

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals(
                "Received locale change event for locale: "
                        + Locale.getDefault().getDisplayName(),
                eventCollector.get(0));

        ui.setLocale(ui.getLocale());

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());

        ui.setLocale(Locale.FRENCH);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals(
                "Received locale change event for locale: "
                        + Locale.FRENCH.getDisplayName(),
                eventCollector.get(1));
    }

    @Test
    public void location_change_should_be_fired_also_on_component_attach() {
        RootComponent root = new RootComponent();

        ui.add(root);

        Assert.assertEquals("Expected event amount was wrong", 0,
                eventCollector.size());

        root.getElement().appendChild(new Translations().getElement());

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals(
                "Received locale change event for locale: "
                        + Locale.getDefault().getDisplayName(),
                eventCollector.get(0));
    }

    @Test
    public void location_change_should_be_fired_also_on_consequent_component_attach() {
        RootComponent root = new RootComponent();

        ui.add(root);

        Assert.assertEquals(
                "No change observers so no events should be gotten.", 0,
                eventCollector.size());

        Translations translations = new Translations();
        root.getElement().appendChild(translations.getElement());

        Assert.assertEquals("Observer should have been notified on attach", 1,
                eventCollector.size());

        translations.getElement().removeFromParent();

        Assert.assertEquals("No event should have been gotten for removal", 1,
                eventCollector.size());

        root.getElement().appendChild(translations.getElement());
        Assert.assertEquals("Reattach should have given an event", 2,
                eventCollector.size());

    }
}
