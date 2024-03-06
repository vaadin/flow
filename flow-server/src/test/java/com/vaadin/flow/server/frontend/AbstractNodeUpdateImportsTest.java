/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
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
    private TaskUpdateImports updater;

    private MockLogger logger;

    @Before
    public void setup() throws Exception {
        File tmpRoot = temporaryFolder.getRoot();

        logger = new MockLogger();

        frontendDirectory = new File(tmpRoot, DEFAULT_FRONTEND_DIR);
        nodeModulesPath = new File(tmpRoot, NODE_MODULES);
        generatedPath = new File(tmpRoot,
                Paths.get(TARGET, DEFAULT_GENERATED_DIR).toString());
        importsFile = new File(generatedPath, IMPORTS_NAME);

        ClassFinder classFinder = getClassFinder();
        updater = new TaskUpdateImports(classFinder, getScanner(classFinder),
                finder -> null, tmpRoot, generatedPath, frontendDirectory, null,
                null, false, TARGET, true, false,
                Mockito.mock(FeatureFlags.class)) {
            @Override
            Logger log() {
                return logger;
            }
        };

        assertTrue(nodeModulesPath.mkdirs());
        createExpectedImports(frontendDirectory, nodeModulesPath);
        assertTrue(
                new File(
                        new File(
                                new File(frontendDirectory,
                                        FrontendUtils.GENERATED),
                                FrontendUtils.JAR_RESOURCES_FOLDER),
                        "ExampleConnector.js").exists());
    }

    protected abstract FrontendDependenciesScanner getScanner(
            ClassFinder finder);

    @Test
    public void generateImportsFile_fileContainsThemeLinesAndExpectedImportsAndCssImportLinesAndLogReports()
            throws Exception {
        List<String> expectedLines = new ArrayList<>(Arrays.asList(
                "export const addCssBlock = function(block, before = false) {",
                " const tpl = document.createElement('template');",
                " tpl.innerHTML = block;",
                " document.head[before ? 'insertBefore' : 'appendChild'](tpl.content, document.head.firstChild);",
                "};",
                "addCssBlock('<custom-style><style include=\"lumo-color lumo-typography\"></style></custom-style>', true);",
                "document.documentElement.setAttribute('theme', 'dark');"));
        expectedLines.addAll(getExpectedImports());

        // An import without `.js` extension
        expectedLines.add(
                "import '@vaadin/vaadin-mixed-component/theme/lumo/vaadin-something-else'");
        // An import not found in node_modules
        expectedLines.add("import 'unresolved/component';");

        expectedLines.add(
                "import $cssFromFile_0 from '@vaadin/vaadin-mixed-component/bar.css?inline'");
        expectedLines
                .add("import $cssFromFile_1 from 'Frontend/foo.css?inline';");
        expectedLines
                .add("import $cssFromFile_2 from 'Frontend/foo.css?inline';");
        expectedLines
                .add("import $cssFromFile_3 from 'Frontend/foo.css?inline';");
        expectedLines
                .add("import $cssFromFile_4 from 'Frontend/foo.css?inline';");
        expectedLines
                .add("import $cssFromFile_5 from 'Frontend/foo.css?inline';");
        expectedLines
                .add("import $cssFromFile_6 from 'Frontend/foo.css?inline';");
        expectedLines.add(
                "import { css, unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';");
        expectedLines.add("addCssBlock(`<style>${$css_0}</style>`);");
        expectedLines.add("addCssBlock(`<style>${$css_1}</style>`);");
        expectedLines.add(
                "addCssBlock(`<style include=\"bar\">${$css_2}</style>`);");
        expectedLines.add("registerStyles('', $css_3, {moduleId: 'baz'});");
        expectedLines.add(
                "registerStyles('', $css_4, {include: 'bar', moduleId: 'baz'});");
        expectedLines.add(
                "registerStyles('foo-bar', $css_5, {moduleId: 'flow_css_mod_5'});");
        expectedLines.add(
                "registerStyles('foo-bar', $css_6, {include: 'bar', moduleId: 'flow_css_mod_6'});");

        assertFalse(importsFile.exists());

        updater.execute();
        assertTrue(importsFile.exists());

        assertContainsImports(true, expectedLines.toArray(new String[0]));

        String output = logger.getLogs();
        assertContains(output, true,
                "Use the './' prefix for files in JAR files: 'ExampleConnector.js'",
                "Use the './' prefix for files in the '"
                        + frontendDirectory.getPath()
                        + "' folder: 'vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js'");

        // Using regex match because of the ➜ character in TC
        assertContains(output, true,
                "Failed to find the following imports in the `node_modules` tree:\n      - unresolved/component");
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
