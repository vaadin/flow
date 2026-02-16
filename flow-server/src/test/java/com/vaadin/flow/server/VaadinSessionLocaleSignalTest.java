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
import com.vaadin.flow.signals.WritableSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link VaadinSession#localeSignal()}.
 */
public class VaadinSessionLocaleSignalTest extends SignalsUnitTest {

    private VaadinSession getSession() {
        return UI.getCurrent().getSession();
    }

    @Test
    public void localeSignal_initialValue_matchesGetLocale() {
        VaadinSession session = getSession();
        WritableSignal<Locale> signal = session.localeSignal();

        assertNotNull(signal, "localeSignal() should never return null");
        assertEquals(session.getLocale(), signal.get(),
                "Signal value should match getLocale()");
    }

    @Test
    public void localeSignal_setLocale_signalUpdated() {
        VaadinSession session = getSession();
        WritableSignal<Locale> signal = session.localeSignal();

        Locale initialLocale = session.getLocale();
        Locale newLocale = Locale.FRENCH;

        // Ensure we're actually changing the locale
        if (initialLocale.equals(newLocale)) {
            newLocale = Locale.GERMAN;
        }

        session.setLocale(newLocale);

        assertEquals(newLocale, signal.get(),
                "Signal should reflect the new locale after setLocale()");
        assertEquals(newLocale, session.getLocale(),
                "getLocale() should also return the new locale");
    }

    @Test
    public void localeSignal_writeToSignal_updatesGetLocale() {
        VaadinSession session = getSession();
        WritableSignal<Locale> signal = session.localeSignal();

        Locale initialLocale = session.getLocale();
        Locale newLocale = Locale.FRENCH;

        // Ensure we're actually changing the locale
        if (initialLocale.equals(newLocale)) {
            newLocale = Locale.GERMAN;
        }

        signal.set(newLocale);

        assertEquals(newLocale, session.getLocale(),
                "getLocale() should reflect the new locale after "
                        + "writing to signal");
        assertEquals(newLocale, signal.get(),
                "Signal should have the new value");
    }

    @Test
    public void localeSignal_sameInstance_returnedOnMultipleCalls() {
        VaadinSession session = getSession();

        WritableSignal<Locale> signal1 = session.localeSignal();
        WritableSignal<Locale> signal2 = session.localeSignal();

        assertSame(signal1, signal2,
                "localeSignal() should return the same instance on "
                        + "multiple calls");
    }

    @Test
    public void localeSignal_multipleLocaleChanges_signalFollows() {
        VaadinSession session = getSession();
        WritableSignal<Locale> signal = session.localeSignal();

        session.setLocale(Locale.FRENCH);
        assertEquals(Locale.FRENCH, signal.get());

        session.setLocale(Locale.GERMAN);
        assertEquals(Locale.GERMAN, signal.get());

        session.setLocale(Locale.JAPANESE);
        assertEquals(Locale.JAPANESE, signal.get());
    }

    @Test
    public void localeSignal_multipleSignalWrites_getLocaleFollows() {
        VaadinSession session = getSession();
        WritableSignal<Locale> signal = session.localeSignal();

        signal.set(Locale.FRENCH);
        assertEquals(Locale.FRENCH, session.getLocale());

        signal.set(Locale.GERMAN);
        assertEquals(Locale.GERMAN, session.getLocale());

        signal.set(Locale.JAPANESE);
        assertEquals(Locale.JAPANESE, session.getLocale());
    }
}
