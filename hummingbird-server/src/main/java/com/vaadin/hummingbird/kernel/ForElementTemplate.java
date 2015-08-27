package com.vaadin.hummingbird.kernel;

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
        return listPath.getNode(node)
                .getMultiValued(listPath.getNodeProperty());
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

    public ModelPath getModelProperty() {
        return listPath;
    }

    public String getInnerScope() {
        return innerScope;
    }
}
