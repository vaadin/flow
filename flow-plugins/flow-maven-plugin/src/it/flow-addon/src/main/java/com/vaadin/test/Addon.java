/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.test;

import java.util.List;

import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.TypeScriptBootstrapModifier;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

public class Addon implements TypeScriptBootstrapModifier {

    @Override
    public void modify(List<String> bootstrapTypeScript, Options options,
            FrontendDependenciesScanner frontendDependenciesScanner) {
        bootstrapTypeScript.add("""
                (window as any).testAddOn=1;
                """);
    }
}
