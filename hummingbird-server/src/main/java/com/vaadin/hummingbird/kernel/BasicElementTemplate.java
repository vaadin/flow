package com.vaadin.hummingbird.kernel;

public class BasicElementTemplate extends AbstractElementTemplate {
    private static final BasicElementTemplate instance = new BasicElementTemplate();

    // Singleton constructor
    private BasicElementTemplate() {
        assert getId() == 0;
    }

    public static BasicElementTemplate get() {
        return instance;
    }

    @Override
    public String getTag(StateNode node) {
        return node.get(Keys.TAG, String.class);
    }

    @Override
    public String getIs(StateNode node) {
        return (String) node.get(Keys.IS);
    }

    public static StateNode createBasicElementModel(String tag) {
        assert validTagName(tag) : "Invalid tag name " + tag;
        StateNode node = StateNode.create();
        node.put(Keys.TAG, tag);
        return node;
    }

    public static StateNode createBasicElementModel(String tag, String is) {
        StateNode node = createBasicElementModel(tag);
        assert validCustomElementTypeExtension(
                is) : "Custom element type (is-attribute) cannot be null and must always contain a dash (-), e.g. x-foo.";
        node.put(Keys.IS, is);
        return node;
    }

    private static boolean validCustomElementTypeExtension(String is) {
        return is != null && is.length() > 2 && is.contains("-");
    }

    private static boolean validTagName(String tag) {
        if (tag.contains("<") || tag.contains(">")) {
            return false;
        }

        return true;
    }

    @Override
    public StateNode getElementDataNode(StateNode node,
            boolean createIfNeeded) {
        return node;
    }

    @Override
    public boolean supports(StateNode node) {
        return node.get(Keys.TAG) != null;
    }
}
