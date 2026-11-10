/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend.scanner;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.CssClass1;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.CssClass2;

import static com.vaadin.flow.server.frontend.scanner.ScannerDependenciesTest.getFrontendDependencies;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScannerCssTest {

    @Test
    void should_visitCssImports() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(CssClass1.class,
                CssClass2.class);
        assertEquals(3, deps.getEntryPoints().size());
        for (EntryPointData entryPoint : deps.getEntryPoints()) {
            if (entryPoint.getName().equals(UI.class.getName())) {
                continue;
            }
            assertEquals(4, entryPoint.getCss().size(),
                    "Wrong amount of css in " + entryPoint.getName());

            assertThat(entryPoint.getCss(),
                    containsInAnyOrder(
                            new CssData("./foo.css", null, null, null),
                            new CssData("./foo.css", null, "bar", null),
                            new CssData("./foo.css", "bar", null, null),
                            new CssData("./foo.css", null, null, "bar")));
        }
        ;
    }

    @Test
    void should_gatherCssImportsInOrderPerClass() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(CssClass3.class);
        assertEquals(2, deps.getEntryPoints().size());
        for (EntryPointData entryPoint : deps.getEntryPoints()) {
            if (entryPoint.getName().equals(UI.class.getName())) {
                continue;
            }
            assertEquals(4, entryPoint.getCss().size(),
                    "Wrong amount of css in " + entryPoint.getName());

            // verifies #6523 as sufficiently complex names can get mixed up
            assertThat(entryPoint.getCss(),
                    contains(new CssData("./foo.css", null, null, null),
                            new CssData("./bar.css", null, null, null),
                            new CssData("./foofoo.css", null, null, null),
                            new CssData("./foobar.css", null, null, null)));
        }
        ;
    }

    @Test
    void should_sumarizeCssImports() throws Exception {
        FrontendDependencies deps = getFrontendDependencies(CssClass1.class,
                CssClass2.class);
        DepsTests.assertImportCount(4, deps.getCss());
    }

    // Moved here from ScannerTestComponents. Otherwise this would affect
    // NodeUpdateImportsTest.java
    @Route("css-route-3")
    @CssImport("./foo.css")
    @CssImport(value = "./bar.css")
    @CssImport(value = "./foofoo.css")
    @CssImport(value = "./foobar.css")
    public static class CssClass3 {
    }

}
