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
package com.vaadin.flow.server;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.Signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link VaadinSession#localeSignal()}.
 */
class VaadinSessionLocaleSignalTest extends SignalsUnitTest {

    private VaadinSession getSession() {
        return UI.getCurrent().getSession();
    }

    @Test
    void localeSignal_initialValue_matchesGetLocale() {
        VaadinSession session = getSession();
        Signal<Locale> signal = session.localeSignal();

        assertNotNull(signal, "localeSignal() should never return null");
        assertEquals(session.getLocale(), signal.peek(),
                "Signal value should match getLocale()");
    }

    @Test
    void localeSignal_setLocale_signalUpdated() {
        VaadinSession session = getSession();
        Signal<Locale> signal = session.localeSignal();

        Locale initialLocale = session.getLocale();
        Locale newLocale = Locale.FRENCH;

        // Ensure we're actually changing the locale
        if (initialLocale.equals(newLocale)) {
            newLocale = Locale.GERMAN;
        }

        session.setLocale(newLocale);

        assertEquals(newLocale, signal.peek(),
                "Signal should reflect the new locale after setLocale()");
        assertEquals(newLocale, session.getLocale(),
                "getLocale() should also return the new locale");
    }

    @Test
    void localeSignal_multipleLocaleChanges_signalFollows() {
        VaadinSession session = getSession();
        Signal<Locale> signal = session.localeSignal();

        session.setLocale(Locale.FRENCH);
        assertEquals(Locale.FRENCH, signal.peek());

        session.setLocale(Locale.GERMAN);
        assertEquals(Locale.GERMAN, signal.peek());

        session.setLocale(Locale.JAPANESE);
        assertEquals(Locale.JAPANESE, signal.peek());
    }
}
