package com.vaadin.hummingbird.kernel;

import java.util.List;
import java.util.Map;

public class ForElementTemplate extends BoundElementTemplateWithChildren {

    private String modelProperty;
    private ElementTemplate childTemplate;

    public ForElementTemplate(String tag, List<AttributeBinding> attributeBindings, Map<String, String> defaultAttributes, String modelProperty, BoundElementTemplate childTemplate) {
        super(tag, attributeBindings, defaultAttributes);
        this.modelProperty = modelProperty;
        this.childTemplate = childTemplate;
        childTemplate.setParentResolver(node -> {
            StateNode parent = node.getParent();
            if (parent == null) {
                return null;
            }
            return Element.getElement(this, parent);
        });
    }

    @Override
    protected int doGetChildCount(StateNode node) {
        List<Object> stateList = getChildNodes(node);
        if (stateList == null) {
            return 0;
        } else {
            return stateList.size();
        }
    }

    private List<Object> getChildNodes(StateNode node) {
        return node.getMultiValued(modelProperty);
    }

    @Override
    protected StateNode getChildNode(int childIndex, StateNode node) {
        return (StateNode) getChildNodes(node).get(childIndex);
    }

    @Override
    protected ElementTemplate getChildTemplate(int childIndex, StateNode node) {
        return childTemplate;
    }

    public ElementTemplate getChildTemplate() {
        return childTemplate;
    }

    public String getModelProperty() {
        return modelProperty;
    }
}
