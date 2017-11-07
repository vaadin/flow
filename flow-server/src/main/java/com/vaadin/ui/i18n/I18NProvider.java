/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.i18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.vaadin.ui.UI;

/**
 * I18N provider interface for internationalization usage.
 */
public interface I18NProvider {

    /**
     * Get the locales that we have translations for. The first locale should be
     * the default locale.
     * 
     * @return provided locales
     */
    default List<Locale> getProvidedLocales() {
        return Arrays.asList(Locale.getDefault());
    }

    /**
     * Get the translation for current {@link com.vaadin.ui.UI} locale.
     * <p>
     * Note! For usability and catching missing translations implementation
     * should never return a null, but an exception string e.g. '!{key}!'
     *
     * @param key
     *            translation key
     * @param params
     *            parameters used in translation string
     * @return translation for key if found (implementation should not return
     *         null)
     */
    String getTranslation(String key, Object... params);

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
     * Get the current locale to use.
     * <p>
     * If UI.getCurrent returns null then we use the same scheme to determine
     * locale as we use on startup when no locale matches.
     * 
     * @return current locale
     */
    default Locale getLocale() {
        UI currentUi = UI.getCurrent();
        Locale locale = currentUi == null ? null : currentUi.getLocale();
        if (locale == null) {
            List<Locale> locales = getProvidedLocales();
            if (locales != null && !locales.isEmpty()) {
                locale = locales.get(0);
            } else {
                locale = Locale.getDefault();
            }
        }
        return locale;
    }
}
