package com.vaadin.hummingbird.kernel;

public interface TemplateBuilder {

    public BoundElementTemplate build();

    public static BoundTemplateBuilder withTag(String tag) {
        return new BoundTemplateBuilder(tag);
    }

    public static BoundTemplateBuilder withTag(String tag, String is) {
        return new BoundTemplateBuilder(tag, is);
    }

    public static TemplateBuilder staticText(String text) {
        return () -> new StaticTextTemplate(text);
    }

    public static TemplateBuilder dynamicText(String modelPath) {
        return dynamicText(new StateNodeBinding(modelPath));
    }

    public static TemplateBuilder dynamicText(Binding binding) {
        return () -> new DynamicTextTemplate(binding);
    }
}
