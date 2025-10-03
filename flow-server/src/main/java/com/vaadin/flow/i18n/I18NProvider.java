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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.LocaleUtil;
import com.vaadin.flow.server.VaadinService;

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
     * Get the default locale. Per default this is the first locale returned by
     * {@link #getProvidedLocales}.
     *
     * @return default locale
     */
    default Locale getDefaultLocale() {
        List<Locale> providedLocales = getProvidedLocales();
        if (providedLocales.isEmpty()) {
            return null;
        }
        return providedLocales.get(0);
    }

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
     * Retrieves all available translations. This is currently used only by
     * Hilla in development mode.
     *
     * @param locale
     *            locale to use
     * @return a map of all available translations (the default implementation
     *         just returns an empty map)
     */
    default Map<String, String> getAllTranslations(Locale locale) {
        return Map.of();
    }

    /**
     * Retrieves the translations for a collection of keys. By default, it calls
     * `getTranslation` on each key, but this can be optimized by the
     * implementation.
     *
     * @param keys
     *            the keys to be translated
     * @param locale
     *            locale to use
     * @return a map of translations
     */
    default Map<String, String> getTranslations(Collection<String> keys,
            Locale locale) {
        return keys.stream().distinct().collect(Collectors
                .toMap(Function.identity(), k -> getTranslation(k, locale)));
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
     * <p>
     * If there is no {@link I18NProvider} available or no translation for the
     * {@code key} it returns an exception string e.g. '!{key}!'.
     *
     * @param locale
     *            locale to use
     * @param key
     *            translation key
     * @param params
     *            parameters used in translation string
     * @return translation for key if found
     */
    static String translate(Locale locale, String key, Object... params) {
        VaadinService vaadinService = VaadinService.getCurrent();
        if (vaadinService == null) {
            throw new IllegalStateException(
                    "I18NProvider is not available as VaadinService is null");
        }
        Instantiator instantiator = vaadinService.getInstantiator();
        if (instantiator == null) {
            throw new IllegalStateException(
                    "I18NProvider is not available as Instantiator is null");
        }

        return LocaleUtil.getI18NProvider()
                .map(i18n -> i18n.getTranslation(key, locale, params))
                .orElseGet(() -> "!{" + key + "}!");
    }
}
