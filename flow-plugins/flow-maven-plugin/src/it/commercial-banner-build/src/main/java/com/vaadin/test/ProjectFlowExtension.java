package com.vaadin.test;

import java.util.List;

import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.TypeScriptBootstrapModifier;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

/**
 * Hello world!
 */
public class ProjectFlowExtension implements TypeScriptBootstrapModifier {

    @Override
    public void modify(List<String> bootstrapTypeScript, Options options,
            FrontendDependenciesScanner frontendDependenciesScanner) {
        System.out.println("ProjectFlowExtension");
        bootstrapTypeScript.add("(window as any).testProject=1;");
    }
}
