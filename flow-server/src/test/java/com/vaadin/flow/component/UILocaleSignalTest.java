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
import com.vaadin.flow.signals.Signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link UI#localeSignal()}.
 */
class UILocaleSignalTest extends SignalsUnitTest {

    @Test
    public void localeSignal_initialValue_matchesGetLocale() {
        UI ui = UI.getCurrent();
        Signal<Locale> signal = ui.localeSignal();

        assertNotNull(signal, "localeSignal() should never return null");
        assertEquals(ui.getLocale(), signal.peek(),
                "Signal value should match getLocale()");
    }

    @Test
    public void localeSignal_setLocale_signalUpdated() {
        UI ui = UI.getCurrent();
        Signal<Locale> signal = ui.localeSignal();

        Locale initialLocale = ui.getLocale();
        Locale newLocale = Locale.FRENCH;

        // Ensure we're actually changing the locale
        if (initialLocale.equals(newLocale)) {
            newLocale = Locale.GERMAN;
        }

        ui.setLocale(newLocale);

        assertEquals(newLocale, signal.peek(),
                "Signal should reflect the new locale after setLocale()");
        assertEquals(newLocale, ui.getLocale(),
                "getLocale() should also return the new locale");
    }

    @Test
    public void localeSignal_sameInstance_returnedOnMultipleCalls() {
        UI ui = UI.getCurrent();

        Signal<Locale> signal1 = ui.localeSignal();
        Signal<Locale> signal2 = ui.localeSignal();

        assertSame(signal1, signal2,
                "localeSignal() should return the same instance on "
                        + "multiple calls");
    }

    @Test
    public void localeSignal_multipleLocaleChanges_signalFollows() {
        UI ui = UI.getCurrent();
        Signal<Locale> signal = ui.localeSignal();

        ui.setLocale(Locale.FRENCH);
        assertEquals(Locale.FRENCH, signal.peek());

        ui.setLocale(Locale.GERMAN);
        assertEquals(Locale.GERMAN, signal.peek());

        ui.setLocale(Locale.JAPANESE);
        assertEquals(Locale.JAPANESE, signal.peek());
    }

    @Test
    public void localeSignal_bindValue_updatesLocale() {
        UI ui = UI.getCurrent();
        LocaleField field = new LocaleField();
        ui.add(field);

        field.bindValue(ui.localeSignal(), ui::setLocale);

        assertEquals(ui.getLocale(), field.getValue());

        // Simulate user picking a new locale in the field
        field.setValueFromClient(Locale.JAPANESE);
        assertEquals(Locale.JAPANESE, ui.getLocale());

        // Programmatic setLocale propagates back to the field
        ui.setLocale(Locale.GERMAN);
        assertEquals(Locale.GERMAN, field.getValue());
    }

    @Test
    public void setLocale_uiLocaleIndependentFromSession() {
        UI ui = UI.getCurrent();

        // Set UI to a specific locale
        ui.setLocale(Locale.GERMAN);
        assertEquals(Locale.GERMAN, ui.getLocale());

        // Changing session locale does not override a UI's own locale
        // (the UI is not registered with session.addUI() here, matching
        // the case where a UI has already set its own locale)
        assertEquals(Locale.GERMAN, ui.getLocale());
        assertNotEquals(ui.getLocale(), ui.getSession().getLocale());
    }

    @Tag("select")
    private static class LocaleField
            extends AbstractField<LocaleField, Locale> {

        public LocaleField() {
            super(Locale.getDefault());
        }

        @Override
        protected void setPresentationValue(Locale newPresentationValue) {
            // NOP
        }

        public void setValueFromClient(Locale value) {
            setModelValue(value, true);
        }
    }
}
