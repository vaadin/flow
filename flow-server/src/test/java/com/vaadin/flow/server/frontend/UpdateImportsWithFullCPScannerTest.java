/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.DepsTests;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

public class UpdateImportsWithFullCPScannerTest
        extends AbstractUpdateImportsTest {

    @Override
    protected FrontendDependenciesScanner getScanner(ClassFinder finder) {
        return new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                .createScanner(true, finder, true, null, true);
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
        expectedJsModuleImports.add("import 'Frontend/local-p3-template.js';");
        expectedJsModuleImports.add("import 'jsmodule/h.js';");
        expectedJsModuleImports.add("import 'jsmodule/g.js';");
        expectedJsModuleImports.add("import '" + DepsTests.UI_IMPORT + "';");
        super.assertFullSortOrder(false, expectedJsModuleImports);
    }
}
