package com.vaadin.viteapp;

import com.vaadin.flow.server.frontend.TypeScriptBootstrapModifier;

public class BootstrapModifier implements TypeScriptBootstrapModifier {

    @Override
    public String modify(String bootstrapTypeScript) {
        return bootstrapTypeScript + "(window as any).bootstrapMod=1;";
    }

}
