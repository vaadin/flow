package com.vaadin.hummingbird.kernel;

public class ModelBinding implements Binding {

    private ModelPath path;

    public ModelBinding(ModelPath path) {
        this.path = path;
    }

    public ModelBinding(String propertyName) {
        this(new ModelPath(propertyName));
    }

    public ModelPath getPath() {
        return path;
    }

    @Override
    public Object getValue(StateNode node) {
        return path.getNode(node).get(path.getNodeProperty());
    }

}
