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
package com.vaadin.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.router.Route;
import com.vaadin.router.Router;
import com.vaadin.router.RouterInterface;
import com.vaadin.router.RouterTest;
import com.vaadin.router.TestRouteRegistry;
import com.vaadin.server.Command;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.server.MockVaadinServletService;
import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinSession;
import com.vaadin.tests.util.MockUI;
import com.vaadin.ui.i18n.LocaleChangeEvent;
import com.vaadin.ui.i18n.LocaleChangeObserver;

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

    public static class RouterTestUI extends MockUI {
        final Router router;

        public RouterTestUI(Router router) {
            super(createMockSession());
            this.router = router;
        }

        private static VaadinSession createMockSession() {
            MockVaadinServletService service = new MockVaadinServletService();
            service.init();
            return new MockVaadinSession(service);
        }

        @Override
        public Optional<RouterInterface> getRouterInterface() {
            return Optional.of(router);
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
        router = new Router(new TestRouteRegistry());
        ui = new RouterTestUI(router);
        eventCollector.clear();
    }

    @Test
    public void navigation_and_locale_change_should_fire_locale_change_observer()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(Translations.class));

        ui.navigateTo("");

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
    public void location_change_should_only_fire_if_location_actually_changed()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(Translations.class));

        ui.navigateTo("");

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
}
