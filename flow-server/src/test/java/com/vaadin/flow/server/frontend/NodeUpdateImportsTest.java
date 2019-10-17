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
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner.FrontendDependenciesScannerFactory;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static org.junit.Assert.assertTrue;

public class NodeUpdateImportsTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private File importsFile;
    private File fallBackImportsFile;
    private File generatedPath;
    private File frontendDirectory;
    private File nodeModulesPath;
    private TaskUpdateImports updater;
    private File tmpRoot;
    private File tokenFile;

    private Logger logger = Mockito.mock(Logger.class);

    @Before
    public void setup() throws Exception {
        tmpRoot = temporaryFolder.getRoot();

        frontendDirectory = new File(tmpRoot, DEFAULT_FRONTEND_DIR);
        nodeModulesPath = new File(tmpRoot, NODE_MODULES);
        generatedPath = new File(tmpRoot, DEFAULT_GENERATED_DIR);
        importsFile = new File(generatedPath, IMPORTS_NAME);
        fallBackImportsFile = new File(generatedPath,
                FrontendUtils.FALLBACK_IMPORTS_NAME);
        File webpackDir = temporaryFolder.newFolder();
        tokenFile = new File(webpackDir, "config/flow-build-info.json");
        FileUtils.forceMkdirParent(tokenFile);
        tokenFile.createNewFile();

        assertTrue(nodeModulesPath.mkdirs());
        createExpectedImports(frontendDirectory, nodeModulesPath);
        assertTrue(new File(nodeModulesPath,
                FLOW_NPM_PACKAGE_NAME + "ExampleConnector.js").exists());

        new File(frontendDirectory, "extra-javascript.js").createNewFile();
        new File(frontendDirectory, "extra-css.css").createNewFile();
    }

    @Test
    public void extraComponentsInCP_componentsAreNotDiscoveredByMainScannerWrittenByFallback_fallbackIsGenerated()
            throws IOException {
        Stream<Class<?>> classes = Stream.concat(
                Stream.of(NodeTestComponents.class.getDeclaredClasses()),
                Stream.of(ExtraNodeTestComponents.class.getDeclaredClasses()));
        ClassFinder classFinder = new DefaultClassFinder(
                new URLClassLoader(getClassPath()),
                classes.toArray(Class<?>[]::new));

        JsonObject fallBackData = Json.createObject();

        updater = new TaskUpdateImports(classFinder,
                new FrontendDependenciesScannerFactory().createScanner(false,
                        classFinder, true),
                finder -> new FrontendDependenciesScannerFactory()
                        .createScanner(true, finder, true),
                tmpRoot, generatedPath, frontendDirectory, tokenFile,
                fallBackData) {
            @Override
            Logger log() {
                return logger;
            }
        };

        updater.execute();

        assertTrue(importsFile.exists());

        String mainContent = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());

        // ============== check main generated imports file ============
        // Contains theme lines
        Assert.assertThat(mainContent, CoreMatchers
                .containsString("const div = document.createElement('div');"));

        Assert.assertThat(mainContent, CoreMatchers.containsString(
                "div.innerHTML = '<custom-style><style include=\"lumo-color lumo-typography\"></style></custom-style>';"));

        // Contains CSS import lines
        Assert.assertThat(mainContent, CoreMatchers.containsString(
                "import $css_0 from '@vaadin/vaadin-mixed-component/bar.css';"));
        Assert.assertThat(mainContent, CoreMatchers.containsString(
                "addCssBlock(`<custom-style><style>${$css_0}</style></custom-style>`);"));

        // Contains theme imports
        Assert.assertThat(mainContent, CoreMatchers.containsString(
                "import '@vaadin/vaadin-lumo-styles/color.js';"));
        Assert.assertThat(mainContent, CoreMatchers.containsString(
                "import '@vaadin/vaadin-lumo-styles/typography.js';"));

        // Contains JS module imports
        Assert.assertThat(mainContent, CoreMatchers
                .containsString("import '@polymer/iron-icon/iron-icon.js';"));
        Assert.assertThat(mainContent,
                CoreMatchers.containsString("import '3rdparty/component.js';"));

        // Contains Javascript imports
        Assert.assertThat(mainContent,
                CoreMatchers.containsString("import 'javascript/a.js';"));
        Assert.assertThat(mainContent,
                CoreMatchers.containsString("import 'javascript/b.js';"));

        // fallback chunk load function is generated
        Assert.assertThat(mainContent, CoreMatchers.containsString(
                "fallbacks[thisScript.getAttribute('data-app-id')].loadFallback = function loadFallback(){"));

        Assert.assertThat(mainContent, CoreMatchers.containsString(
                "return import('./generated-flow-imports-fallback.js');"));

        assertTrue(fallBackImportsFile.exists());

        // ============== check fallback generated imports file ============

        String fallBackContent = FileUtils.readFileToString(fallBackImportsFile,
                Charset.defaultCharset());

        // Does not Contains theme lines
        Assert.assertThat(fallBackContent, CoreMatchers.not(CoreMatchers
                .containsString("const div = document.createElement('div');")));

        // Does not contains theme imports
        Assert.assertThat(fallBackContent,
                CoreMatchers.not(CoreMatchers.containsString(
                        "import '@vaadin/vaadin-lumo-styles/color.js';")));

        // Does not contains CSS import lines
        Assert.assertThat(fallBackContent,
                CoreMatchers.not(CoreMatchers.containsString(
                        "import $css_0 from '@vaadin/vaadin-mixed-component/bar.css';")));

        // Contains CSS import lines from CP not discovered by byte scanner
        Assert.assertThat(fallBackContent, CoreMatchers.containsString(
                "import $css_0 from 'Frontend/extra-css.css';"));
        Assert.assertThat(fallBackContent, CoreMatchers.containsString(
                "addCssBlock(`<dom-module id=\"flow_css_mod_0\" theme-for=\"extra-foo\"><template><style include=\"extra-bar\">${$css_0}</style></template></dom-module>`);"));

        // Does not contains JS module imports
        Assert.assertThat(fallBackContent, CoreMatchers.not(CoreMatchers
                .containsString("import '@polymer/iron-icon/iron-icon.js';")));

        // Contains JS module imports
        Assert.assertThat(fallBackContent,
                CoreMatchers.containsString("import '@polymer/a.js';"));

        // Does not contain Javascript imports
        Assert.assertThat(fallBackContent, CoreMatchers
                .not(CoreMatchers.containsString("import 'javascript/a.js';")));

        // Contains Javascript imports
        Assert.assertThat(fallBackContent, CoreMatchers
                .containsString("import 'Frontend/extra-javascript.js';"));

        // ============== check token file with fallback chunk data ============

        String tokenContent = FileUtils.readFileToString(tokenFile,
                Charset.defaultCharset());
        JsonObject object = Json.parse(tokenContent);

        assertTokenFileWithFallBack(object);
        assertTokenFileWithFallBack(fallBackData);
    }

    @Test
    public void emptyByteCodeScannerData_themeIsDiscovered_fallbackIsGenerated()
            throws IOException {
        ClassFinder classFinder = new DefaultClassFinder(
                new URLClassLoader(getClassPath()),
                EmptyByteScannerDataTestComponents.class.getDeclaredClasses());

        updater = new TaskUpdateImports(classFinder,
                new FrontendDependenciesScannerFactory().createScanner(false,
                        classFinder, true),
                finder -> new FrontendDependenciesScannerFactory()
                        .createScanner(true, finder, true),
                tmpRoot, generatedPath, frontendDirectory, tokenFile, null) {
            @Override
            Logger log() {
                return logger;
            }
        };

        updater.execute();

        assertTrue(importsFile.exists());

        String mainContent = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());

        // ============== check main generated imports file ============

        // Contains theme lines
        Assert.assertThat(mainContent, CoreMatchers
                .containsString("const div = document.createElement('div');"));

        Assert.assertThat(mainContent, CoreMatchers.containsString(
                "div.innerHTML = '<custom-style>foo</custom-style>';"));

        // fallback chunk load function is generated
        Assert.assertThat(mainContent, CoreMatchers.containsString(
                "fallbacks[thisScript.getAttribute('data-app-id')].loadFallback = function loadFallback(){"));

        Assert.assertThat(mainContent, CoreMatchers.containsString(
                "return import('./generated-flow-imports-fallback.js');"));

        // ============== check fallback generated imports file ============

        String fallBackContent = FileUtils.readFileToString(fallBackImportsFile,
                Charset.defaultCharset());

        // Does not Contains theme lines
        Assert.assertThat(fallBackContent, CoreMatchers.not(CoreMatchers
                .containsString("const div = document.createElement('div');")));

        // Contains CSS import lines from CP not discovered by byte scanner
        Assert.assertThat(fallBackContent, CoreMatchers
                .containsString("import $css_0 from 'Frontend/foo.css';"));
        Assert.assertThat(fallBackContent, CoreMatchers.containsString(
                "addCssBlock(`<dom-module id=\"baz\"><template><style include=\"bar\">${$css_0}</style></template></dom-module>`);"));

        // Contains JS module imports
        Assert.assertThat(fallBackContent, CoreMatchers.containsString(
                "import '@vaadin/vaadin-lumo-styles/icons.js';"));
        Assert.assertThat(fallBackContent, CoreMatchers
                .containsString("import 'Frontend/common-js-file.js';"));

        // Contains Javascript imports
        Assert.assertThat(fallBackContent, CoreMatchers.containsString(
                "import '@vaadin/flow-frontend/ExampleConnector.js';"));
    }

    @Test
    public void noFallBackScanner_fallbackIsNotGenerated() throws IOException {
        Stream<Class<?>> classes = Stream.concat(
                Stream.of(NodeTestComponents.class.getDeclaredClasses()),
                Stream.of(ExtraNodeTestComponents.class.getDeclaredClasses()));
        ClassFinder classFinder = new DefaultClassFinder(
                new URLClassLoader(getClassPath()),
                classes.toArray(Class<?>[]::new));

        updater = new TaskUpdateImports(classFinder,
                new FrontendDependenciesScannerFactory().createScanner(false,
                        classFinder, true),
                finder -> null, tmpRoot, generatedPath, frontendDirectory,
                tokenFile, null) {
            @Override
            Logger log() {
                return logger;
            }
        };

        updater.execute();

        assertTrue(importsFile.exists());

        String mainContent = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());

        // fallback chunk load function is not generated
        Assert.assertThat(mainContent,
                CoreMatchers.not(CoreMatchers.containsString(
                        "window.Vaadin.Flow.loadFallback = function loadFallback(){")));

        Assert.assertFalse(fallBackImportsFile.exists());

    }

    @Test
    public void noFallBackScanner_fallbackIsNotImportedEvenIfTheFileExists()
            throws Exception {
        Stream<Class<?>> classes = Stream.concat(
                Stream.of(NodeTestComponents.class.getDeclaredClasses()),
                Stream.of(ExtraNodeTestComponents.class.getDeclaredClasses()));
        ClassFinder classFinder = new DefaultClassFinder(
                new URLClassLoader(getClassPath()),
                classes.toArray(Class<?>[]::new));

        // create fallback imports file:
        // it is present after generated but the user is now running
        // everything without fallback. The file should not be included into
        // the imports
        fallBackImportsFile.mkdirs();
        fallBackImportsFile.createNewFile();
        Assert.assertTrue(fallBackImportsFile.exists());

        updater = new TaskUpdateImports(classFinder,
                new FrontendDependenciesScannerFactory().createScanner(false,
                        classFinder, true),
                finder -> null, tmpRoot, generatedPath, frontendDirectory,
                tokenFile, null) {
            @Override
            Logger log() {
                return logger;
            }
        };

        updater.execute();

        assertTrue(importsFile.exists());

        String mainContent = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());

        // fallback file is not imported in generated-flow-imports
        Assert.assertThat(mainContent, CoreMatchers.not(CoreMatchers
                .containsString(FrontendUtils.FALLBACK_IMPORTS_NAME)));
    }

    private void assertTokenFileWithFallBack(JsonObject object)
            throws IOException {
        JsonObject fallback = object.getObject("chunks").getObject("fallback");

        JsonArray modules = fallback.getArray("jsModules");
        Set<String> modulesSet = new HashSet<>();
        for (int i = 0; i < modules.length(); i++) {
            modulesSet.add(modules.getString(i));
        }
        Assert.assertTrue(modulesSet.contains("@polymer/a.js"));
        Assert.assertTrue(modulesSet.contains("./extra-javascript.js"));

        JsonArray css = fallback.getArray("cssImports");
        Assert.assertEquals(1, css.length());
        JsonObject cssImport = css.get(0);
        Assert.assertEquals("extra-bar", cssImport.getString("include"));
        Assert.assertEquals("extra-foo", cssImport.getString("themeFor"));
        Assert.assertEquals("./extra-css.css", cssImport.getString("value"));
    }

}
