/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.TestRouteRegistry;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.tests.util.MockUI;

class LocationObserverTest {

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
            Assertions.assertNotNull(event.getUI());
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class RootComponent extends Component {
    }

    @BeforeEach
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        ui = new MockUI();
        eventCollector.clear();
    }

    @Test
    public void navigation_and_locale_change_should_fire_locale_change_observer()
            throws InvalidRouteConfigurationException {
        router = new Router(new TestRouteRegistry());
        ui = new MockUI(router);

        RouteConfiguration.forRegistry(router.getRegistry())
                .setAnnotatedRoute(Translations.class);

        ui.navigate("");

        Assertions.assertEquals(1, eventCollector.size(),
                "Expected event amount was wrong");
        Assertions.assertEquals(
                "Received locale change event for locale: "
                        + Locale.getDefault().getDisplayName(),
                eventCollector.get(0));

        ui.setLocale(Locale.CANADA);

        Assertions.assertEquals(2, eventCollector.size(),
                "Expected event amount was wrong");
        Assertions.assertEquals(
                "Received locale change event for locale: "
                        + Locale.CANADA.getDisplayName(),
                eventCollector.get(1));
    }

    @Test
    public void location_change_should_only_fire_if_location_actually_changed() {
        ui.add(new Translations());

        Assertions.assertEquals(1, eventCollector.size(),
                "Expected event amount was wrong");
        Assertions.assertEquals(
                "Received locale change event for locale: "
                        + Locale.getDefault().getDisplayName(),
                eventCollector.get(0));

        ui.setLocale(ui.getLocale());

        Assertions.assertEquals(1, eventCollector.size(),
                "Expected event amount was wrong");

        ui.setLocale(Locale.FRENCH);

        Assertions.assertEquals(2, eventCollector.size(),
                "Expected event amount was wrong");
        Assertions.assertEquals(
                "Received locale change event for locale: "
                        + Locale.FRENCH.getDisplayName(),
                eventCollector.get(1));
    }

    @Test
    public void location_change_should_be_fired_also_on_component_attach() {
        RootComponent root = new RootComponent();

        ui.add(root);

        Assertions.assertEquals(0, eventCollector.size(),
                "Expected event amount was wrong");

        root.getElement().appendChild(new Translations().getElement());

        Assertions.assertEquals(1, eventCollector.size(),
                "Expected event amount was wrong");
        Assertions.assertEquals(
                "Received locale change event for locale: "
                        + Locale.getDefault().getDisplayName(),
                eventCollector.get(0));
    }

    @Test
    public void location_change_should_be_fired_also_on_consequent_component_attach() {
        RootComponent root = new RootComponent();

        ui.add(root);

        Assertions.assertEquals(0, eventCollector.size(),
                "No change observers so no events should be gotten.");

        Translations translations = new Translations();
        root.getElement().appendChild(translations.getElement());

        Assertions.assertEquals(1, eventCollector.size(),
                "Observer should have been notified on attach");

        translations.getElement().removeFromParent();

        Assertions.assertEquals(1, eventCollector.size(),
                "No event should have been gotten for removal");

        root.getElement().appendChild(translations.getElement());
        Assertions.assertEquals(2, eventCollector.size(),
                "Reattach should have given an event");

    }
}
