package com.vaadin.hummingbird.kernel;

import java.util.Collection;

public class Element {

    private ElementTemplate template;
    private StateNode node;

    public Element(String tag) {
        this(BasicElementTemplate.get(), BasicElementTemplate.createBasicElementModel(tag));
    }

    private Element(ElementTemplate template, StateNode node) {
        // Private constructor to force using the static getter that might
        // enable caching at some point
        assert template.supports(node);
        this.template = template;
        this.node = node;
    }

    public String getTag() {
        return template.getTag(node);
    }

    public ElementTemplate getTemplate() {
        return template;
    }

    public StateNode getNode() {
        return node;
    }

    public void setAttribute(String name, String value) {
        assert validAttribute(name);
        template.setAttribute(name, value, node);
    }

    private boolean validAttribute(String name) {
        if ("#text".equals(getTag())) {
            assert "content".equals(name) : "Attribute " + name + " is not supported for text nodes";
        }
        return true;
    }

    public void setAttribute(String name, boolean value) {
        assert validAttribute(name);
        template.setAttribute(name, Boolean.toString(value), node);
    }

    public String getAttribute(String name) {
        return template.getAttribute(name, node);
    }

    public void addEventListener(String type, EventListener listener) {
        template.addListener(type, listener, node);
    }

    public void removeEventListener(String type, EventListener listener) {
        template.removeListener(type, listener, node);
    }

    public int getChildCount() {
        return template.getChildCount(node);
    }

    public Element getChild(int index) {
        return template.getChild(index, node);
    }

    public void insertChild(int index, Element child) {
        template.insertChild(index, child, node);
    }

    public void removeFromParent() {
        Element parent = getParent();
        if (parent != null) {
            parent.template.removeChild(parent.getNode(), this);
        }
    }

    public Element getParent() {
        return template.getParent(node);
    }

    public static Element getElement(ElementTemplate template, StateNode node) {
        return new Element(template, node);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        toString(b);
        return b.toString();
    }

    public Collection<String> getAttributeNames() {
        return template.getAttributeNames(node);
    }

    public void toString(StringBuilder b) {
        String tag = getTag();
        if ("#text".equals(tag)) {
            String content = getAttribute("content");
            if (content != null) {
                b.append(content);
            }
        } else {
            b.append('<');
            b.append(tag);
            for (String attribute : getAttributeNames()) {
                String value = getAttribute(attribute);
                if (value != null) {
                    b.append(' ');
                    b.append(attribute);
                    b.append("=\"");
                    b.append(value);
                    b.append('\"');
                }
            }
            b.append('>');

            for (int i = 0; i < getChildCount(); i++) {
                getChild(i).toString(b);
            }
            b.append("</");
            b.append(tag);
            b.append('>');
        }
    }

    public static Element createText(String content) {
        Element element = new Element("#text");
        element.setAttribute("content", content);
        return element;
    }

    public void removeAllChildren() {
        while (getChildCount() > 0) {
            getChild(0).removeFromParent();
        }

    }
}
