package com.vaadin.hummingbird.kernel;

import java.util.Collection;

public interface ElementTemplate {

    public String getTag(StateNode node);

    public void setAttribute(String name, String value, StateNode node);

    public String getAttribute(String name, StateNode node);

    public int getChildCount(StateNode node);

    public Element getChild(int index, StateNode node);

    public void insertChild(int index, Element child, StateNode node);

    public Element getParent(StateNode node);

    public void removeChild(StateNode node, Element element);

    public boolean supports(StateNode node);

    public Collection<String> getAttributeNames(StateNode node);

    public abstract int getId();

    public void addListener(String type, EventListener listener, StateNode node);

    public void removeListener(String type, EventListener listener, StateNode node);
}
