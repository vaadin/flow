/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.slf4j.Logger;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.server.frontend.NodeTestComponents.LumoTest;
import com.vaadin.flow.server.frontend.NodeTestComponents.MainLayout;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.DepsTests;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.AbstractTheme;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractUpdateImportsTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Route(value = "simplecss")
    @CssImport("./foo.css")
    public static class FooCssImport extends Component {

    }

    @Route(value = "themefor")
    @CssImport(value = "./foo.css", themeFor = "something")
    public static class ThemeForCssImport extends Component {

    }

    @Route(value = "simplecss")
    @CssImport("./bar.css")
    public static class BarCssImport extends Component {

    }

    @Route(value = "simplecss2")
    @CssImport("./foo.css")
    public static class FooCssImport2 extends Component {

    }

    protected File tmpRoot;
    protected File frontendDirectory;
    protected File nodeModulesPath;
    protected UpdateImports updater;

    private MockLogger logger;

    private static final String ERROR_MSG = "foo-bar-baz";

    private FeatureFlags featureFlags;

    protected Options options;

    private File tokenFile;

    class UpdateImports extends AbstractUpdateImports {

        private Map<File, List<String>> output;

        UpdateImports(ClassFinder classFinder,
                FrontendDependenciesScanner scanner, Options options) {
            super(options, scanner, classFinder);
        }

        @Override
        protected void writeOutput(Map<File, List<String>> output) {
            this.output = output;
        }

        public Map<File, List<String>> getOutput() {
            return output;
        }

        protected List<String> getMergedOutput() {
            return merge(output);
        }

        @Override
        protected Collection<String> getGeneratedModules() {
            return Arrays.asList("generated-modules-foo",
                    "generated-modules-bar");
        }

        @Override
        protected Logger getLogger() {
            return logger;
        }

        @Override
        protected String getImportsNotFoundMessage() {
            return ERROR_MSG;
        }
    }

    @Before
    public void setup() throws Exception {
        tmpRoot = temporaryFolder.getRoot();

        logger = new MockLogger();

        frontendDirectory = new File(tmpRoot, DEFAULT_FRONTEND_DIR);
        nodeModulesPath = new File(tmpRoot, NODE_MODULES);
        tokenFile = new File(tmpRoot, TOKEN_FILE);

        ClassFinder classFinder = getClassFinder();
        featureFlags = Mockito.mock(FeatureFlags.class);
        options = new Options(Mockito.mock(Lookup.class), tmpRoot)
                .withTokenFile(tokenFile).withProductionMode(true)
                .withFeatureFlags(featureFlags).withBundleBuild(true);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
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
    public void importsFilesAreNotFound_throws() {
        deleteExpectedImports(frontendDirectory, nodeModulesPath);
        exception.expect(IllegalStateException.class);
        updater.run();
    }

    @Test
    public void getModuleLines_npmPackagesDontExist_logExplanation() {
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

        MatcherAssert.assertThat(logger.getLogs(),
                CoreMatchers.allOf(
                        CoreMatchers.containsString(
                                "@vaadin/vaadin-lumo-styles/spacing.js"),
                        CoreMatchers.containsString(ERROR_MSG)));
    }

    @Test
    public void getModuleLines_oneFrontendDependencyDoesntExist_throwExceptionAndlogExplanation() {
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
    public void getModuleLines_oneFrontendDependencyAndFrontendDirectoryDontExist_throwExceptionAdvisingUserToRunPrepareFrontend()
            throws Exception {
        ClassFinder classFinder = getClassFinder();
        options.withTokenFile(null);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);

        Files.move(frontendDirectory.toPath(),
                new File(tmpRoot, "_frontend").toPath());

        try {
            updater.run();
            Assert.fail(
                    "Execute should have failed with advice to run `prepare-frontend`");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString(
                    "Unable to locate frontend resources and missing token file. "
                            + "Please run the `prepare-frontend` Vaadin plugin goal before deploying the application"));
        }
    }

    @Test
    public void getModuleLines_multipleFrontendDependencyDoesntExist_throwExceptionAndlogExplanation() {
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
                + "%n      - `%s` in included JARs"
                + "%n%n  Please, double check that those files exist. If you use a custom directory "
                + "for your resource files instead of default "
                + "`frontend` folder then make sure you it's correctly configured "
                + "(e.g. set '%s' property)", frontendDirectory.getPath(),
                Constants.RESOURCES_FRONTEND_DEFAULT,
                Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT,
                FrontendUtils.PARAM_FRONTEND_DIR);

        return String.format("%n%n  %s%n      - %s%n  %s%n%n", prefix,
                String.join("\n      - ", resourcesNotFound), suffix);
    }

    @Test
    public void generateLines_resultingLinesContainsThemeLinesAndExpectedImportsAndCssLinesAndGeneratedImportsAndLoggerReports()
            throws Exception {
        List<String> expectedLines = new ArrayList<>();
        getExpectedImports().stream().filter(imp -> imp.equals("/foo.css"))
                .forEach(imp -> expectedLines
                        .add("import '" + addWebpackPrefix(imp) + "';"));

        // An import without `.js` extension
        expectedLines.add(
                "import '@vaadin/vaadin-mixed-component/theme/lumo/vaadin-something-else';");
        // An import not found in node_modules
        expectedLines.add("import 'unresolved/component';");

        expectedLines.add(
                "import $cssFromFile_0 from '@vaadin/vaadin-mixed-component/bar.css?inline';");
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
                "injectGlobalCss($cssFromFile_0.toString(), 'CSSImport end', document);");
        expectedLines.add(
                "injectGlobalCss($cssFromFile_1.toString(), 'CSSImport end', document);");
        expectedLines.add(
                "addCssBlock(`<style include=\"bar\">${$css_2}</style>`);");
        expectedLines.add("registerStyles('', $css_3, {moduleId: 'baz'});");
        expectedLines.add(
                "registerStyles('', $css_4, {include: 'bar', moduleId: 'baz'});");
        expectedLines.add(
                "registerStyles('foo-bar', $css_5, {moduleId: 'flow_css_mod_5'});");
        expectedLines.add(
                "registerStyles('foo-bar', $css_6, {include: 'bar', moduleId: 'flow_css_mod_6'});");

        expectedLines
                .add("import 'Frontend/generated/flow/generated-modules-foo';");
        expectedLines
                .add("import 'Frontend/generated/flow/generated-modules-bar';");

        updater.run();

        for (String line : expectedLines) {
            Assert.assertTrue(
                    "\n" + line + " IS NOT FOUND IN: \n"
                            + updater.getMergedOutput(),
                    updater.getMergedOutput().contains(line));
        }

        // All generated module ids are distinct
        Pattern moduleIdPattern = Pattern
                .compile(".*moduleId: '(flow_css_mod_[^']*)'.*");
        List<String> moduleIds = updater.getMergedOutput().stream()
                .map(moduleIdPattern::matcher).filter(Matcher::matches)
                .map(m -> m.group(1)).collect(Collectors.toList());
        long uniqueModuleIds = moduleIds.stream().distinct().count();
        Assert.assertTrue("expected modules", moduleIds.size() > 0);
        Assert.assertEquals("duplicates in generated " + moduleIds,
                moduleIds.size(), uniqueModuleIds);

        String output = logger.getLogs();

        MatcherAssert.assertThat(output, CoreMatchers.containsString(
                "Use the './' prefix for files in JAR files: 'ExampleConnector.js'"));
        MatcherAssert.assertThat(output, CoreMatchers
                .containsString("Use the './' prefix for files in the '"
                        + frontendDirectory.getPath()
                        + "' folder: 'vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js'"));

        // Using regex match because of the ➜ character in TC
        MatcherAssert.assertThat(output, CoreMatchers.containsString(
                "Failed to find the following imports in the `node_modules` tree:\n      - unresolved/component"));

        MatcherAssert.assertThat(output,
                CoreMatchers.not(CoreMatchers.containsString(
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

    @Test
    public void jsModulesOrderIsPreservedAnsAfterJsModules() {
        updater.run();

        assertImportOrder("jsmodule/g.js", "javascript/a.js", "javascript/b.js",
                "javascript/c.js");
    }

    @Test
    public void duplicateEagerCssOnlyImportedOnce() throws Exception {
        Class<?>[] testClasses = { FooCssImport.class, FooCssImport2.class,
                UI.class, AllEagerAppConf.class };
        ClassFinder classFinder = getClassFinder(testClasses);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

        Map<File, List<String>> output = updater.getOutput();

        File flowGeneratedImports = FrontendUtils
                .getFlowGeneratedImports(frontendDirectory);

        assertOnce("import { injectGlobalCss } from",
                output.get(flowGeneratedImports));
        assertOnce("import { css, unsafeCSS, registerStyles } from",
                output.get(flowGeneratedImports));
        assertOnce("from 'Frontend/foo.css?inline';",
                output.get(flowGeneratedImports));
    }

    @Test
    public void eagerCssImportsMerged() throws Exception {
        createExpectedImport(frontendDirectory, nodeModulesPath, "./bar.css");
        Class<?>[] testClasses = { FooCssImport.class, BarCssImport.class,
                UI.class, AllEagerAppConf.class };
        ClassFinder classFinder = getClassFinder(testClasses);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

        Map<File, List<String>> output = updater.getOutput();

        File flowGeneratedImports = FrontendUtils
                .getFlowGeneratedImports(frontendDirectory);

        assertOnce("import { injectGlobalCss } from",
                output.get(flowGeneratedImports));
        assertOnce("import { css, unsafeCSS, registerStyles } from",
                output.get(flowGeneratedImports));
        assertOnce("from 'Frontend/foo.css?inline';",
                output.get(flowGeneratedImports));
        assertOnce("from 'Frontend/bar.css?inline';",
                output.get(flowGeneratedImports));
        assertOnce("import $cssFromFile_0 from",
                output.get(flowGeneratedImports));
        assertOnce("import $cssFromFile_1 from",
                output.get(flowGeneratedImports));
    }

    @Test
    public void themeForCssImports_eagerLoaded() throws Exception {
        Class<?>[] testClasses = { ThemeForCssImport.class, UI.class };
        ClassFinder classFinder = getClassFinder(testClasses);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

        Map<File, List<String>> output = updater.getOutput();

        File flowGeneratedImports = FrontendUtils
                .getFlowGeneratedImports(frontendDirectory);

        assertOnce("import { injectGlobalCss } from",
                output.get(flowGeneratedImports));
        assertOnce("import { css, unsafeCSS, registerStyles } from",
                output.get(flowGeneratedImports));
        assertOnce("from 'Frontend/foo.css?inline';",
                output.get(flowGeneratedImports));
        assertOnce("import $cssFromFile_0 from",
                output.get(flowGeneratedImports));
    }

    protected void assertOnce(String key, List<String> output) {
        int found = 0;
        for (String row : output) {
            if (row.contains(key)) {
                found++;
            }
        }
        Assert.assertEquals("Expected one instance of '" + key + "'", 1, found);
    }

    @Test
    public void importingBinaryFile_importVisitorShouldNotFail()
            throws IOException, URISyntaxException {
        // Add a binary image import to 'commmon-js-file.js' which should not
        // fail the import visitor and should be ignored
        File newFile = resolveImportFile(frontendDirectory, nodeModulesPath,
                "./common-js-file.js");
        Files.copy(
                Paths.get(getClass().getClassLoader().getResource("dice.jpg")
                        .toURI()),
                new File(newFile.getParentFile(), "dice.jpg").toPath());
        Files.write(newFile.toPath(),
                Collections.singleton("import './dice.jpg'"));

        updater.run();
    }

    @Route("")
    @JsModule("./jsm-all.js")
    @JsModule(value = "./jsm-all2.js", developmentOnly = false)
    @JsModule(value = "./jsm-dev.js", developmentOnly = true)
    @JavaScript("./js-all.js")
    @JavaScript(value = "./js-all2.js", developmentOnly = false)
    @JavaScript(value = "./js-dev.js", developmentOnly = true)
    public static class DevelopmentAndProductionDependencies extends Component {
    }

    @Test
    public void developmentDependencies_includedInDevelopmentMode()
            throws IOException, URISyntaxException {
        createAndLoadDependencies(false);

        List<String> out = updater.getMergedOutput().stream()
                .filter(row -> !row.startsWith("export "))
                .filter(row -> !row.startsWith("window.Vaadin"))
                .filter(row -> !row.contains("Frontend/generated/flow"))
                .filter(row -> !row.contains("const loadOnDemand"))
                .filter(row -> !row.contains(
                        "@vaadin/common-frontend/ConnectionIndicator"))
                .toList();

        Assert.assertEquals(List.of("import 'Frontend/jsm-all.js';",
                "import 'Frontend/jsm-all2.js';",
                "import 'Frontend/jsm-dev.js';", "import 'Frontend/js-all.js';",
                "import 'Frontend/js-all2.js';",
                "import 'Frontend/js-dev.js';"), out);

    }

    @Test
    public void developmentDependencies_notIncludedInProductionMode()
            throws IOException, URISyntaxException {
        createAndLoadDependencies(true);

        List<String> out = updater.getMergedOutput().stream()
                .filter(row -> !row.startsWith("export "))
                .filter(row -> !row.startsWith("window.Vaadin"))
                .filter(row -> !row.contains("Frontend/generated/flow"))
                .filter(row -> !row.contains("const loadOnDemand"))
                .filter(row -> !row.contains(
                        "@vaadin/common-frontend/ConnectionIndicator"))
                .toList();
        Assert.assertEquals(List.of("import 'Frontend/jsm-all.js';",
                "import 'Frontend/jsm-all2.js';",
                "import 'Frontend/js-all.js';",
                "import 'Frontend/js-all2.js';"), out);
    }

    private void createAndLoadDependencies(boolean productionMode)
            throws IOException {
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./jsm-all.js");
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./jsm-all2.js");
        createExpectedImport(frontendDirectory, nodeModulesPath, "./js-all.js");
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./js-all2.js");

        if (!productionMode) {
            createExpectedImport(frontendDirectory, nodeModulesPath,
                    "./jsm-dev.js");
            createExpectedImport(frontendDirectory, nodeModulesPath,
                    "./js-dev.js");
        }

        ClassFinder classFinder = getClassFinder(
                DevelopmentAndProductionDependencies.class);

        options.withProductionMode(productionMode);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

    }

    @Route(value = "")
    static class MainView extends Component {
        NodeTestComponents.TranslatedImports translatedImports;
        NodeTestComponents.LocalP3Template localP3Template;
        NodeTestComponents.JavaScriptOrder javaScriptOrder;
    }

    @LoadDependenciesOnStartup
    static class AllEagerAppConf implements AppShellConfigurator {

    }

    @JsModule("./fake-material-theme.js")
    public static class FakeMaterialTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return "fake-material-base";
        }

        @Override
        public String getThemeUrl() {
            return null;
        }

    }

    @Test
    public void multipleThemes_importsOnlyFromActiveTheme() throws Exception {
        // The active theme comes from NodeTestComponents.MainLayout
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./fake-material-theme.js");
        Class[] testClasses = { FakeMaterialTheme.class, MainLayout.class,
                LumoTest.class };
        ClassFinder classFinder = getClassFinder(testClasses);

        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();
        String output = String.join("\n", updater.getMergedOutput());

        // Lumo is the active theme so its imports should be included
        Assert.assertTrue(output
                .contains("import '@vaadin/vaadin-lumo-styles/color.js';"));

        // FakeMaterialTheme is inactive and its JS module annotation value
        // should not be there
        Assert.assertFalse(output.contains("Frontend/fake-material-theme.js"));

    }

    public void assertFullSortOrder(boolean uiImportSeparated,
            List<String> expectedJsModuleImports) throws MalformedURLException {
        Class[] testClasses = { MainView.class,
                NodeTestComponents.TranslatedImports.class,
                NodeTestComponents.LocalP3Template.class,
                NodeTestComponents.JavaScriptOrder.class, UI.class,
                AllEagerAppConf.class };
        ClassFinder classFinder = getClassFinder(testClasses);

        options.withTokenFile(new File(tmpRoot, TOKEN_FILE));
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

        // Imports are collected as
        // - theme and css
        // - JsModules
        // - JavaScript
        // - Generated webcompoents
        List<String> expectedImports = new ArrayList<>();
        List<String> uiAndGeneratedImports = new ArrayList<>();

        // JsModules
        expectedImports.addAll(expectedJsModuleImports);

        getAnntotationsAsStream(JavaScript.class, testClasses)
                .map(JavaScript::value).map(this::updateToImport)
                .forEach(expectedImports::add);

        if (uiImportSeparated) {
            String uiImport = expectedImports.stream()
                    .filter(dep -> dep.contains(DepsTests.UI_IMPORT))
                    .findFirst().get();
            expectedImports.remove(uiImport);
            uiAndGeneratedImports.add(uiImport);
        }
        updater.getGeneratedModules().stream()
                .map(updater::resolveGeneratedModule).map(this::updateToImport)
                .forEach(uiAndGeneratedImports::add);

        List<String> result = updater.getMergedOutput();
        result.removeIf(line -> line.startsWith("import { injectGlobalCss }"));
        result.removeIf(line -> line.startsWith("export "));
        result.removeIf(line -> line.isBlank());
        result.removeIf(line -> line.contains("loadOnDemand"));
        result.removeIf(line -> line.contains("window.Vaadin"));

        expectedImports.addAll(uiAndGeneratedImports);
        Assert.assertEquals(expectedImports, result);
    }

    private <T extends Annotation> Stream<T> getAnntotationsAsStream(
            Class<T> annotation, Class<?>... classes) {
        Stream<T> stream = Stream.empty();
        for (Class<?> clazz : classes) {
            stream = Stream.concat(stream,
                    Stream.of(clazz.getAnnotationsByType(annotation)));
        }
        return stream;
    }

    private String updateToImport(String value) {
        if (value.startsWith("./")) {
            value = value.replace("./", FrontendUtils.FRONTEND_FOLDER_ALIAS);
        }
        return String.format("import '%s';", value);
    }

    private void assertContainsImports(boolean contains, String... imports) {
        for (String line : imports) {
            boolean result = updater.getMergedOutput()
                    .contains("import '" + addWebpackPrefix(line) + "';");
            String message = "\n  " + (contains ? "NOT " : "") + "FOUND '"
                    + line + " IN: \n" + updater.getMergedOutput();
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
            int nextIndex = updater.getMergedOutput()
                    .indexOf("import '" + prefixed + "';");
            assertTrue("import '" + prefixed + "' not found", nextIndex != -1);
            assertTrue("import '" + prefixed + "' appears in the wrong order",
                    curIndex <= nextIndex);
            curIndex = nextIndex;
        }
    }

}
