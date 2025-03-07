package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

public class FrontendScannerPwaTest extends AbstractScannerPwaTest {
    protected PwaConfiguration getPwaConfiguration(Class<?>... classes)
            throws Exception {
        FrontendDependencies frontendDependencies = new FrontendDependencies(
                new DefaultClassFinder(
                        new HashSet<>(new ArrayList<>(Arrays.asList(classes)))),
                true, null, true);
        return frontendDependencies.getPwaConfiguration();
    }
}
