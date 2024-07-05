/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.vaadin.flow.server.VaadinRequest;

/**
 * Utility class for locale handling.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
    public static Optional<Locale> getLocaleMatchByLanguage(
            VaadinRequest request, List<Locale> providedLocales) {
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
