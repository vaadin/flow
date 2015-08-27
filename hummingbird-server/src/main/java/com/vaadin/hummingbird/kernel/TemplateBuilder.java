package com.vaadin.hummingbird.kernel;

import java.util.function.Function;

public interface TemplateBuilder {

    public BoundElementTemplate build();

    public static BoundTemplateBuilder withTag(String tag) {
        return new BoundTemplateBuilder(tag);
    }

    public static TemplateBuilder staticText(String text) {
        return () -> new StaticTextTemplate(text);
    }

    public static TemplateBuilder dynamicText(String modelPath) {
        return () -> new DynamicTextTemplate(modelPath);
    }

    public static TemplateBuilder dynamicText(
            Function<StateNode, String> function) {
        return () -> new DynamicTextTemplate(function);
    }

    public static TemplateBuilder dynamicText(ModelPath path) {
        return () -> new DynamicTextTemplate(path);
    }
}
