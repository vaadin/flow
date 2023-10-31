package com.vaadin.viteapp;

import java.util.List;

import com.vaadin.flow.server.frontend.TypeScriptBootstrapModifier;

public class BootstrapModifier implements TypeScriptBootstrapModifier {

    @Override
    public void modify(List<String> bootstrapTypeScript) {
        bootstrapTypeScript.add("(window as any).bootstrapMod=1;");
    }

}
