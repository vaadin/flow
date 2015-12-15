package com.vaadin.hummingbird.kernel;

import java.util.Collections;
import java.util.List;

public class ForElementTemplate extends BoundElementTemplate {

    private final Binding listBinding;
    private final String innerScope;

    public ForElementTemplate(BoundTemplateBuilder builder, Binding listBinding,
            String innerScope) {
        super(builder);
        this.listBinding = listBinding;
        this.innerScope = innerScope;
    }

    @Override
    protected int getElementCount(StateNode node) {
        return getChildNodes(node).size();
    }

    private List<?> getChildNodes(StateNode node) {
        Object value = listBinding.getValue(node);
        if (value instanceof ListNode) {
            ListNode listNode = (ListNode) value;
            return new ListNodeAsList(listNode);
        } else {
            return Collections.emptyList();
        }
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

    public Binding getListBinding() {
        return listBinding;
    }

    public String getInnerScope() {
        return innerScope;
    }
}
