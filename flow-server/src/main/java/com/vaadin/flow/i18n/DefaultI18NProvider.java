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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default i18n provider that will be initialized if custom {@link I18NProvider}
 * is not available.
 */
public class DefaultI18NProvider implements I18NProvider {

    final List<Locale> providedLocales;
    private final ClassLoader classLoader;

    public static final String BUNDLE_FOLDER = "vaadin-i18n";
    public static final String BUNDLE_FILENAME = "translations";
    // Get bundles named `translations` from `vaadin-i18n` folder.
    public static final String BUNDLE_PREFIX = BUNDLE_FOLDER + "."
            + BUNDLE_FILENAME;

    /**
     * Construct {@link DefaultI18NProvider} for a list of locales that we have
     * translations for.
     *
     * @param providedLocales
     *            List of locales. The first locale should be the default
     *            locale.
     */
    public DefaultI18NProvider(List<Locale> providedLocales) {
        this(providedLocales, DefaultI18NProvider.class.getClassLoader());
    }

    /**
     * Construct {@link DefaultI18NProvider} for a list of locales that we have
     * translations for. Enables giving a specific classloader if needed.
     *
     * @param providedLocales
     *            List of locales. The first locale should be the default
     *            locale.
     * @param classLoader
     *            ClassLoader to use for loading translation bundles.
     */
    public DefaultI18NProvider(List<Locale> providedLocales,
            ClassLoader classLoader) {
        this.providedLocales = Collections.unmodifiableList(providedLocales);
        this.classLoader = classLoader;
    }

    @Override
    public List<Locale> getProvidedLocales() {
        return providedLocales;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) {
            getLogger().warn("Got lang request for key with null value!");
            return "";
        }

        final ResourceBundle bundle = getBundle(locale);
        if (bundle == null) {
            return key;
        }

        String value;
        try {
            value = bundle.getString(key);
        } catch (final MissingResourceException e) {
            getLogger().debug("Missing resource for key " + key, e);
            return "!" + locale.getLanguage() + ": " + key;
        }
        if (params.length > 0) {
            value = new MessageFormat(value, locale).format(params);
        }
        return value;
    }

    @Override
    public Map<String, String> getAllTranslations(Locale locale) {
        var bundle = getBundle(locale);
        return bundle == null ? Map.of()
                : getTranslations(bundle.keySet(), locale);
    }

    ResourceBundle getBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle(BUNDLE_PREFIX, locale, classLoader);
        } catch (final MissingResourceException e) {
            getLogger().warn("Missing resource bundle for " + BUNDLE_PREFIX
                    + " and locale " + locale.getDisplayName(), e);
        }
        return null;
    }

    static Logger getLogger() {
        return LoggerFactory.getLogger(DefaultI18NProvider.class);
    }

}
