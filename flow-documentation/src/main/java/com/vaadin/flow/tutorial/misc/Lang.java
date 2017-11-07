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
package com.vaadin.flow.tutorial.misc;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.UI;
import com.vaadin.ui.i18n.I18NProvider;

@CodeFor("miscellaneous/tutorial-i18n-localization.asciidoc")
public class Lang implements I18NProvider {

    public static final String BUNDLE_PREFIX = "translate";

    public final Locale LOCALE_FI = new Locale("fi", "FI");
    public final Locale LOCALE_EN = new Locale("en", "GB");

    private List<Locale> locales = Collections
            .unmodifiableList(Arrays.asList(LOCALE_FI, LOCALE_EN));

    private static final LoadingCache<Locale, ResourceBundle> bundleCache = CacheBuilder
            .newBuilder().expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<Locale, ResourceBundle>() {

                @Override
                public ResourceBundle load(final Locale key) throws Exception {
                    return initializeBundle(key);
                }
            });

    @Override
    public List<Locale> getProvidedLocales() {
        return locales;
    }

    @Override
    public String getTranslation(String key, Object... params) {
        if (key == null) {
            Logger.getLogger(Lang.class.getName()).log(Level.WARNING,
                    "Got lang request for key with null value!");
            return "";
        }
        return getTranslation(key, getLocale(), params);
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) {
            Logger.getLogger(Lang.class.getName()).log(Level.WARNING,
                    "Got lang request for key with null value!");
            return "";
        }

        final ResourceBundle bundle = bundleCache.getUnchecked(locale);

        String value;
        try {
            value = bundle.getString(key);
        } catch (final MissingResourceException e) {
            Logger.getLogger(Lang.class.getName()).log(Level.WARNING,
                    "Missing resource", e);
            return "!" + locale.getLanguage() + ": " + key;
        }
        if (params.length > 0) {
            value = MessageFormat.format(value, params);
        }
        return value;
    }

    private static ResourceBundle initializeBundle(final Locale locale) {
        return readProperties(locale);
    }

    protected static ResourceBundle readProperties(final Locale locale) {
        final ClassLoader cl = Lang.class.getClassLoader();

        ResourceBundle propertiesBundle = null;
        try {
            propertiesBundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale,
                    cl);
        } catch (final MissingResourceException e) {
            Logger.getLogger(Lang.class.getName()).log(Level.WARNING,
                    "Missing resource", e);
        }
        return propertiesBundle;
    }

    private Locale getLocale() {
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
