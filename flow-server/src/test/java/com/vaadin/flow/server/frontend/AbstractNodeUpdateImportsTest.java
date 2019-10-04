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
import java.lang.reflect.Method;
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.impl.SimpleLogger;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractNodeUpdateImportsTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private File importsFile;
    private File generatedPath;
    private File frontendDirectory;
    private File nodeModulesPath;
    private File loggerFile;
    private TaskUpdateImports updater;

    private boolean useMockLog;

    private Logger logger = Mockito.mock(Logger.class);

    @Before
    public void setup() throws Exception {
        File tmpRoot = temporaryFolder.getRoot();

        // Use a file for logs so as tests can assert the warnings shown to the
        // user.
        loggerFile = new File(tmpRoot, "test.log");
        loggerFile.createNewFile();
        // Setting a system property we make SimpleLogger to output to a file
        System.setProperty(SimpleLogger.LOG_FILE_KEY,
                loggerFile.getAbsolutePath());
        // re-init logger to get new configuration
        initLogger();

        frontendDirectory = new File(tmpRoot, DEFAULT_FRONTEND_DIR);
        nodeModulesPath = new File(tmpRoot, NODE_MODULES);
        generatedPath = new File(tmpRoot, DEFAULT_GENERATED_DIR);
        importsFile = new File(generatedPath, IMPORTS_NAME);

        ClassFinder classFinder = getClassFinder();
        updater = new TaskUpdateImports(classFinder, getScanner(classFinder),
                finder -> null, tmpRoot, generatedPath, frontendDirectory, null,
                null) {
            @Override
            Logger log() {
                if (useMockLog) {
                    return logger;
                } else {
                    return super.log();
                }
            }
        };

        assertTrue(nodeModulesPath.mkdirs());
        createExpectedImports(frontendDirectory, nodeModulesPath);
        assertTrue(new File(nodeModulesPath,
                FLOW_NPM_PACKAGE_NAME + "ExampleConnector.js").exists());
    }

    @After
    public void tearDown() throws Exception {
        // re-init logger to reset to default
        System.clearProperty(SimpleLogger.LOG_FILE_KEY);
        initLogger();
    }

    protected abstract FrontendDependenciesScanner getScanner(
            ClassFinder finder);

    private void initLogger() throws Exception, SecurityException {
        // init method is protected
        Method method = SimpleLogger.class.getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(null);
    }

    @Test
    public void generateImportsFile_fileContainsThemeLinesAndExpectedImportsAndCssImportLinesAndLogReports()
            throws Exception {
        List<String> expectedLines = new ArrayList<>(Arrays.asList(
                "const div = document.createElement('div');",
                "div.innerHTML = '<custom-style><style include=\"lumo-color lumo-typography\"></style></custom-style>';",
                "document.head.insertBefore(div.firstElementChild, document.head.firstChild);",
                "document.body.setAttribute('theme', 'dark');"));
        expectedLines.addAll(getExpectedImports());

        // An import without `.js` extension
        expectedLines.add(
                "import '@vaadin/vaadin-mixed-component/theme/lumo/vaadin-something-else'");
        // An import not found in node_modules
        expectedLines.add("import 'unresolved/component';");

        expectedLines.add(
                "import $css_0 from '@vaadin/vaadin-mixed-component/bar.css'");
        expectedLines.add("import $css_1 from 'Frontend/foo.css';");
        expectedLines.add("import $css_2 from 'Frontend/foo.css';");
        expectedLines.add("import $css_3 from 'Frontend/foo.css';");
        expectedLines.add("import $css_4 from 'Frontend/foo.css';");
        expectedLines.add("import $css_5 from 'Frontend/foo.css';");
        expectedLines.add("import $css_6 from 'Frontend/foo.css';");
        expectedLines.add(
                "addCssBlock(`<custom-style><style>${$css_0}</style></custom-style>`);");
        expectedLines.add(
                "addCssBlock(`<custom-style><style>${$css_1}</style></custom-style>`);");
        expectedLines.add(
                "addCssBlock(`<custom-style><style include=\"bar\">${$css_2}</style></custom-style>`);");
        expectedLines.add(
                "addCssBlock(`<dom-module id=\"baz\"><template><style>${$css_3}</style></template></dom-module>`);");
        expectedLines.add(
                "addCssBlock(`<dom-module id=\"baz\"><template><style include=\"bar\">${$css_4}</style></template></dom-module>`);");
        expectedLines.add(
                "addCssBlock(`<dom-module id=\"flow_css_mod_5\" theme-for=\"foo-bar\"><template><style>${$css_5}</style></template></dom-module>`);");
        expectedLines.add(
                "addCssBlock(`<dom-module id=\"flow_css_mod_6\" theme-for=\"foo-bar\"><template><style include=\"bar\">${$css_6}</style></template></dom-module>`);");

        assertFalse(importsFile.exists());

        updater.execute();
        assertTrue(importsFile.exists());

        assertContainsImports(true, expectedLines.toArray(new String[0]));

        assertTrue(loggerFile.exists());

        String output = FileUtils
                .readFileToString(loggerFile, StandardCharsets.UTF_8)
                // fix for windows
                .replace("\r", "");
        assertContains(output, true,
                "changing 'frontend://frontend-p3-template.js' to './frontend-p3-template.js'",
                "Use the './' prefix for files in JAR files: 'ExampleConnector.js'",
                "Use the './' prefix for files in the '"
                        + frontendDirectory.getPath()
                        + "' folder: 'vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js'");

        // Using regex match because of the ➜ character in TC
        assertContains(output, true,
                "Failed to find the following imports in the `node_modules` tree:\n      - unresolved/component");

        assertContains(output, false,
                "changing 'frontend://foo-dir/javascript-lib.js' to './foo-dir/javascript-lib.js'");
    }

    @Test
    public void noChanges_generatedJsFileIsNotUpdated() throws Exception {
        updater.execute();
        long timestamp1 = importsFile.lastModified();

        // need to sleep because timestamp is in seconds
        sleep(1000);
        updater.execute();
        long timestamp2 = importsFile.lastModified();

        Assert.assertEquals(timestamp1, timestamp2);
    }

    @Test
    public void removeJsModuleImportFromFile_importIsReadedAfterRegeneration()
            throws Exception {
        updater.execute();
        removeImports("@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");
        assertContainsImports(false, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");

        updater.execute();
        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");
    }

    @Test
    public void addModuleImportManuallyIntoGeneratedFile_importIsRemovedAfterRegeneration()
            throws Exception {
        updater.execute();
        addImports("./added-import.js");
        assertContainsImports(true, "./added-import.js");

        updater.execute();
        assertContainsImports(false, "./added-import.js");
    }

    @Test
    public void addAndRemoveJsModuleImports_addedImportIsNotPreseredAfterRegeneration()
            throws Exception {
        updater.execute();

        removeImports("@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");
        addImports("./added-import.js");

        assertContainsImports(false, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-template.js");
        assertContainsImports(true, "./added-import.js");

        updater.execute();

        assertContainsImports(false, "./added-import.js");
    }

    @Test
    public void addJsModules_themeModulesAreOnTop() throws Exception {
        updater.execute();

        addImports("styles/styles.js");

        assertImportOrder("@vaadin/vaadin-lumo-styles/color.js",
                "Frontend/foo.js");
        assertImportOrder("@vaadin/vaadin-lumo-styles/color.js",
                "styles/styles.js");
    }

    private void assertContainsImports(boolean contains, String... imports)
            throws IOException {
        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());

        for (String line : imports) {
            assertContains(content, contains, addWebpackPrefix(line));
        }
    }

    private void assertContains(String content, boolean contains,
            String... checks) {
        for (String importString : checks) {
            boolean result = content.contains(importString);
            String message = "\n  " + (contains ? "NOT " : "") + "FOUND '"
                    + importString + " IN: \n" + content;
            if (contains) {
                assertTrue(message, result);
            } else {
                assertFalse(message, result);
            }
        }
    }

    private void assertImportOrder(String... imports) throws IOException {
        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());
        int curIndex = -1;
        for (String line : imports) {
            String prefixed = addWebpackPrefix(line);
            int nextIndex = content.indexOf(prefixed);
            assertTrue("import '" + prefixed + "' not found", nextIndex != -1);
            assertTrue("import '" + prefixed + "' appears in the wrong order",
                    curIndex <= nextIndex);
            curIndex = nextIndex;
        }
    }

    private void removeImports(String... imports) throws IOException {
        List<String> importsList = Arrays.asList(imports);

        List<String> current = FileUtils.readLines(importsFile,
                Charset.defaultCharset());

        Set<String> removed = current
                .stream().filter(line -> importsList.stream()
                        .map(this::addWebpackPrefix).anyMatch(line::contains))
                .collect(Collectors.toSet());

        current.removeAll(removed);

        String content = String.join("\n", current);

        replaceJsFile(content + "\n");
    }

    private void addImports(String... imports) throws IOException {
        String content = Arrays.stream(imports).map(this::addWebpackPrefix)
                .map(s -> "import '" + s + "';")
                .collect(Collectors.joining("\n"));

        replaceJsFile(content + "\n", StandardOpenOption.APPEND);
    }

    private void replaceJsFile(String content, OpenOption... options)
            throws IOException {
        Files.write(Paths.get(importsFile.toURI()),
                content.getBytes(StandardCharsets.UTF_8), options);
    }
}
