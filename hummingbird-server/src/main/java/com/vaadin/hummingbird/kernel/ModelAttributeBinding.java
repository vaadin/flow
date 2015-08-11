package com.vaadin.hummingbird.kernel;

public class ModelAttributeBinding extends AttributeBinding {

    private String modelPath;

    public ModelAttributeBinding(String attributeName, String modelPath) {
        super(attributeName);
        this.modelPath = modelPath;
    }

    @Override
    public String getValue(StateNode node) {
        return node.get(modelPath, String.class);
    }

    public String getModelPath() {
        return modelPath;
    }
}
