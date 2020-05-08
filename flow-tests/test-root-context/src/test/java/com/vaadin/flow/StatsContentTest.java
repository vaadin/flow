/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEAULT_FLOW_RESOURCES_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.FALLBACK_IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;

public class StatsContentTest {

    private File generatedFrontend;

    @Before
    public void init() {

        final File baseDir = new File(System.getProperty("user.dir", "."));
        generatedFrontend = new File(baseDir, DEFAULT_GENERATED_DIR);
    }

    @Test
    public void generatedStats_containsFlowImportsTemplates()
            throws IOException {
        final File flowImports = new File(generatedFrontend, IMPORTS_NAME);
        final String flowImportsString = IOUtils
                .toString(flowImports.toURI(), StandardCharsets.UTF_8);

        final String statsJson = loadStatsJson();

        for (String importString : collectImports(flowImportsString)) {
            assertImportInStats(statsJson, importString);
        }
    }

    @Test
    public void generatedStats_containsFlowFallbackImportsTemplates()
            throws IOException {
        final File flowImports = new File(generatedFrontend,
                FALLBACK_IMPORTS_NAME);
        final String flowImportsString = IOUtils
                .toString(flowImports.toURI(), StandardCharsets.UTF_8);

        final String statsJson = loadStatsJson();

        for (String importString : collectImports(flowImportsString)) {
            assertImportInStats(statsJson, importString);
        }
    }

    private String loadStatsJson() throws IOException {
        final InputStream statsStream = getClass().getClassLoader()
                .getResourceAsStream("META-INF/VAADIN/config/stats.json");
        Assert.assertNotNull("stats.json not generated", statsStream);
        return IOUtils.toString(statsStream, StandardCharsets.UTF_8);
    }

    /**
     * Collect import statements and and clean import strings to match stats
     * path.
     *
     * @param flowImportsString
     *         <@code String> to collect imports from
     * @return list of cleaned imports from <@code String>
     */
    private List<String> collectImports(String flowImportsString) {
        return Arrays.stream(flowImportsString.split("\n"))
                .filter(in -> in.startsWith("import"))
                .map(in -> in.replace("import '", "")
                        .replace(FLOW_NPM_PACKAGE_NAME, String.format("../%s/",
                                DEAULT_FLOW_RESOURCES_FOLDER))
                        .replace("//", "/").replace("Frontend/", "./")
                        .replace("';", "")).collect(Collectors.toList());
    }

    private void assertImportInStats(String statsJson, String importString) {
        // If import starts with @ e.g. '@vaadin' or '@polymer' it may be in
        // a path like '"../node_modules/.pnpm/registry.npmjs.org/@vaadin/vaadin-development-mode-detector/1.1.0/node_modules/@vaadin/vaadin-development-mode-detector/vaadin-development-mode-detector.js"'
        if (importString.startsWith("@")) {
            String testId = String
                    .format("\"name\": \"(.*)%s(.*)\"", importString);
            Pattern regex = Pattern.compile(testId);
            Assert.assertTrue(String.format("Couldn't find %s", testId),
                    regex.matcher(statsJson).find());
        } else {
            String testId = String.format("\"name\": \"%s\"", importString);
            Assert.assertTrue(String.format("Couldn't find %s", testId),
                    statsJson.contains(testId));
        }
    }
}
