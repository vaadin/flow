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

package com.vaadin.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;

import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;

import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

/**
 * A class that is responsible for retrieving the application's polymer.json
 * contents and extracting the data from it.
 *
 * @author Vaadin Ltd.
 *
 * @see <a href=
 *      "https://www.polymer-project.org/2.0/docs/tools/polymer-json">Documentation
 *      on polymer.json</a>
 */
class PolymerJsonReader {
    private static final String POLYMER_JSON_CONTEXT_PATH = "/polymer.json";
    private static final String INIT_PAGE_JSON_PROPERTY_NAME = "entrypoint";
    private static final String BOOTSTRAP_PAGE_JSON_PROPERTY_NAME = "shell";
    private static final String URL_PATH_SEPARATOR = "/";

    private final List<Dependency> bootstrapDependencies = new ArrayList<>(2);

    /**
     * Creates a reader class that looks for
     * {@link PolymerJsonReader#POLYMER_JSON_CONTEXT_PATH} in
     * {@link ServletContext} and parses it.
     *
     * @param fileCharset
     *            expected polymer.json charset
     * @param servletContext
     *            the servlet context to look for polymer.json file in
     */
    PolymerJsonReader(String fileCharset, ServletContext servletContext,
            String rootUrlPart) {
        Charset potentialPolymerJsonCharset = Optional.ofNullable(fileCharset)
                .map(Charset::forName).orElse(StandardCharsets.UTF_8);

        Optional<JsonObject> polymerJsonContents = getPolymerJsonContents(
                servletContext, potentialPolymerJsonCharset);
        if (polymerJsonContents.isPresent()) {
            addDependenciesIfPresent(polymerJsonContents.get(), rootUrlPart);
        } else {
            Logger.getLogger(getClass().getName()).info(() -> String.format(
                    "Was not able to locate the '%s' file in servlet context, considering ",
                    POLYMER_JSON_CONTEXT_PATH));
        }
    }

    private void addDependenciesIfPresent(JsonObject jsonObject,
            String rootUrlPart) {
        Stream.of(BOOTSTRAP_PAGE_JSON_PROPERTY_NAME,
                INIT_PAGE_JSON_PROPERTY_NAME).map(jsonObject::<JsonValue> get)
                .filter(Objects::nonNull).map(JsonValue::asString)
                .map(relativeUrlPart -> relativeUrlToAbsolute(rootUrlPart,
                        relativeUrlPart))
                .map(url -> new Dependency(Dependency.Type.HTML_IMPORT, url,
                        LoadMode.EAGER))
                .forEach(bootstrapDependencies::add);
    }

    private String relativeUrlToAbsolute(String rootUrlPart,
            String relativeUrlPart) {
        String urlStart = rootUrlPart.endsWith(URL_PATH_SEPARATOR)
                ? rootUrlPart.substring(0, rootUrlPart.length() - 1)
                : rootUrlPart;
        String urlEnd = relativeUrlPart.startsWith(URL_PATH_SEPARATOR)
                ? relativeUrlPart.substring(1)
                : relativeUrlPart;
        return urlStart + URL_PATH_SEPARATOR + urlEnd;
    }

    private Optional<JsonObject> getPolymerJsonContents(
            ServletContext servletContext, Charset charset) {
        try (InputStream inputStream = servletContext
                .getResourceAsStream(POLYMER_JSON_CONTEXT_PATH)) {
            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, charset))) {
                    return Optional.of(JsonUtil.parse(
                            reader.lines().collect(Collectors.joining(""))));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    String.format("Failed to read polymer.json located at '%s'",
                            POLYMER_JSON_CONTEXT_PATH),
                    e);
        }
        return Optional.empty();
    }

    /**
     * Gets the dependencies that are mentioned in polymer.json file and should
     * be loaded first. Returns an empty list, if no file or no dependencies are
     * found in file.
     *
     * @return bootstrap dependencies
     */
    Collection<Dependency> getBootstrapDependencies() {
        return Collections.unmodifiableList(bootstrapDependencies);
    }
}
