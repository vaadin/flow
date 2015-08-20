package com.vaadin.hummingbird.kernel;

import java.util.Collection;

import com.vaadin.ui.Component;

public interface ElementTemplate {

    public String getTag(StateNode node);

    public void setAttribute(String name, Object value, StateNode node);

    public Object getAttribute(String name, StateNode node);

    public int getChildCount(StateNode node);

    public Element getChild(int index, StateNode node);

    public void insertChild(int index, Element child, StateNode node);

    public Element getParent(StateNode node);

    public void removeChild(StateNode node, Element element);

    public boolean supports(StateNode node);

    public Collection<String> getAttributeNames(StateNode node);

    public abstract int getId();

    public void addEventListener(String type, EventListener listener,
            StateNode node);

    public void removeEventListener(String type, EventListener listener,
            StateNode node);

    public Collection<EventListener> getEventListeners(String eventType,
            StateNode node);

    public void setComponent(Component c, StateNode node);

    public Component getComponent(StateNode node);

    public void addEventData(String type, StateNode node, String[] data);

}
