/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.i18n;

import java.util.EventObject;
import java.util.Locale;

import com.vaadin.flow.component.UI;

/**
 * Event object with data related to locale change.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class LocaleChangeEvent extends EventObject {

    private final Locale locale;

    /**
     * Locale change event constructor.
     *
     * @param ui
     *            The ui on which the Event initially occurred.
     * @param locale
     *            new locale that was set
     */
    public LocaleChangeEvent(UI ui, Locale locale) {
        super(ui);
        this.locale = locale;
    }

    /**
     * Get the new locale that was set.
     *
     * @return set locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the UI where the locale changed in.
     *
     * @return the ui
     */
    public UI getUI() {
        return (UI) getSource();
    }
}
