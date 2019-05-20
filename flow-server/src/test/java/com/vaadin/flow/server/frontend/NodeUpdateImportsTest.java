/*
 * Copyright 2000-2019 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.*;

public class NodeUpdateImportsTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File importsFile;
    private File generatedPath;
    private File frontendDirectory;
    private File nodeModulesPath;
    private TaskUpdateImports updater;

    @Before
    public void setup() throws Exception {

        File tmpRoot = temporaryFolder.getRoot();

        frontendDirectory = new File(tmpRoot, DEFAULT_FRONTEND_DIR);
        nodeModulesPath = new File(tmpRoot, NODE_MODULES);
        generatedPath = new File(tmpRoot, DEFAULT_GENERATED_DIR);
        importsFile = new File(generatedPath, IMPORTS_NAME);

        updater = new TaskUpdateImports(getClassFinder(), null,
                tmpRoot, generatedPath, frontendDirectory);

        Assert.assertTrue(nodeModulesPath.mkdirs());
        createExpectedImports(frontendDirectory, nodeModulesPath);
    }

    @Test
    public void should_ThrowException_WhenImportsDoNotExist() {
        deleteExpectedImports(frontendDirectory, nodeModulesPath);

        boolean exceptionNotThrown = true;
        try {
            updater.execute();
        } catch (IllegalStateException expected) {
            exceptionNotThrown = false;
            String exceptionMessage = expected.getMessage();
            Assert.assertTrue(
                    exceptionMessage.contains(importsFile.getAbsolutePath()));

            String content = null;
            try {
                content = FileUtils.readFileToString(importsFile,
                        Charset.defaultCharset());
            } catch (IOException e) {
            }

            String innerMessage = expected.getCause().getMessage();
            Assert.assertTrue(
                    innerMessage + " is missing "
                            + nodeModulesPath.getAbsolutePath()
                            + "\n While imports file is " + content + "\n",
                    innerMessage.contains(nodeModulesPath.getAbsolutePath()));

            List<String> expectedImports = new ArrayList<>(
                    getExpectedImports());
            expectedImports.remove("@vaadin/flow-frontend/ExampleConnector.js");

            for (String expectedImport : expectedImports) {
                String normalizedImport = expectedImport.startsWith("./")
                        ? expectedImport.substring(2)
                        : expectedImport;
                Assert.assertTrue(
                        innerMessage + " is missing " + expectedImport
                                + "\n While imports file is " + content + "\n",
                        innerMessage.contains(normalizedImport));
            }
        }

        if (exceptionNotThrown) {
            Assert.fail(
                    "Expected an exception to be thrown when no imported files exist");
        }
    }

    @Test
    public void should_UpdateMainJsFile() throws Exception {
        Assert.assertFalse(importsFile.exists());

        List<String> expectedLines = new ArrayList<>(Arrays.asList(
                "const div = document.createElement('div');",
                "div.innerHTML = '<custom-style><style include=\"lumo-color lumo-typography\"></style></custom-style>';",
                "document.head.insertBefore(div.firstElementChild, document.head.firstChild);",
                "document.body.setAttribute('theme', 'dark');"));
        expectedLines.addAll(getExpectedImports());

        updater.execute();

        assertContainsImports(true, expectedLines.toArray(new String[0]));

        File flowPackage = new File(nodeModulesPath, FLOW_NPM_PACKAGE_NAME);

        Assert.assertTrue(flowPackage.exists());
        Assert.assertTrue(new File(flowPackage, "ExampleConnector.js")
                .exists());
    }

    @Test
    public void shouldNot_UpdateJsFile_when_NoChanges() throws Exception {
        updater.execute();
        long timestamp1 = importsFile.lastModified();

        // need to sleep because timestamp is in seconds
        sleep(1000);
        updater.execute();
        long timestamp2 = importsFile.lastModified();

        Assert.assertEquals(timestamp1, timestamp2);
    }

    @Test
    public void should_ContainLumoThemeFiles() throws Exception {
        updater.execute();

        assertContainsImports(true,
                "@vaadin/vaadin-lumo-styles/color.js",
                "@vaadin/vaadin-lumo-styles/typography.js",
                "@vaadin/vaadin-lumo-styles/sizing.js",
                "@vaadin/vaadin-lumo-styles/spacing.js",
                "@vaadin/vaadin-lumo-styles/style.js",
                "@vaadin/vaadin-lumo-styles/icons.js");
    }

    @Test
    public void should_AddImports() throws Exception {
        updater.execute();
        removeImports("@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
        assertContainsImports(false, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");

        updater.execute();
        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
    }

    @Test
    public void should_removeImports() throws Exception {
        updater.execute();
        addImports("./added-import.js");
        assertContainsImports(true, "./added-import.js");

        updater.execute();
        assertContainsImports(false, "./added-import.js");
    }

    @Test
    public void should_AddRemove_Imports() throws Exception {
        updater.execute();

        removeImports("@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
        addImports("./added-import.js");

        assertContainsImports(false, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
        assertContainsImports(true, "./added-import.js");

        updater.execute();

        assertContainsImports(false, "./added-import.js");
    }

    private void assertContainsImports(boolean contains, String... imports)
            throws IOException {
        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());
        for (String importString : imports) {
                if (contains) {
                    Assert.assertTrue(
                        importString + " not found in:\n" + content,
                        content.contains(addWebpackPrefix(importString)));
                } else {
                    Assert.assertFalse(
                        importString + " not found in:\n" + content,
                        content.contains(addWebpackPrefix(importString)));
                }
            }

    }

    private String addWebpackPrefix(String s) {
        if (s.startsWith("./")) {
            return WEBPACK_PREFIX_ALIAS + s.substring(2);
        }
        return s;
    }

    private void removeImports(String... imports) throws IOException {
        List<String> importsList = Arrays.asList(imports);

        List<String> current = FileUtils.readLines(importsFile,
                Charset.defaultCharset());

        Set<String> removed = current.stream()
                .filter(line -> importsList.stream().map(this::addWebpackPrefix).anyMatch(line::contains))
                .collect(Collectors.toSet());

        current.removeAll(removed);

        String content = String.join("\n", current);

        replaceJsFile(content + "\n");
    }

    private void addImports(String... imports) throws IOException {
        String content = Arrays.stream(imports).map(this::addWebpackPrefix).map(s -> "import '" + s + "';")
                .collect(Collectors.joining("\n"));

        replaceJsFile(content + "\n", StandardOpenOption.APPEND);
    }

    private void replaceJsFile(String content, OpenOption... options)
            throws IOException {
        Files.write(Paths.get(importsFile.toURI()),
                content.getBytes(StandardCharsets.UTF_8), options);
    }
}
