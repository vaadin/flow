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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractNodeUpdateImportsTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private File importsFile;
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
        importsFile = FrontendUtils.getFlowGeneratedImports(frontendDirectory);

        ClassFinder classFinder = getClassFinder();
        Options options = new MockOptions(classFinder, tmpRoot)
                .withFrontendDirectory(frontendDirectory)
                .withBuildDirectory(TARGET).withProductionMode(true)
                .withBundleBuild(true);
        updater = new TaskUpdateImports(getScanner(classFinder), options) {
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
        List<String> expectedLines = new ArrayList<>();
        expectedLines.addAll(getExpectedImports());

        // An import without `.js` extension
        expectedLines.add(
                "import '@vaadin/vaadin-mixed-component/src/vaadin-something-else'");
        // An import not found in node_modules
        expectedLines.add("import 'unresolved/component';");

        expectedLines.add(
                "import \\$cssFromFile_\\d from '@vaadin/vaadin-mixed-component/bar.css\\?inline'");
        expectedLines.add(
                "import \\$cssFromFile_\\d from 'Frontend/foo.css\\?inline';");
        expectedLines.add(
                "import \\$cssFromFile_\\d from 'Frontend/foo.css\\?inline';");
        expectedLines.add(
                "import \\$cssFromFile_\\d from 'Frontend/foo.css\\?inline';");
        expectedLines.add(
                "import \\$cssFromFile_\\d from 'Frontend/foo.css\\?inline';");
        expectedLines.add(
                "import \\$cssFromFile_\\d from 'Frontend/foo.css\\?inline';");
        expectedLines.add(
                "import \\$cssFromFile_\\d from 'Frontend/foo.css\\?inline';");
        expectedLines.add(
                "import \\$cssFromFile_\\d from 'Frontend/foo.css\\?inline';");
        expectedLines.add(
                "import \\{ css, unsafeCSS, registerStyles \\} from '@vaadin/vaadin-themable-mixin';");
        expectedLines.add(
                "injectGlobalCss\\(\\$cssFromFile_\\d.toString\\(\\), 'CSSImport end', document\\);");
        expectedLines.add(
                "injectGlobalCss\\(\\$cssFromFile_\\d.toString\\(\\), 'CSSImport end', document\\);");
        expectedLines.add("function addCssBlock\\(block\\) \\{");
        expectedLines.add(
                "addCssBlock\\(`<style include=\"bar\">\\$\\{\\$css_\\d\\}</style>`\\);");
        expectedLines.add(
                "registerStyles\\('', \\$css_\\d, \\{moduleId: 'baz'\\}\\);");
        expectedLines.add(
                "registerStyles\\('', \\$css_\\d, \\{include: 'bar', moduleId: 'baz'\\}\\);");
        expectedLines.add(
                "registerStyles\\('foo-bar', \\$css_\\d, \\{moduleId: 'flow_css_mod_\\d'\\}\\);");
        expectedLines.add(
                "registerStyles\\('foo-bar', \\$css_\\d, \\{include: 'bar', moduleId: 'flow_css_mod_\\d'\\}\\);");

        assertFalse(importsFile.exists());

        updater.execute();
        assertTrue(importsFile.exists());

        assertContainsImports(true, expectedLines.toArray(new String[0]));

        String output = logger.getLogs();
        assertContains(output, true,
                "Use the './' prefix for files in JAR files: 'ExampleConnector.js'",
                "Use the './' prefix for files in the '"
                        + frontendDirectory.getPath().replace("\\", "\\\\")
                        + "' folder: 'vaadin-mixed-component/src/vaadin-mixed-component.js'");

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

    private void assertContainsImports(boolean contains, String... imports)
            throws IOException {
        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());

        for (String line : imports) {
            assertContains(content, contains, addFrontendAlias(line));
        }
    }

    private void assertContains(String content, boolean contains,
            String... checks) {
        for (String importString : checks) {
            boolean result = Pattern.compile(importString).matcher(content)
                    .find();
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
            String prefixed = addFrontendAlias(line);
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
                        .map(this::addFrontendAlias).anyMatch(line::contains))
                .collect(Collectors.toSet());

        current.removeAll(removed);

        String content = String.join("\n", current);

        replaceJsFile(content + "\n");
    }

    private void addImports(String... imports) throws IOException {
        String content = Arrays.stream(imports).map(this::addFrontendAlias)
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
