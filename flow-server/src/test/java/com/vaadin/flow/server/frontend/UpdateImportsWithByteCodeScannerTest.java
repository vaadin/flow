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
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.DependencyTrigger;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.DepsTests;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

public class UpdateImportsWithByteCodeScannerTest
        extends AbstractUpdateImportsTest {

    @Override
    protected FrontendDependenciesScanner getScanner(ClassFinder finder) {
        return new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                .createScanner(false, finder, true);
    }

    @Test
    public void assertFullSortOrder() throws MalformedURLException {
        List<String> expectedJsModuleImports = new ArrayList<>();
        expectedJsModuleImports.add(
                "import '@vaadin/vaadin-mixed-component/src/vaadin-mixed-component.js';");
        expectedJsModuleImports.add(
                "import '@vaadin/vaadin-mixed-component/src/vaadin-something-else.js';");
        expectedJsModuleImports.add(
                "import '@vaadin/vaadin-mixed-component/src/vaadin-something-else';");
        expectedJsModuleImports.add(
                "import '@vaadin/vaadin-mixed-component/src/vaadin-custom-themed-component.js';");
        expectedJsModuleImports.add("import 'jsmodule/h.js';");
        expectedJsModuleImports.add("import 'jsmodule/g.js';");
        expectedJsModuleImports.add("import 'Frontend/local-p3-template.js';");
        expectedJsModuleImports.add("import '" + DepsTests.UI_IMPORT + "';");
        super.assertFullSortOrder(true, expectedJsModuleImports);
    }

    @JavaScript("./lazy-component-javascript.js")
    @JsModule("./lazy-component-jsmodule.js")
    @CssImport("./lazy-component-cssimport.css")
    public static class LazyComponent extends Component {

    }

    @JavaScript("./eager-component-javascript.js")
    @JsModule("./eager-component-jsmodule-2.js")
    @JsModule("./eager-component-jsmodule-1.js")
    @CssImport("./eager-component-cssimport.css")
    public static class EagerComponent extends Component {

    }

    @Route(value = "lazy")
    static class LazyRoute extends Component {
        LazyComponent lazyComponent;
    }

    @Route(value = "eager")
    static class EagerRoute extends Component {
        EagerComponent eagerComponent;
    }

    public static class TriggerClass1 extends Component {
    }

    public static class TriggerClass2 extends Component {
    }

    @Route(value = "lazy")
    @DependencyTrigger(TriggerClass1.class)
    static class LazyRouteWithOtherTrigger extends Component {
        LazyComponent lazyComponent;
    }

    @Route(value = "lazy")
    @DependencyTrigger({ TriggerClass1.class, TriggerClass2.class })
    static class LazyRouteWithOtherTriggers extends Component {
        LazyComponent lazyComponent;
    }

    @LoadDependenciesOnStartup(EagerRoute.class)
    static class LazyEagerAppConf implements AppShellConfigurator {
    }

    @LoadDependenciesOnStartup(MainView.class)
    static class LazyAppConf implements AppShellConfigurator {
    }

    @Test
    public void lazyRouteIsLazyLoaded() throws Exception {
        Class<?>[] testClasses = { MainView.class, LazyAppConf.class,
                NodeTestComponents.TranslatedImports.class,
                NodeTestComponents.LocalP3Template.class, LazyRoute.class,
                UI.class };

        createExpectedLazyImports();
        ClassFinder classFinder = getClassFinder(testClasses);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

        Map<File, List<String>> output = updater.getOutput();

        File flowGeneratedFolder = FrontendUtils
                .getFlowGeneratedFolder(frontendDirectory);
        File flowGeneratedImports = FrontendUtils
                .getFlowGeneratedImports(frontendDirectory);
        File flowGeneratedImportsDTs = new File(flowGeneratedFolder,
                FrontendUtils.IMPORTS_D_TS_NAME);

        File lazyChunk = new File(new File(flowGeneratedFolder, "chunks"),
                "chunk-" + StringUtil.getHash(LazyRoute.class.getName(),
                        StandardCharsets.UTF_8) + ".js");

        Assert.assertEquals(Set.of(flowGeneratedImports,
                flowGeneratedImportsDTs, lazyChunk), output.keySet());

        String mainImportContent = String.join("\n",
                output.get(flowGeneratedImports));
        String lazyChunkContent = String.join("\n", output.get(lazyChunk));

        assertImports(mainImportContent, lazyChunkContent,
                new String[] { "Frontend/local-p3-template.js", },
                new String[] { "Frontend/lazy-component-javascript.js",
                        "Frontend/lazy-component-jsmodule.js",
                        "Frontend/lazy-component-cssimport.css" });
    }

    @Test
    public void lazyAndEagerRoutesProperlyHandled() throws Exception {
        Class<?>[] testClasses = { LazyRoute.class, EagerRoute.class,
                LazyEagerAppConf.class, UI.class };

        createExpectedLazyImports();
        ClassFinder classFinder = getClassFinder(testClasses);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

        Map<File, List<String>> output = updater.getOutput();

        File flowGeneratedFolder = FrontendUtils
                .getFlowGeneratedFolder(frontendDirectory);
        File flowGeneratedImports = FrontendUtils
                .getFlowGeneratedImports(frontendDirectory);
        File flowGeneratedImportsDTs = new File(flowGeneratedFolder,
                FrontendUtils.IMPORTS_D_TS_NAME);

        File lazyChunk = new File(new File(flowGeneratedFolder, "chunks"),
                "chunk-" + StringUtil.getHash(LazyRoute.class.getName(),
                        StandardCharsets.UTF_8) + ".js");

        Assert.assertEquals(Set.of(flowGeneratedImports,
                flowGeneratedImportsDTs, lazyChunk), output.keySet());

        String mainImportContent = String.join("\n",
                output.get(flowGeneratedImports));
        String lazyChunkContent = String.join("\n", output.get(lazyChunk));

        assertImports(mainImportContent, lazyChunkContent,
                new String[] { "Frontend/eager-component-cssimport.css",
                        "Frontend/eager-component-javascript.js",
                        "Frontend/eager-component-jsmodule-2.js",
                        "Frontend/eager-component-jsmodule-1.js" },
                new String[] { "Frontend/lazy-component-cssimport.css",
                        "Frontend/lazy-component-javascript.js",
                        "Frontend/lazy-component-jsmodule.js", });
    }

    private void createExpectedLazyImports() throws IOException {
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./lazy-component-javascript.js");
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./lazy-component-jsmodule.js");
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./lazy-component-cssimport.css");
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./eager-component-javascript.js");
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./eager-component-jsmodule-2.js");
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./eager-component-jsmodule-1.js");
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./eager-component-cssimport.css");
    }

    @Test
    public void lazyRouteTriggeredByOtherComponent() throws Exception {
        createExpectedLazyImports();

        Class<?>[] testClasses = { LazyRouteWithOtherTrigger.class,
                LazyAppConf.class, UI.class };

        ClassFinder classFinder = getClassFinder(testClasses);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

        Map<File, List<String>> output = updater.getOutput();
        File flowGeneratedImports = FrontendUtils
                .getFlowGeneratedImports(frontendDirectory);
        String mainImportContent = String.join("\n",
                output.get(flowGeneratedImports));

        String routeHash = StringUtil.getHash(
                LazyRouteWithOtherTrigger.class.getName(),
                StandardCharsets.UTF_8);
        String triggerHash = StringUtil.getHash(TriggerClass1.class.getName(),
                StandardCharsets.UTF_8);
        // Chunk named after route
        Assert.assertTrue(mainImportContent
                .contains("chunks/chunk-" + routeHash + ".js"));
        // Trigger is only trigger and not route
        Assert.assertTrue(
                mainImportContent.contains("key === '" + triggerHash + "'"));
        Assert.assertFalse(
                mainImportContent.contains("key === '" + routeHash + "'"));
    }

    @Test
    public void lazyRouteTriggeredByOtherComponents() throws Exception {
        createExpectedLazyImports();

        Class<?>[] testClasses = { LazyRouteWithOtherTriggers.class,
                LazyAppConf.class, UI.class };

        ClassFinder classFinder = getClassFinder(testClasses);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

        Map<File, List<String>> output = updater.getOutput();
        File flowGeneratedImports = FrontendUtils
                .getFlowGeneratedImports(frontendDirectory);
        String mainImportContent = String.join("\n",
                output.get(flowGeneratedImports));

        String routeHash = StringUtil.getHash(
                LazyRouteWithOtherTriggers.class.getName(),
                StandardCharsets.UTF_8);
        String trigger1Hash = StringUtil.getHash(TriggerClass1.class.getName(),
                StandardCharsets.UTF_8);
        String trigger2Hash = StringUtil.getHash(TriggerClass2.class.getName(),
                StandardCharsets.UTF_8);
        // Chunk named after route
        Assert.assertTrue(mainImportContent
                .contains("chunks/chunk-" + routeHash + ".js"));
        // Trigger is only trigger and not route
        Assert.assertTrue(
                mainImportContent.contains("key === '" + trigger1Hash + "'"));
        Assert.assertTrue(
                mainImportContent.contains("key === '" + trigger2Hash + "'"));
        Assert.assertFalse(
                mainImportContent.contains("key === '" + routeHash + "'"));
    }

    @Test
    public void cssInLazyChunkWorks() throws Exception {
        createExpectedImport(frontendDirectory, nodeModulesPath, "./bar.css");
        Class<?>[] testClasses = { FooCssImport.class, UI.class };
        ClassFinder classFinder = getClassFinder(testClasses);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

        Map<File, List<String>> output = updater.getOutput();

        File flowGenerated = FrontendUtils
                .getFlowGeneratedFolder(frontendDirectory);
        File chunk = new File(new File(flowGenerated, "chunks"), "chunk-"
                + BundleUtils.getChunkId(FooCssImport.class.getName()) + ".js");

        assertOnce("import { injectGlobalCss } from", output.get(chunk));
        assertOnce("from 'Frontend/foo.css?inline';", output.get(chunk));
        assertOnce("import $cssFromFile_0 from", output.get(chunk));
    }

    private void assertImports(String mainImportContent,
            String lazyImportContent, String[] mainImports,
            String[] lazyImports) {

        for (String mainImport : mainImports) {
            Assert.assertTrue("Main import should contain " + mainImport,
                    mainImportContent.contains(mainImport));
            Assert.assertFalse("Lazy import should not contain " + mainImport,
                    lazyImportContent.contains(mainImport));
        }

        for (String lazyImport : lazyImports) {
            Assert.assertFalse("Main import should not contain " + lazyImport,
                    mainImportContent.contains(lazyImport));
            Assert.assertTrue("Lazy import should contain " + lazyImport,
                    lazyImportContent.contains(lazyImport));
        }

    }

    @Route("")
    public static class RootView extends Component {

    }

    @Route("login")
    public static class LoginView extends Component {

    }

    @Route("other")
    public static class OtherView extends Component {

    }

    @Test
    public void loginAndRootEagerByDefault() throws Exception {
        createExpectedLazyImports();

        Class<?>[] testClasses = { RootView.class, LoginView.class,
                OtherView.class, UI.class };

        ClassFinder classFinder = getClassFinder(testClasses);
        updater = new UpdateImports(classFinder, getScanner(classFinder),
                options);
        updater.run();

        Map<File, List<String>> output = updater.getOutput();

        assertEagerRoute(output, LoginView.class);
        assertEagerRoute(output, RootView.class);
        assertLazyRoute(output, OtherView.class);
    }

    private void assertLazyRoute(Map<File, List<String>> output,
            Class<?> routeClass) {
        assertRoute(true, output, routeClass);
    }

    private void assertEagerRoute(Map<File, List<String>> output,
            Class<?> routeClass) {
        assertRoute(false, output, routeClass);
    }

    private void assertRoute(boolean lazy, Map<File, List<String>> output,
            Class<?> routeClass) {
        File flowGeneratedImports = FrontendUtils
                .getFlowGeneratedImports(frontendDirectory);
        String mainImportContent = String.join("\n",
                output.get(flowGeneratedImports));

        String hash = StringUtil.getHash(routeClass.getName(),
                StandardCharsets.UTF_8);

        String chunkName = "chunks/chunk-" + hash + ".js";
        File chunkFile = new File(
                FrontendUtils.getFlowGeneratedFolder(frontendDirectory),
                chunkName);
        Assert.assertEquals(lazy, output.containsKey(chunkFile));
        Assert.assertEquals(lazy, mainImportContent.contains(chunkName));
    }

}
