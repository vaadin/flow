package com.vaadin.hummingbird.kernel;

public class BasicElementTemplate extends AbstractElementTemplate {
    private static final BasicElementTemplate instance = new BasicElementTemplate();

    // Singleton constructor
    private BasicElementTemplate() {
    }

    public static BasicElementTemplate get() {
        return instance;
    }

    @Override
    public String getTag(StateNode node) {
        return node.get(Keys.TAG, String.class);
    }

    public static StateNode createBasicElementModel(String tag) {
        StateNode node = StateNode.create();
        node.put(Keys.TAG, tag);
        return node;
    }

    @Override
    protected StateNode getElementDataNode(StateNode node, boolean createIfNeeded) {
        return node;
    }

    @Override
    public boolean supports(StateNode node) {
        return node.get(Keys.TAG) != null;
    }
}
