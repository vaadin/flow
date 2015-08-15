package com.vaadin.hummingbird.kernel;

import java.util.List;
import java.util.Map;

public class ForElementTemplate extends BoundElementTemplate {

    private String modelProperty;

    public ForElementTemplate(String tag,
            List<AttributeBinding> attributeBindings,
            Map<String, String> defaultAttributes, String modelProperty,
            List<BoundElementTemplate> children) {
        super(tag, attributeBindings, defaultAttributes, children);
        this.modelProperty = modelProperty;
    }

    @Override
    protected int getElementCount(StateNode node) {
        return getChildNodes(node).size();
    }

    private List<Object> getChildNodes(StateNode node) {
        return node.getMultiValued(modelProperty);
    }

    @Override
    protected Element getElement(StateNode node, int indexInTemplate) {
        StateNode childNode = (StateNode) getChildNodes(node)
                .get(indexInTemplate);
        return Element.getElement(this, childNode);
    }

    @Override
    public Element getParent(StateNode node) {
        StateNode parentNode = node.getParent();
        if (parentNode == null) {
            return null;
        }
        return super.getParent(parentNode);
    }

    public String getModelProperty() {
        return modelProperty;
    }
}
