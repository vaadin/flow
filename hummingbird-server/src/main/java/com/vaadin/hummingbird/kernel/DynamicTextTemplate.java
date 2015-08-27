package com.vaadin.hummingbird.kernel;

import java.util.function.Function;

public class DynamicTextTemplate extends BoundElementTemplate {
    private DynamicTextTemplate(AttributeBinding binding) {
        super(TemplateBuilder.withTag("#text").bindAttribute(binding));
    }

    public DynamicTextTemplate(Function<StateNode, String> function) {
        this(createBinding(function));
    }

    public DynamicTextTemplate(ModelPath modelPath) {
        this(new ModelAttributeBinding("content", modelPath));
    }

    public DynamicTextTemplate(String modelPath) {
        this(new ModelPath(modelPath));
    }

    private static AttributeBinding createBinding(
            Function<StateNode, String> function) {
        return new AttributeBinding("content") {
            @Override
            public String getValue(StateNode node) {
                return function.apply(node);
            }
        };
    }

    public AttributeBinding getBinding() {
        return getAttributeBindings().get("content");
    }
}
