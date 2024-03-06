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
import java.util.List;
import java.util.Locale;

/**
 * I18N provider interface for internationalization usage.
 *
 * @since 1.0
 */
public interface I18NProvider extends Serializable {

    /**
     * Get the locales that we have translations for. The first locale should be
     * the default locale.
     *
     * @return provided locales
     */
    List<Locale> getProvidedLocales();

    /**
     * Get the translation for key with given locale.
     * <p>
     * Note! For usability and catching missing translations implementation
     * should never return a null, but an exception string e.g. '!{key}!'
     *
     * @param key
     *            translation key
     * @param locale
     *            locale to use
     * @param params
     *            parameters used in translation string
     * @return translation for key if found
     */
    String getTranslation(String key, Locale locale, Object... params);

    /**
     * Get the translation for key with given locale.
     * <p>
     * Note! For usability and catching missing translations implementation
     * should never return a null, but an exception string e.g. '!{key}!'
     *
     * @param key
     *            translation key
     * @param locale
     *            locale to use
     * @param params
     *            parameters used in translation string
     * @return translation for key if found
     */
    default String getTranslation(Object key, Locale locale, Object... params) {
        return getTranslation(key.toString(), locale, params);
    }
}
