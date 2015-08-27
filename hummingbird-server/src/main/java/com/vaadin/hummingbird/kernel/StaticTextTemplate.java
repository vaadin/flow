package com.vaadin.hummingbird.kernel;

public class StaticTextTemplate extends BoundElementTemplate {

    public StaticTextTemplate(String content) {
        super(TemplateBuilder.withTag("#text").setAttribute("content",
                content));
    }

    public String getContent() {
        return getDefaultAttributeValues().get("content");
    }
}
