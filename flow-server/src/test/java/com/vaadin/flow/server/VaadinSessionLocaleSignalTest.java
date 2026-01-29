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

import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.signals.Signal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

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
        Signal<Locale> signal = session.localeSignal();

        assertNotNull("localeSignal() should never return null", signal);
        assertEquals("Signal value should match getLocale()",
                session.getLocale(), signal.value());
    }

    @Test
    public void localeSignal_setLocale_signalUpdated() {
        VaadinSession session = getSession();
        Signal<Locale> signal = session.localeSignal();

        Locale initialLocale = session.getLocale();
        Locale newLocale = Locale.FRENCH;

        // Ensure we're actually changing the locale
        if (initialLocale.equals(newLocale)) {
            newLocale = Locale.GERMAN;
        }

        session.setLocale(newLocale);

        assertEquals("Signal should reflect the new locale after setLocale()",
                newLocale, signal.value());
        assertEquals("getLocale() should also return the new locale", newLocale,
                session.getLocale());
    }

    @Test
    public void localeSignal_sameInstance_returnedOnMultipleCalls() {
        VaadinSession session = getSession();

        Signal<Locale> signal1 = session.localeSignal();
        Signal<Locale> signal2 = session.localeSignal();

        assertSame("localeSignal() should return the same instance on "
                + "multiple calls", signal1, signal2);
    }

    @Test
    public void localeSignal_returnTypeIsReadOnlySignal() {
        VaadinSession session = getSession();

        // The return type of localeSignal() is Signal<Locale>, not
        // WritableSignal<Locale>. This is enforced at compile time.
        // This test verifies that the method returns a non-null Signal
        // and that we can read from it (the read-only contract).
        Signal<Locale> signal = session.localeSignal();
        assertNotNull("localeSignal() should return a non-null Signal", signal);

        // Verify we can read the value
        Locale value = signal.value();
        assertEquals("Signal value should match getLocale()",
                session.getLocale(), value);
    }

    @Test
    public void localeSignal_multipleLocaleChanges_signalFollows() {
        VaadinSession session = getSession();
        Signal<Locale> signal = session.localeSignal();

        session.setLocale(Locale.FRENCH);
        assertEquals(Locale.FRENCH, signal.value());

        session.setLocale(Locale.GERMAN);
        assertEquals(Locale.GERMAN, signal.value());

        session.setLocale(Locale.JAPANESE);
        assertEquals(Locale.JAPANESE, signal.value());
    }

    @Test
    public void localeSignal_setLocaleLazy_signalCreatedWithCurrentValue() {
        VaadinSession session = getSession();

        // Set locale before accessing the signal
        session.setLocale(Locale.ITALIAN);

        // Now access the signal - it should have the Italian locale
        Signal<Locale> signal = session.localeSignal();

        assertEquals("Signal should be initialized with current locale",
                Locale.ITALIAN, signal.value());
    }
}
