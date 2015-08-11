package com.vaadin.hummingbird.kernel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StaticChildrenElementTemplate extends BoundElementTemplateWithChildren {

    private final List<BoundElementTemplate> children;

    public StaticChildrenElementTemplate(String tag, List<AttributeBinding> boundAttributes, Map<String, String> defaultAttributes, List<BoundElementTemplate> children) {
        super(tag, boundAttributes, defaultAttributes);
        this.children = children;

        children.forEach(c -> c.setParentResolver(node -> Element.getElement(this, node)));
    }

    @Override
    protected int doGetChildCount(StateNode node) {
        return children.size();
    }

    @Override
    protected StateNode getChildNode(int childIndex, StateNode node) {
        return node;
    }

    @Override
    protected ElementTemplate getChildTemplate(int childIndex, StateNode node) {
        return children.get(childIndex);
    }

    public List<BoundElementTemplate> getChildren() {
        return Collections.unmodifiableList(children);
    }
}
