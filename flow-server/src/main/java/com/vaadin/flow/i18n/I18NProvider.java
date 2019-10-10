/*
 * Copyright 2000-2018 Vaadin Ltd.
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
}
