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
package com.vaadin.flow.internal;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.vaadin.flow.server.VaadinRequest;

/**
 * Utility class for locale handling.
 *
 * @since 1.0
 */
public final class LocaleUtil {

    private LocaleUtil() {
    }

    /**
     * Get the exact locale match for the given request in the provided locales.
     * 
     * @param request
     *            request to get locale for
     * @param providedLocales
     *            application provided locales
     * @return found locale or null if no exact matches
     */
    public static Optional<Locale> getExactLocaleMatch(VaadinRequest request,
            List<Locale> providedLocales) {
        Locale foundLocale = null;
        Enumeration<Locale> locales = request.getLocales();
        while (locales.hasMoreElements()) {
            Locale locale = locales.nextElement();
            if (providedLocales.contains(locale)) {
                foundLocale = locale;
                break;
            }
        }
        return Optional.ofNullable(foundLocale);
    }

    /**
     * Get the locale matching the language of the request locale in the
     * provided locales.
     * 
     * @param request
     *            request to get locale for
     * @param providedLocales
     *            application provided locales
     * @return found locale or null if no matches by language
     */
    public static Optional<Locale> getLocaleMatchByLanguage(VaadinRequest request,
            List<Locale> providedLocales) {
        Locale foundLocale = null;
        Enumeration<Locale> locales = request.getLocales();
        while (locales.hasMoreElements()) {
            Locale locale = locales.nextElement();
            Optional<Locale> matching = providedLocales
                    .stream().filter(providedLocale -> providedLocale
                            .getLanguage().equals(locale.getLanguage()))
                    .findFirst();
            if (matching.isPresent()) {
                foundLocale = matching.get();
                break;
            }
        }
        return Optional.ofNullable(foundLocale);
    }
}
