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
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
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
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.shared.ui.LoadMode;

public class UpdateImportsWithByteCodeScannerTest
        extends AbstractUpdateImportsTest {

    @Override
    protected FrontendDependenciesScanner getScanner(ClassFinder finder) {
        return new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                .createScanner(false, finder, true);
    }

    @Test
    public void assertFullSortOrder() throws MalformedURLException {
        super.assertFullSortOrder(true);
    }

    @JavaScript("./lazy-component-javascript.js")
    @JsModule("./lazy-component-jsmodule.js")
    @CssImport("./lazy-component-cssimport.css")
    public static class LazyComponent extends Component {

    }

    @Route(value = "lazy", loadMode = LoadMode.LAZY)
    static class LazyRoute extends Component {
        LazyComponent lazyComponent;
    }

    @Test
    public void lazyRouteIsLazyLoaded() throws Exception {
        Class<?>[] testClasses = { MainView.class,
                NodeTestComponents.TranslatedImports.class,
                NodeTestComponents.LocalP3Template.class, LazyRoute.class,
                UI.class };

        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./lazy-component-javascript.js");
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./lazy-component-jsmodule.js");
        createExpectedImport(frontendDirectory, nodeModulesPath,
                "./lazy-component-cssimport.css");
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
                new String[] { "Frontend/local-p3-template.js",
                        "Frontend/lazy-component-cssimport.css" },
                new String[] { "Frontend/lazy-component-javascript.js",
                        "Frontend/lazy-component-jsmodule.js" });
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
            Assert.assertTrue("Lazy import should not contain " + lazyImport,
                    lazyImportContent.contains(lazyImport));
        }

    }

}
