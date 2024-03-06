/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.i18n;

import java.io.Serializable;

/**
 * Any {@code com.vaadin.ui.Component} implementing this interface will be
 * informed when the UI locale is changed and on attach.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface LocaleChangeObserver extends Serializable {

    /**
     * Notifies when the UI locale is changed.
     *
     * @param event
     *            locale change event with event details
     */
    void localeChange(LocaleChangeEvent event);
}
