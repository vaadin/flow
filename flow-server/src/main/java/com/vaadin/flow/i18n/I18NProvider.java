/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.i18n;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.internal.LocaleUtil;

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

    /**
     * Get the translation for key via {@link I18NProvider} instance retrieved
     * from the current VaadinService. Uses the current UI locale, or if not
     * available, then the default locale.
     *
     * @param key
     *            translation key
     * @param params
     *            parameters used in translation string
     * @return translation for key if found
     * @throws IllegalStateException
     *             thrown if no I18NProvider found from the VaadinService
     */
    static String translate(String key, Object... params) {
        return translate(LocaleUtil.getLocale(), key, params);
    }

    /**
     * Get the translation for key with given locale via {@link I18NProvider}
     * instance retrieved from the current VaadinService.
     *
     * @param locale
     *            locale to use
     * @param key
     *            translation key
     * @param params
     *            parameters used in translation string
     * @return translation for key if found
     * @throws IllegalStateException
     *             thrown if no I18NProvider found from the VaadinService
     */
    static String translate(Locale locale, String key, Object... params) {
        return LocaleUtil.getI18NProvider()
                .orElseThrow(() -> new IllegalStateException(
                        "I18NProvider is not available via current VaadinService. VaadinService, Instantiator or I18NProvider is null."))
                .getTranslation(key, locale, params);
    }
}
