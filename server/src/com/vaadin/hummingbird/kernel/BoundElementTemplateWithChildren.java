package com.vaadin.hummingbird.kernel;

import java.util.Collection;
import java.util.Map;

public abstract class BoundElementTemplateWithChildren extends
        BoundElementTemplate {
    public BoundElementTemplateWithChildren(String tag,
            Collection<AttributeBinding> attributeBindings,
            Map<String, String> defaultAttributeValues) {
        super(tag, attributeBindings, defaultAttributeValues);
    }

    protected abstract int doGetChildCount(StateNode node);

    protected abstract StateNode getChildNode(int childIndex, StateNode node);

    protected abstract ElementTemplate getChildTemplate(int childIndex,
            StateNode node);

    @Override
    public int getChildCount(StateNode node) {
        return doGetChildCount(node);
    }

    @Override
    public Element getChild(int index, StateNode node) {
        StateNode childNode = getChildNode(index, node);
        ElementTemplate childTemplate = getChildTemplate(index, node);
        return Element.getElement(childTemplate, childNode);
    }

    @Override
    public void insertChild(int index, Element child, StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeChild(StateNode node, Element element) {
        throw new UnsupportedOperationException();
    }

}
