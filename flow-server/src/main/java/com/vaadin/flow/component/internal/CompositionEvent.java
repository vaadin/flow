/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.internal;

import java.util.Locale;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Abstract class for composition events.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class CompositionEvent extends ComponentEvent<Component> {

    private final String data;
    private final Locale locale;

    /**
     * Creates a new composition event.
     *
     * @param source
     *            the component that fired the event
     * @param fromClient
     *            <code>true</code> if the event was originally fired on the
     *            client, <code>false</code> if the event originates from
     *            server-side logic
     * @param data
     *            the string being composed
     * @param locale
     *            language code for the composition event, if available;
     *            otherwise, the empty string
     */
    public CompositionEvent(Component source, boolean fromClient, String data,
            String locale) {
        super(source, fromClient);
        this.data = data;
        this.locale = locale == null ? null : Locale.forLanguageTag(locale);
    }

    /**
     * Creates a new server-side composition event with no additional
     * information.
     *
     * @param source
     *            the component that fired the event
     */
    public CompositionEvent(Component source) {
        this(source, false, "", "");
    }

    /**
     * Gets the string being composed.
     *
     * @return the string being composed
     */
    public String getData() {
        return data;
    }

    /**
     * Gets the optional {@link Locale} of the event.
     *
     * @return the optional {@link Locale} of the event
     */
    public Optional<Locale> getLocale() {
        return Optional.ofNullable(locale);
    }

}
