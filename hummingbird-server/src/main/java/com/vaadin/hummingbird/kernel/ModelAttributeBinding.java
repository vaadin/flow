package com.vaadin.hummingbird.kernel;

public class ModelAttributeBinding extends AttributeBinding {

    private ModelPath path;

    public ModelAttributeBinding(String attributeName, ModelPath path) {
        super(attributeName);
        this.path = path;
    }

    public ModelAttributeBinding(String attributeName, String propertyName) {
        this(attributeName, new ModelPath(propertyName));
    }

    public ModelPath getPath() {
        return path;
    }

    @Override
    public String getValue(StateNode node) {
        return path.getNode(node).get(path.getNodeProperty(), String.class);
    }

}
