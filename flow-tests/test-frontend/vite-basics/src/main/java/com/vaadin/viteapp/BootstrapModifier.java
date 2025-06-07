package com.vaadin.viteapp;

import java.util.List;

import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.TypeScriptBootstrapModifier;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

public class BootstrapModifier implements TypeScriptBootstrapModifier {

    public void modify(List<String> bootstrapTypeScript, Options options,
            FrontendDependenciesScanner frontendDependenciesScanner) {
        bootstrapTypeScript.add("(window as any).bootstrapMod=1;");
    }

}
