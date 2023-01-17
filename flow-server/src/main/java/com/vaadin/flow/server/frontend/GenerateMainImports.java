/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import static com.vaadin.flow.server.frontend.FrontendUtils.FALLBACK_IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;

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
    private final ClassFinder finder;
    private List<String> lines;
    private FrontendDependenciesScanner frontendDepScanner;
    private JsonObject statsJson;

    public GenerateMainImports(ClassFinder classFinder,
            FrontendDependenciesScanner frontendDepScanner, Options options,
            JsonObject statsJson) {
        super(options);
        finder = classFinder;
        this.frontendDepScanner = frontendDepScanner;
        this.statsJson = statsJson;
    }

    public List<String> getLines() {
        if (lines == null) {
            return Collections.emptyList();
        }
        return lines;
    }

    @Override
    protected void writeImportLines(List<String> lines) {
        // NO-OP. Only store the lines to write
        this.lines = lines;
    }

    @Override
    protected List<String> getModules() {
        return frontendDepScanner.getModules();
    }

    @Override
    protected Set<String> getScripts() {
        return frontendDepScanner.getScripts();
    }

    @Override
    protected URL getResource(String name) {
        return finder.getResource(name);
    }

    @Override
    protected Collection<String> getGeneratedModules() {
        if (options.getGeneratedFolder() == null) {
            return Collections.emptySet();
        }
        // Exclude generated-flow-imports.js and
        // generated-flow-imports-fallback.js
        // as they are not generated modules, but import files.
        return NodeUpdater
                .getGeneratedModules(options.getGeneratedFolder(), Stream
                        .of(new File(options.getGeneratedFolder(), IMPORTS_NAME)
                                .getName(),
                                new File(options.getGeneratedFolder(),
                                        FALLBACK_IMPORTS_NAME).getName())
                        .collect(Collectors.toSet()));
    }

    @Override
    protected ThemeDefinition getThemeDefinition() {
        return frontendDepScanner.getThemeDefinition();
    }

    @Override
    protected AbstractTheme getTheme() {
        return frontendDepScanner.getTheme();
    }

    @Override
    protected Set<CssData> getCss() {
        return frontendDepScanner.getCss();
    }

    @Override
    protected Collection<String> getThemeLines() {
        return Collections.emptyList();
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
        JsonArray statsBundle = statsJson.hasKey("bundleImports")
                ? statsJson.getArray("bundleImports")
                : Json.createArray();
        importName = importName.replace("Frontend/", "./");

        for (int i = 0; i < statsBundle.length(); i++) {
            if (importName.equals(
                    statsBundle.getString(i).replace("Frontend/", "./"))) {
                return true;
            }
        }
        return false;
    }
}
