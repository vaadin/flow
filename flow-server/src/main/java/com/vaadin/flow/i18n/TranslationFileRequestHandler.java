/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import com.vaadin.flow.server.*;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class TranslationFileRequestHandler implements RequestHandler {

    static final String LANGUAGE_TAG_PARAMETER_NAME = "langtag";

    static final String RETRIEVED_LOCALE_HEADER_NAME = "Retrieved-Locale";

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        if (!HandlerHelper.isRequestType(request,
                HandlerHelper.RequestType.TRANSLATION_FILE)) {
            return false;
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

    private void handleFound(VaadinResponse response,
            ResourceBundle translationPropertyFile) throws IOException {
        response.setStatus(HttpStatusCode.OK.getCode());
        response.setHeader(RETRIEVED_LOCALE_HEADER_NAME,
                translationPropertyFile.getLocale().toLanguageTag());
        writeFileToResponse(response, translationPropertyFile);
    }

    private void handleNotFound(VaadinResponse response) {
        response.setStatus(HttpStatusCode.NOT_FOUND.getCode());
    }

    private void writeFileToResponse(VaadinResponse response,
            ResourceBundle translationPropertyFile) throws IOException {
        JsonObject json = Json.createObject();
        translationPropertyFile.keySet().forEach(
                key -> json.put(key, translationPropertyFile.getString(key)));
        response.getWriter().write(json.toJson());
    }

    private Locale getLocale(VaadinRequest request) {
        String languageTag = Objects.requireNonNullElse(
                request.getParameter(LANGUAGE_TAG_PARAMETER_NAME), "");
        return Locale.forLanguageTag(languageTag);
    }

    private ResourceBundle getTranslationPropertyFile(Locale locale) {
        try {
            return ResourceBundle.getBundle(DefaultI18NProvider.BUNDLE_PREFIX,
                    locale, I18NUtil.getClassLoader());
        } catch (MissingResourceException e) {
            getLogger().warn(
                    "Missing resource bundle for "
                            + DefaultI18NProvider.BUNDLE_PREFIX + " and locale "
                            + locale.getDisplayName()
                            + ". There is no default bundle to fall back on.",
                    e);
            return null;
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(TranslationFileRequestHandler.class);
    }
}
