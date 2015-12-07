package com.vaadin.hummingbird.kernel;

import java.util.Collections;
import java.util.List;

public class ForElementTemplate extends BoundElementTemplate {

    private final ModelPath listPath;
    private final String innerScope;

    public ForElementTemplate(BoundTemplateBuilder builder, ModelPath listPath,
            String innerScope) {
        super(builder);
        this.listPath = listPath;
        this.innerScope = innerScope;
    }

    @Override
    protected int getElementCount(StateNode node) {
        return getChildNodes(node).size();
    }

    private List<Object> getChildNodes(StateNode node) {
        // Don't create a list if it does not exist.
        // Inefficient and can be of wrong type
        if (listPath.getNode(node).containsKey(listPath.getNodeProperty())) {
            return listPath.getNode(node)
                    .getMultiValued(listPath.getNodeProperty());
        }
        return Collections.emptyList();
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
        parentNode = parentNode.getParent();
        if (parentNode == null) {
            return null;
        }
        return super.getParent(parentNode);
    }

    public ModelPath getModelProperty() {
        return listPath;
    }

    public String getInnerScope() {
        return innerScope;
    }
}
