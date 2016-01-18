package com.vaadin.hummingbird.kernel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.vaadin.ui.Component;

public interface ElementTemplate extends Serializable {

    public String getTag(StateNode node);

    public String getIs(StateNode node);

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

    public void addEventListener(String type, DomEventListener listener,
            StateNode node);

    public void removeEventListener(String type, DomEventListener listener,
            StateNode node);

    /**
     * Returns the event listeners registered for the given event type.
     * <p>
     * Returns an empty collection if there are no listeners registered
     *
     * @param eventType
     * @param node
     * @return a collection with the registered event listeners.
     */
    public Collection<DomEventListener> getEventListeners(String eventType,
            StateNode node);

    public List<Component> getComponents(StateNode node,
            boolean createIfNeeded);

    public void addEventData(String type, StateNode node, String[] data);

    public void removeEventData(String type, StateNode node, String[] data);

    public Collection<String> getEventData(String eventType, StateNode node);

    public void runBeforeNextClientResponse(Runnable runnable, StateNode node);

    public StateNode getElementDataNode(StateNode node, boolean createIfNeeded);

    /**
     * Returns the manually added classes. Can be used for adding & removing new
     * classes. For {@link BoundElementTemplate}, this will not contain the
     * classes coming from the template.
     *
     * @param node
     * @param createIfNeeded
     *            <code>true</code> for creating the necessary state node (when
     *            writing)
     * @return list of the manually added classes for this template
     */
    public List<String> getClassList(StateNode node, boolean createIfNeeded);

    /**
     * Returns a <b>READ-ONLY</b> list with the classes for the state node. The
     * list will include the manually added classes (with server side API), and
     * the classes coming from the template (in case of
     * {@link BoundElementTemplate}).
     *
     * @param node
     * @return an unmodifiable list of classes for the node
     */
    public List<String> getAllClasses(StateNode node);

}
