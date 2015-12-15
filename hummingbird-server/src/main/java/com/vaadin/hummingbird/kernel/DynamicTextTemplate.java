package com.vaadin.hummingbird.kernel;

public class DynamicTextTemplate extends BoundElementTemplate {
    public DynamicTextTemplate(Binding binding) {
        super(TemplateBuilder.withTag("#text").bindAttribute("content",
                binding));
    }

    public Binding getBinding() {
        return getAttributeBindings().get("content");
    }
}
