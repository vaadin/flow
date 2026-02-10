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

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.WritableSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link UI#localeSignal()}.
 */
class UILocaleSignalTest extends SignalsUnitTest {

    @Test
    public void localeSignal_initialValue_matchesGetLocale() {
        UI ui = UI.getCurrent();
        WritableSignal<Locale> signal = ui.localeSignal();

        assertNotNull(signal, "localeSignal() should never return null");
        assertEquals(ui.getLocale(), signal.value(),
                "Signal value should match getLocale()");
    }

    @Test
    public void localeSignal_setLocale_signalUpdated() {
        UI ui = UI.getCurrent();
        WritableSignal<Locale> signal = ui.localeSignal();

        Locale initialLocale = ui.getLocale();
        Locale newLocale = Locale.FRENCH;

        // Ensure we're actually changing the locale
        if (initialLocale.equals(newLocale)) {
            newLocale = Locale.GERMAN;
        }

        ui.setLocale(newLocale);

        assertEquals(newLocale, signal.value(),
                "Signal should reflect the new locale after setLocale()");
        assertEquals(newLocale, ui.getLocale(),
                "getLocale() should also return the new locale");
    }

    @Test
    public void localeSignal_writeToSignal_updatesGetLocale() {
        UI ui = UI.getCurrent();
        WritableSignal<Locale> signal = ui.localeSignal();

        Locale initialLocale = ui.getLocale();
        Locale newLocale = Locale.FRENCH;

        // Ensure we're actually changing the locale
        if (initialLocale.equals(newLocale)) {
            newLocale = Locale.GERMAN;
        }

        signal.value(newLocale);

        assertEquals(newLocale, ui.getLocale(),
                "getLocale() should reflect the new locale after "
                        + "writing to signal");
        assertEquals(newLocale, signal.value(),
                "Signal should have the new value");
    }

    @Test
    public void localeSignal_sameInstance_returnedOnMultipleCalls() {
        UI ui = UI.getCurrent();

        WritableSignal<Locale> signal1 = ui.localeSignal();
        WritableSignal<Locale> signal2 = ui.localeSignal();

        assertSame(signal1, signal2,
                "localeSignal() should return the same instance on "
                        + "multiple calls");
    }

    @Test
    public void localeSignal_multipleLocaleChanges_signalFollows() {
        UI ui = UI.getCurrent();
        WritableSignal<Locale> signal = ui.localeSignal();

        ui.setLocale(Locale.FRENCH);
        assertEquals(Locale.FRENCH, signal.value());

        ui.setLocale(Locale.GERMAN);
        assertEquals(Locale.GERMAN, signal.value());

        ui.setLocale(Locale.JAPANESE);
        assertEquals(Locale.JAPANESE, signal.value());
    }

    @Test
    public void localeSignal_multipleSignalWrites_getLocaleFollows() {
        UI ui = UI.getCurrent();
        WritableSignal<Locale> signal = ui.localeSignal();

        signal.value(Locale.FRENCH);
        assertEquals(Locale.FRENCH, ui.getLocale());

        signal.value(Locale.GERMAN);
        assertEquals(Locale.GERMAN, ui.getLocale());

        signal.value(Locale.JAPANESE);
        assertEquals(Locale.JAPANESE, ui.getLocale());
    }
}
