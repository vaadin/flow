package com.vaadin.hummingbird.kernel;

import java.util.Collections;
import java.util.List;

public class ForElementTemplate extends BoundElementTemplate {

    private final Binding listBinding;
    private final String innerScope;
    private String indexVariable;
    private String evenVariable;
    private String oddVariable;
    private String lastVariable;

    public ForElementTemplate(BoundTemplateBuilder builder, Binding listBinding,
            String innerScope, String indexVariable, String evenVariable,
            String oddVariable, String lastVariable) {
        super(builder);
        this.listBinding = listBinding;
        this.innerScope = innerScope;
        this.indexVariable = indexVariable;
        this.evenVariable = evenVariable;
        this.oddVariable = oddVariable;
        this.lastVariable = lastVariable;
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

    public String getIndexVariable() {
        return indexVariable;
    }

    public String getEvenVariable() {
        return evenVariable;
    }

    public String getOddVariable() {
        return oddVariable;
    }

    public String getLastVariable() {
        return lastVariable;
    }

}
