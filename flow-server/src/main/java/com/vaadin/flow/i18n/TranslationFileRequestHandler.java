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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vaadin.flow.i18n.DefaultI18NProvider.BUNDLE_FOLDER;

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

    static final String CHUNK_PARAMETER_NAME = "chunks";

    static final String KEYS_PARAMETER_NAME = "keys";

    static final String RETRIEVED_LOCALE_HEADER_NAME = "X-Vaadin-Retrieved-Locale";

    static final String CHUNK_RESOURCE = BUNDLE_FOLDER + "/i18n.json";

    private final I18NProvider i18NProvider;

    private final ClassLoader classLoader;

    private Map<String, String[]> chunkData;

    public TranslationFileRequestHandler(I18NProvider i18NProvider,
            ClassLoader classLoader) {
        this.i18NProvider = i18NProvider;
        this.classLoader = classLoader;
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        if (i18NProvider == null) {
            handleMissingI18NProvider(session, response);
            return true;
        }
        var locale = getLocale(request);
        var chunks = request.getParameterMap().get(CHUNK_PARAMETER_NAME);
        var keys = request.getParameterMap().get(KEYS_PARAMETER_NAME);
        var translations = collectTranslations(chunks, keys, locale);
        if (translations.isEmpty()) {
            handleNotFound(response);
        } else {
            handleFound(locale, response, translations);
        }
        return true;
    }

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return HandlerHelper.isRequestType(request,
                HandlerHelper.RequestType.TRANSLATION_FILE);
    }

    private void handleFound(Locale locale, VaadinResponse response,
            ObjectNode translations) throws IOException {
        response.setStatus(HttpStatusCode.OK.getCode());
        response.setHeader(RETRIEVED_LOCALE_HEADER_NAME,
                locale.toLanguageTag());
        response.setHeader("Content-Type", JsonConstants.JSON_CONTENT_TYPE);
        response.getWriter().write(translations.toString());
    }

    private void handleNotFound(VaadinResponse response) {
        response.setStatus(HttpStatusCode.NOT_FOUND.getCode());
    }

    private void handleMissingI18NProvider(VaadinSession session,
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

    private ObjectNode collectTranslations(String[] chunks, String[] keys,
            Locale locale) {
        var json = JacksonUtils.createObjectNode();

        var chunkStream = Optional.ofNullable(chunks).map(chunkNames -> {
            var chunkData = getChunkData();
            // for each chunk name, retrieve the keys from the chunk data
            return Arrays.stream(chunks).map(chunkData::get)
                    .filter(Objects::nonNull).flatMap(Arrays::stream);
        });

        // add single keys if requested
        var keyStream = Optional.ofNullable(keys).map(Arrays::stream);

        var requestedKeys = Stream.of(chunkStream, keyStream)
                .filter(Optional::isPresent).flatMap(Optional::orElseThrow)
                .collect(Collectors.toSet());
        var translations = requestedKeys.isEmpty()
                ? i18NProvider.getAllTranslations(locale)
                : i18NProvider.getTranslations(requestedKeys, locale);
        translations.forEach(json::put);
        return json;
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

    /**
     * Retrieves the chunk data from the JSON file.
     *
     * @return a map containing chunk names and their corresponding keys
     */
    private Map<String, String[]> getChunkData() {
        if (chunkData == null) {
            chunkData = new HashMap<>();
            var chunkResource = classLoader.getResource(CHUNK_RESOURCE);

            if (chunkResource != null) {
                try {
                    var json = JacksonUtils.getMapper().readTree(chunkResource);
                    var chunksNode = json.get("chunks");

                    if (chunksNode != null && chunksNode.isObject()) {
                        var fieldNames = chunksNode.fieldNames();

                        while (fieldNames.hasNext()) {
                            var chunkName = fieldNames.next();
                            var keysNode = chunksNode.get(chunkName)
                                    .get("keys");

                            if (keysNode != null && keysNode.isArray()) {
                                var keys = new String[keysNode.size()];

                                for (int i = 0; i < keysNode.size(); i++) {
                                    keys[i] = keysNode.get(i).asText();
                                }

                                chunkData.put(chunkName, keys);
                            }
                        }
                    }
                } catch (IOException e) {
                    getLogger().error("Error while reading the resource "
                            + CHUNK_RESOURCE, e);
                }
            }
        }

        return chunkData;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(TranslationFileRequestHandler.class);
    }
}
