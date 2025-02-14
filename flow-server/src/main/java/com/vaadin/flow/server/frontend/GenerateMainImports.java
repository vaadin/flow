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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

/**
 * Collect generated-flow-imports content for project to use to determine if
 * dev-bundle contains all required imports.
 * <p>
 * Only used when checking if dev bundle need to be rebuild in dev mode without
 * a dev server.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class GenerateMainImports extends AbstractUpdateImports {
    private JsonNode statsJson;
    private Map<File, List<String>> output;

    public GenerateMainImports(FrontendDependenciesScanner frontendDepScanner,
            Options options, JsonNode statsJson) {
        super(options, frontendDepScanner);
        this.statsJson = statsJson;
    }

    public List<String> getLines() {
        if (output == null) {
            return Collections.emptyList();
        }
        return merge(output);
    }

    @Override
    protected void writeOutput(Map<File, List<String>> outputFiles) {
        this.output = outputFiles;
    }

    @Override
    protected boolean addCssLines(Collection<String> lines, CssData cssData,
            int i) {
        super.addCssLines(lines, cssData, i);
        // CSS files in 'generated/jar-resources' are not generated at this
        // moment, so not let the application interrupt and continue with
        // checking the dev-bundle
        return true;
    }

    @Override
    protected Logger getLogger() {
        // Do not log file not found etc. for the generator.
        return NOPLogger.NOP_LOGGER;
    }

    @Override
    protected boolean inMemoryCollection() {
        return true;
    }

    @Override
    protected String getImportsNotFoundMessage() {
        return "";
    }

    @Override
    protected boolean importedFileExists(String importName) {
        if (super.importedFileExists(importName)) {
            return true;
        }
        // Accept import name change if import is in bundle.
        // Basically it means theme file import path like:
        // "@vaadin/accordion/theme/lumo/accordion.js" instead of
        // "@vaadin/accordion/src/accordion.js"
        ArrayNode statsBundle = statsJson.has("bundleImports")
                ? (ArrayNode) statsJson.get("bundleImports")
                : JacksonUtils.createArrayNode();
        importName = importName.replace("Frontend/", "./");

        for (int i = 0; i < statsBundle.size(); i++) {
            if (importName.equals(statsBundle.get(i).textValue()
                    .replace("Frontend/", "./"))) {
                return true;
            }
        }
        return false;
    }
}
