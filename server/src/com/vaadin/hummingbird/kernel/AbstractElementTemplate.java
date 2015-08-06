package com.vaadin.hummingbird.kernel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractElementTemplate implements ElementTemplate {

    public enum Keys {
        TEMPLATE, TAG, PARENT_TEMPLATE, CHILDREN;
    }

    private static final AtomicInteger nextId = new AtomicInteger();

    private int id;

    public AbstractElementTemplate() {
        this.id = nextId.incrementAndGet();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getAttribute(String name, StateNode node) {
        StateNode elementDataNode = getElementDataNode(node, false);
        if (elementDataNode == null) {
            return null;
        } else {
            return elementDataNode.get(name, String.class);
        }
    }

    @Override
    public void setAttribute(String name, String value, StateNode node) {
        if (value == null) {
            StateNode elementDataNode = getElementDataNode(node, false);
            if (elementDataNode != null) {
                elementDataNode.remove(name);
            }
        } else {
            StateNode elementDataNode = getElementDataNode(node, true);
            elementDataNode.put(name, value);
        }
    }

    @Override
    public int getChildCount(StateNode node) {
        return getChildrenList(node).map(List::size).orElse(Integer.valueOf(0))
                .intValue();
    }

    protected abstract StateNode getElementDataNode(StateNode node,
            boolean createIfNeeded);

    private Optional<List<Object>> getChildrenList(StateNode node) {
        return Optional.ofNullable(getChildrenList(node, false));
    }

    private List<Object> getOrCreateChildrenList(StateNode node) {
        return getChildrenList(node, true);
    }

    private List<Object> getChildrenList(StateNode node, boolean create) {
        StateNode elementDataNode = getElementDataNode(node, create);
        if (elementDataNode == null) {
            return null;
        } else {
            return elementDataNode.getMultiValued(Keys.CHILDREN);
        }
    }

    @Override
    public Element getChild(int index, StateNode node) {
        StateNode childState = (StateNode) getChildrenList(node).orElseThrow(
                IndexOutOfBoundsException::new).get(index);

        ElementTemplate childTemplate = childState.get(Keys.TEMPLATE,
                ElementTemplate.class);
        if (childTemplate == null) {
            childTemplate = BasicElementTemplate.get();
        }
        return Element.getElement(childTemplate, childState);
    }

    @Override
    public void insertChild(int index, Element child, StateNode node) {
        child.removeFromParent();

        StateNode childNode = child.getNode();
        getOrCreateChildrenList(node).add(index, childNode);

        ElementTemplate template = child.getTemplate();
        if (!(template instanceof BasicElementTemplate)) {
            childNode.put(Keys.TEMPLATE, template);
        }
        if (!(this instanceof BasicElementTemplate)) {
            childNode.put(Keys.PARENT_TEMPLATE, this);
        }
    }

    @Override
    public Element getParent(StateNode node) {
        StateNode parentNode = node.getParent();
        if (parentNode == null) {
            return null;
        }

        ElementTemplate parentTemplate = node.get(Keys.PARENT_TEMPLATE,
                ElementTemplate.class);
        if (parentTemplate == null) {
            parentTemplate = BasicElementTemplate.get();
        } else {
            parentNode = parentNode.getParent();
            assert parentNode != null;
        }

        return Element.getElement(parentTemplate, parentNode);
    }

    @Override
    public void removeChild(StateNode node, Element element) {
        StateNode childNode = element.getNode();

        getChildrenList(node).ifPresent(list -> {
            if (list.remove(childNode)) {
                childNode.remove(Keys.TEMPLATE);
                childNode.remove(Keys.PARENT_TEMPLATE);
            }
        });
    }

    @Override
    public Collection<String> getAttributeNames(StateNode node) {
        StateNode dataNode = getElementDataNode(node, false);
        if (dataNode == null) {
            return Collections.emptySet();
        } else {
            return dataNode.getStringKeys();
        }
    }
}
