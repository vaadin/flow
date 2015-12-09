package com.vaadin.hummingbird.kernel;

import java.util.function.Function;

public class DynamicTextTemplate extends BoundElementTemplate {
    private DynamicTextTemplate(Binding binding) {
        super(TemplateBuilder.withTag("#text").bindAttribute("content",
                binding));
    }

    public DynamicTextTemplate(Function<StateNode, String> function) {
        this(createBinding(function));
    }

    public DynamicTextTemplate(ModelPath modelPath) {
        this(new ModelBinding(modelPath));
    }

    public DynamicTextTemplate(String modelPath) {
        this(new ModelPath(modelPath));
    }

    private static Binding createBinding(Function<StateNode, String> function) {
        return new Binding() {
            @Override
            public String getValue(StateNode node) {
                return function.apply(node);
            }
        };
    }

    public Binding getBinding() {
        return getAttributeBindings().get("content");
    }
}
