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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.slf4j.Logger;
import org.slf4j.impl.SimpleLogger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.CssData;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractUpdateImportsTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private File tmpRoot;
    private File generatedPath;
    private File frontendDirectory;
    private File nodeModulesPath;
    private File loggerFile;
    private UpdateImports updater;

    private boolean useMockLog;

    private Logger logger = Mockito.mock(Logger.class);

    private class UpdateImports extends AbstractUpdateImports {

        private final ClassFinder finder;
        private final FrontendDependenciesScanner scanner;
        private List<String> resultingLines;

        UpdateImports(ClassFinder classFinder,
                FrontendDependenciesScanner scanner, File npmDirectory) {
            super(frontendDirectory, npmDirectory, generatedPath);
            this.scanner = scanner;
            finder = classFinder;
        }

        @Override
        protected void writeImportLines(List<String> lines) {
            resultingLines = lines;
        }

        @Override
        protected Collection<String> getThemeLines() {
            return Arrays.asList("theme-line-foo", "theme-line-bar");
        }

        @Override
        protected List<String> getModules() {
            return scanner.getModules();
        }

        @Override
        protected Set<String> getScripts() {
            return scanner.getScripts();
        }

        @Override
        protected URL getResource(String name) {
            return finder.getResource(name);
        }

        @Override
        protected Collection<String> getGeneratedModules() {
            return Arrays.asList("generated-modules-foo",
                    "generated-modules-bar");
        }

        @Override
        protected ThemeDefinition getThemeDefinition() {
            return scanner.getThemeDefinition();
        }

        @Override
        protected AbstractTheme getTheme() {
            return scanner.getTheme();
        }

        @Override
        protected Set<CssData> getCss() {
            return scanner.getCss();
        }

        @Override
        protected Logger getLogger() {
            if (useMockLog) {
                return logger;
            } else {
                NodeUpdater updater = Mockito.mock(NodeUpdater.class);
                Mockito.doCallRealMethod().when(updater).log();
                return updater.log();
            }
        }
    }

    @Before
    public void setup() throws Exception {
        tmpRoot = temporaryFolder.getRoot();

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

        ClassFinder classFinder = getClassFinder();
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                tmpRoot);
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
    public void importsFilesAreNotFound_throws() {
        deleteExpectedImports(frontendDirectory, nodeModulesPath);
        exception.expect(IllegalStateException.class);
        updater.run();
    }

    @Test
    public void getModuleLines_npmPackagesDontExist_logExplanation() {
        useMockLog = true;
        Mockito.when(logger.isInfoEnabled()).thenReturn(true);
        boolean atLeastOneRemoved = false;
        for (String imprt : getExpectedImports()) {
            if (imprt.startsWith("@vaadin") && imprt.endsWith(".js")) {
                assertTrue(resolveImportFile(nodeModulesPath, nodeModulesPath,
                        imprt).delete());
                atLeastOneRemoved = true;
            }
        }
        assertTrue(atLeastOneRemoved);
        updater.run();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).info(captor.capture());

        Assert.assertThat(captor.getValue(), CoreMatchers.allOf(
                CoreMatchers.containsString(
                        "@vaadin/vaadin-lumo-styles/spacing.js"),
                CoreMatchers.containsString(
                        "If the build fails, check that npm packages are installed."),
                CoreMatchers.containsString(
                        "To fix the build remove `node_modules` directory to reset modules."),
                CoreMatchers.containsString(
                        "In addition you may run `npm install` to fix `node_modules` tree structure.")));
    }

    @Test
    public void getModuleLines_oneFrontendDependencyDoesntExist_throwExceptionAndlogExplanation() {
        useMockLog = true;
        Mockito.when(logger.isInfoEnabled()).thenReturn(true);

        String fooFileName = "./foo.js";
        assertFileRemoved(fooFileName, frontendDirectory);

        try {
            updater.run();
            Assert.fail("Execute should have failed with missing file");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(),
                    CoreMatchers
                            .containsString(getFormattedFrontendErrorMessage(
                                    Sets.newSet(fooFileName))));
        }

    }

    @Test
    public void getModuleLines_multipleFrontendDependencyDoesntExist_throwExceptionAndlogExplanation() {
        useMockLog = true;
        Mockito.when(logger.isInfoEnabled()).thenReturn(true);

        String localTemplateFileName = "./local-template.js";
        String fooFileName = "./foo.js";

        assertFileRemoved(localTemplateFileName, frontendDirectory);
        assertFileRemoved(fooFileName, frontendDirectory);

        try {
            updater.run();
            Assert.fail("Execute should have failed with missing files");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), CoreMatchers
                    .containsString(getFormattedFrontendErrorMessage(
                            Sets.newSet(localTemplateFileName, fooFileName))));
        }

    }

    private void assertFileRemoved(String fileName, File directory) {
        assertTrue(String.format(
                "File `%s` was not removed from, or does not exist in, `%s`",
                fileName, directory),
                resolveImportFile(directory, directory, fileName).delete());
    }

    private String getFormattedFrontendErrorMessage(
            Set<String> resourcesNotFound) {
        String prefix = "Failed to find the following files: ";

        String suffix = String.format("%n  Locations searched were:"
                + "%n      - `%s` in this project"
                + "%n      - `%s` in included JARs"
                + "%n%n  Please, double check that those files exist. If you use a custom directory "
                + "for your resource files instead of default "
                + "`frontend` folder then make sure you it's correctly configured "
                + "(e.g. set '%s' property)", frontendDirectory.getPath(),
                Constants.RESOURCES_FRONTEND_DEFAULT,
                FrontendUtils.PARAM_FRONTEND_DIR);

        return String.format("%n%n  %s%n      - %s%n  %s%n%n", prefix,
                String.join("\n      - ", resourcesNotFound), suffix);
    }

    @Test
    public void generateLines_resultingLinesContainsThemeLinesAndExpectedImportsAndCssLinesAndGeneratedImportsAndLoggerReports()
            throws Exception {
        List<String> expectedLines = new ArrayList<>(
                Arrays.asList("theme-line-foo", "theme-line-bar"));
        getExpectedImports().stream().filter(imp -> imp.equals("/foo.css"))
                .forEach(imp -> expectedLines
                        .add("import '" + addWebpackPrefix(imp) + "';"));

        // An import without `.js` extension
        expectedLines.add(
                "import '@vaadin/vaadin-mixed-component/theme/lumo/vaadin-something-else';");
        // An import not found in node_modules
        expectedLines.add("import 'unresolved/component';");

        expectedLines.add(
                "import $css_0 from '@vaadin/vaadin-mixed-component/bar.css';");
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

        expectedLines.add("import 'generated-modules-foo';");
        expectedLines.add("import 'generated-modules-bar';");

        updater.run();

        for (String line : expectedLines) {
            Assert.assertTrue(
                    "\n" + line + " IS NOT FOUND IN: \n"
                            + updater.resultingLines,
                    updater.resultingLines.contains(line));
        }

        assertTrue(loggerFile.exists());

        String output = FileUtils
                .readFileToString(loggerFile, StandardCharsets.UTF_8)
                // fix for windows
                .replace("\r", "");

        Assert.assertThat(output, CoreMatchers.containsString(
                "changing 'frontend://frontend-p3-template.js' to './frontend-p3-template.js'"));
        Assert.assertThat(output, CoreMatchers.containsString(
                "Use the './' prefix for files in JAR files: 'ExampleConnector.js'"));
        Assert.assertThat(output, CoreMatchers
                .containsString("Use the './' prefix for files in the '"
                        + frontendDirectory.getPath()
                        + "' folder: 'vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js'"));

        // Using regex match because of the ➜ character in TC
        Assert.assertThat(output, CoreMatchers.containsString(
                "Failed to find the following imports in the `node_modules` tree:\n      - unresolved/component"));

        Assert.assertThat(output, CoreMatchers.not(CoreMatchers.containsString(
                "changing 'frontend://foo-dir/javascript-lib.js' to './foo-dir/javascript-lib.js'")));
    }

    @Test
    public void cssFileNotFound_throws() {
        assertTrue(resolveImportFile(frontendDirectory, nodeModulesPath,
                "@vaadin/vaadin-mixed-component/bar.css").delete());
        exception.expect(IllegalStateException.class);
        updater.run();
    }

    @Test
    public void generate_containsLumoThemeFiles() {
        updater.run();

        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/color.js",
                "@vaadin/vaadin-lumo-styles/typography.js",
                "@vaadin/vaadin-lumo-styles/sizing.js",
                "@vaadin/vaadin-lumo-styles/spacing.js",
                "@vaadin/vaadin-lumo-styles/style.js",
                "@vaadin/vaadin-lumo-styles/icons.js");
    }

    // flow #6408
    @Test
    public void jsModuleOnRouterLayout_shouldBe_addedAfterLumoStyles() {
        updater.run();

        assertContainsImports(true, "Frontend/common-js-file.js");

        assertImportOrder("@vaadin/vaadin-lumo-styles/color.js",
                "Frontend/common-js-file.js");
        assertImportOrder(
                "@vaadin/vaadin-mixed-component/theme/lumo/vaadin-something-else.js",
                "Frontend/common-js-file.js");
    }

    @Test
    public void jsModulesOrderIsPreservedAnsAfterJsModules() {
        updater.run();

        assertImportOrder("jsmodule/g.js", "javascript/a.js", "javascript/b.js",
                "javascript/c.js");
    }

    @Route(value = "")
    private static class MainView extends Component {
        NodeTestComponents.TranslatedImports translatedImports;
        NodeTestComponents.LocalP3Template localP3Template;
        NodeTestComponents.JavaScriptOrder javaScriptOrder;
    }

    @Test
    public void assertFullSortOrder() throws MalformedURLException {
        Class[] testClasses = { MainView.class, NodeTestComponents.TranslatedImports.class,
                NodeTestComponents.LocalP3Template.class,
                NodeTestComponents.JavaScriptOrder.class };
        ClassFinder classFinder = getClassFinder(testClasses);

        updater = new UpdateImports(classFinder, getScanner(classFinder),
                tmpRoot);
        updater.run();

        // Imports are collected as
        // - theme and css
        // - JsModules (external e.g. in node_modules/)
        // - JavaScript
        // - Generated webcompoents
        // - JsModules (internal e.g. in frontend/)
        List<String> expectedImports = new ArrayList<>();
        expectedImports.addAll(updater.getThemeLines());

        getAnntotationsAsStream(JsModule.class, testClasses).map(JsModule::value).map(this::updateToImport).sorted().forEach(expectedImports::add);
        getAnntotationsAsStream(JavaScript.class, testClasses).map(JavaScript::value).map(this::updateToImport).sorted().forEach(expectedImports::add);

        List<String> internals = expectedImports.stream().filter(importValue -> importValue.contains(FrontendUtils.WEBPACK_PREFIX_ALIAS)).sorted().collect(
                Collectors.toList());
        updater.getGeneratedModules().stream().map(this::updateToImport).forEach(expectedImports::add);
        // Remove internals from the full list
        expectedImports.removeAll(internals);
        // Add internals to end of list
        expectedImports.addAll(internals);

        Assert.assertEquals(expectedImports, updater.resultingLines);
    }

    private <T extends Annotation> Stream<T> getAnntotationsAsStream(Class<T> annotation, Class<?>... classes) {
        Stream<T> stream = Stream.empty();
        for(Class<?> clazz : classes) {
            stream = Stream.concat(stream, Stream.of(clazz.getAnnotationsByType(annotation)));
        }
        return stream;
    }

    private String updateToImport(String value) {
        if(value.startsWith("./")) {
            value = value.replace("./", FrontendUtils.WEBPACK_PREFIX_ALIAS);
        }
        return String.format("import '%s';", value);
    }

    private void assertContainsImports(boolean contains, String... imports) {
        for (String line : imports) {
            boolean result = updater.resultingLines
                    .contains("import '" + addWebpackPrefix(line) + "';");
            String message = "\n  " + (contains ? "NOT " : "") + "FOUND '"
                    + line + " IN: \n" + updater.resultingLines;
            if (contains) {
                assertTrue(message, result);
            } else {
                assertFalse(message, result);
            }
        }
    }

    private void assertImportOrder(String... imports) {
        int curIndex = -1;
        for (String line : imports) {
            String prefixed = addWebpackPrefix(line);
            int nextIndex = updater.resultingLines
                    .indexOf("import '" + prefixed + "';");
            assertTrue("import '" + prefixed + "' not found", nextIndex != -1);
            assertTrue("import '" + prefixed + "' appears in the wrong order",
                    curIndex <= nextIndex);
            curIndex = nextIndex;
        }
    }

}
