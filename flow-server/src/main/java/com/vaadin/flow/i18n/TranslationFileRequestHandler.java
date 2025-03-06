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

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.JsonConstants;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Handles translation file requests. Translation file requests are internal
 * requests sent by the client-side to retrieve the translation file for the
 * specified language tag. The response contains the translations in JSON
 * format. Also, the language tag of the retrieved translation file is included
 * as a header with the name {@code X-Vaadin-Retrieved-Locale}. The language tag
 * parameter {@code langtag} supports both dash and underscore as separators.
 * <p>
 * The translation file to return is determined by matching the requested locale
 * to the available bundles with the following prioritization order:
 * <ul>
 * <li>Exact match</li>
 * <li>Language and country match</li>
 * <li>Language match</li>
 * <li>Default bundle (root bundle)</li>
 * </ul>
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 24.4
 */
public class TranslationFileRequestHandler extends SynchronizedRequestHandler {

    static final String LANGUAGE_TAG_PARAMETER_NAME = "langtag";

    static final String RETRIEVED_LOCALE_HEADER_NAME = "X-Vaadin-Retrieved-Locale";

    private static final Locale FALLBACK_LOCALE = Locale.ROOT;

    private final DefaultI18NProvider i18NProvider;

    private final boolean hasFallbackBundle;

    public TranslationFileRequestHandler(I18NProvider i18NProvider) {
        boolean hasDefaultI18NProvider = i18NProvider != null
                && DefaultI18NProvider.class.equals(i18NProvider.getClass());
        this.i18NProvider = hasDefaultI18NProvider
                ? (DefaultI18NProvider) i18NProvider
                : null;
        this.hasFallbackBundle = hasFallbackBundle();
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        if (i18NProvider == null) {
            handleCustomI18NProvider(session, response);
            return true;
        }
        Locale locale = getLocale(request);
        ResourceBundle translationPropertyFile = getTranslationPropertyFile(
                locale);
        if (translationPropertyFile == null) {
            handleNotFound(response);
        } else {
            handleFound(response, translationPropertyFile);
        }
        return true;
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return HandlerHelper.isRequestType(request,
                HandlerHelper.RequestType.TRANSLATION_FILE);
    }

    private void handleFound(VaadinResponse response,
            ResourceBundle translationPropertyFile) throws IOException {
        response.setStatus(HttpStatusCode.OK.getCode());
        response.setHeader(RETRIEVED_LOCALE_HEADER_NAME,
                translationPropertyFile.getLocale().toLanguageTag());
        response.setHeader("Content-Type", JsonConstants.JSON_CONTENT_TYPE);
        writeFileToResponse(response, translationPropertyFile);
    }

    private void handleNotFound(VaadinResponse response) {
        response.setStatus(HttpStatusCode.NOT_FOUND.getCode());
    }

    private void handleCustomI18NProvider(VaadinSession session,
            VaadinResponse response) throws IOException {
        String errorMessage = "Loading translations is not supported when using a custom i18n provider.";
        if (session.getService().getDeploymentConfiguration()
                .isProductionMode()) {
            response.setStatus(HttpStatusCode.NOT_FOUND.getCode());
        } else {
            response.sendError(HttpStatusCode.NOT_IMPLEMENTED.getCode(),
                    errorMessage);
        }
        getLogger().debug(errorMessage);
    }

    private void writeFileToResponse(VaadinResponse response,
            ResourceBundle translationPropertyFile) throws IOException {
        ObjectNode json = JacksonUtils.createObjectNode();
        translationPropertyFile.keySet().forEach(
                key -> json.put(key, translationPropertyFile.getString(key)));
        response.getWriter().write(json.toString());
    }

    private Locale getLocale(VaadinRequest request) {
        String languageTag = Objects.requireNonNullElse(
                request.getParameter(LANGUAGE_TAG_PARAMETER_NAME), "");
        if (languageTag.contains("_")) {
            String[] tokens = languageTag.split("_");
            String language = tokens[0];
            String country = tokens.length > 1 ? tokens[1] : "";
            String variant = tokens.length > 2 ? tokens[2] : "";
            return new Locale(language, country, variant);
        }
        return Locale.forLanguageTag(languageTag);
    }

    private ResourceBundle getTranslationPropertyFile(Locale locale) {
        Locale bestMatchLocale = getBestMatchLocale(locale);
        if (bestMatchLocale == null) {
            if (FALLBACK_LOCALE.equals(locale)) {
                getLogger().debug(
                        "Missing the requested default bundle for {}.",
                        DefaultI18NProvider.BUNDLE_PREFIX);
            } else {
                getLogger().debug(
                        "Missing resource bundles for {}, both the requested locale {} and the default bundle.",
                        DefaultI18NProvider.BUNDLE_PREFIX,
                        locale.getDisplayName());
            }
            return null;
        }
        if (!locale.equals(bestMatchLocale)) {
            if (FALLBACK_LOCALE.equals(bestMatchLocale)) {
                getLogger().debug(
                        "Missing resource bundle for {} and locale {}. Using the default bundle.",
                        DefaultI18NProvider.BUNDLE_PREFIX,
                        locale.getDisplayName());
            } else {
                getLogger().debug(
                        "Missing resource bundle for {} and locale {}. Using the best match locale {}.",
                        DefaultI18NProvider.BUNDLE_PREFIX,
                        locale.getDisplayName(),
                        bestMatchLocale.getDisplayName());
            }
        }
        return i18NProvider.getBundle(bestMatchLocale,
                ResourceBundle.Control.getNoFallbackControl(
                        ResourceBundle.Control.FORMAT_PROPERTIES));
    }

    private Locale getBestMatchLocale(Locale locale) {
        Set<Locale> providedLocales = Set
                .copyOf(i18NProvider.getProvidedLocales());
        if (providedLocales.contains(locale)) {
            return locale;
        }
        Optional<Locale> languageAndCountryMatch = providedLocales.stream()
                .filter(providedLocale -> providedLocale.getLanguage()
                        .equals(locale.getLanguage())
                        && providedLocale.getCountry()
                                .equals(locale.getCountry()))
                .findAny();
        if (languageAndCountryMatch.isPresent()) {
            return languageAndCountryMatch.get();
        }
        Optional<Locale> languageMatch = providedLocales.stream()
                .filter(providedLocale -> providedLocale.getLanguage()
                        .equals(locale.getLanguage()))
                .findAny();
        if (languageMatch.isPresent()) {
            return languageMatch.get();
        }
        if (hasFallbackBundle) {
            return FALLBACK_LOCALE;
        }
        return null;
    }

    private boolean hasFallbackBundle() {
        if (this.i18NProvider != null) {
            try {
                this.i18NProvider.getBundle(FALLBACK_LOCALE,
                        ResourceBundle.Control.getNoFallbackControl(
                                ResourceBundle.Control.FORMAT_PROPERTIES));
                return true;
            } catch (MissingResourceException e) {
                // NO-OP
            }
        }
        return false;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(TranslationFileRequestHandler.class);
    }
}
