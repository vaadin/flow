package com.vaadin.hummingbird.kernel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI.Root;

public abstract class AbstractElementTemplate implements ElementTemplate {

    public enum Keys {
        TEMPLATE, TAG, PARENT_TEMPLATE, CHILDREN, LISTENERS, SERVER_ONLY, EVENT_DATA;
    }

    private static final AtomicInteger nextId = new AtomicInteger(1);

    private int id;

    public AbstractElementTemplate() {
        if (getClass() == BasicElementTemplate.class) {
            id = 0;
        } else {
            id = nextId.incrementAndGet();
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Object getAttribute(String name, StateNode node) {
        StateNode elementDataNode = getElementDataNode(node, false);
        if (elementDataNode == null) {
            return null;
        } else {
            return elementDataNode.get(name, Object.class);
        }
    }

    @Override
    public void setAttribute(String name, Object value, StateNode node) {
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
    public void addEventListener(String type, DomEventListener listener,
            StateNode node) {
        StateNode listeners = getListenerNode(node, true);

        List<Object> typeListeners = listeners.getMultiValued(type);
        if (typeListeners.isEmpty()) {
            // List of listened types going to the client
            getElementDataNode(node, true).getMultiValued(Keys.LISTENERS)
                    .add(type);
        }
        // The listener instance list staying on the server
        typeListeners.add(listener);
    }

    @Override
    public void addEventData(String type, StateNode node, String[] data) {
        StateNode elementData = getElementDataNode(node, true);
        StateNode eventDataNode = elementData.get(Keys.EVENT_DATA,
                StateNode.class);
        if (eventDataNode == null) {
            eventDataNode = StateNode.create();
            elementData.put(Keys.EVENT_DATA, eventDataNode);
        }

        List<Object> eventData = eventDataNode.getMultiValued(type);
        for (String d : data) {
            if (!eventData.contains(d)) {
                eventData.add(d);
            }
        }
    }

    @Override
    public Collection<String> getEventData(String type, StateNode node) {
        StateNode elementData = getElementDataNode(node, true);
        StateNode eventDataNode = elementData.get(Keys.EVENT_DATA,
                StateNode.class);
        if (eventDataNode == null) {
            return Collections.emptySet();
        }

        Collection<String> eventData = new HashSet<>(
                (List) eventDataNode.getMultiValued(type));
        return eventData;
    }

    private StateNode getListenerNode(StateNode node, boolean createIfNeeded) {
        StateNode dataNode = getElementDataNode(node, createIfNeeded);
        StateNode listeners = dataNode.get(DomEventListener.class,
                StateNode.class);
        if (listeners == null) {
            listeners = StateNode.create();
            listeners.put(Keys.SERVER_ONLY, Keys.SERVER_ONLY);
            dataNode.put(DomEventListener.class, listeners);
        }
        return listeners;
    }

    @Override
    public void removeEventListener(String type, DomEventListener listener,
            StateNode node) {
        StateNode listenerNode = getListenerNode(node, false);
        if (listenerNode == null) {
            return;
        }
        if (!listenerNode.containsKey(type)) {
            return;
        }

        List<Object> listeners = listenerNode.getMultiValued(type);
        if (listeners.remove(listener)) {
            if (listeners.isEmpty()) {
                listenerNode.remove(type);
                StateNode elementDataNode = getElementDataNode(node, true);

                elementDataNode.getMultiValued(Keys.LISTENERS).remove(type);

                if (listenerNode.getStringKeys().isEmpty()) {
                    elementDataNode.remove(Keys.LISTENERS);
                    elementDataNode.remove(DomEventListener.class);
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Collection<DomEventListener> getEventListeners(String type,
            StateNode node) {
        StateNode listenerNode = getListenerNode(node, false);
        if (listenerNode == null) {
            return Collections.emptyList();
        }
        if (!listenerNode.containsKey(type)) {
            return Collections.emptyList();
        }

        List<Object> listeners = listenerNode.getMultiValued(type);
        return (Collection) Collections.unmodifiableList(listeners);
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
        StateNode childState = (StateNode) getChildrenList(node)
                .orElseThrow(IndexOutOfBoundsException::new).get(index);

        ElementTemplate childTemplate = childState.get(Keys.TEMPLATE,
                ElementTemplate.class);
        if (childTemplate == null) {
            childTemplate = BasicElementTemplate.get();
        }
        return Element.getElement(childTemplate, childState);
    }

    @Override
    public void insertChild(int index, Element child, StateNode node) {
        assert child != null : "Cannot insert null child";

        // Ensure that this element is not a child of the child element
        if (node.hasAncestor(child.getNode())) {
            throw new IllegalArgumentException(
                    "Cannot add node inside its own children");
        }

        if (child.getParent() != null) {
            if (child.getParent().getNode() == node
                    && child.getParent().getTemplate() == this) {
                // Adjust index if child is a child of this
                int currentIndex = child.getParent().getChildIndex(child);
                if (currentIndex == index) {
                    // Already at the correct position
                    return;
                }
                if (index > currentIndex) {
                    index--;
                }
            }
            child.removeFromParent();
        }

        StateNode childNode = child.getNode();
        getOrCreateChildrenList(node).add(index, childNode);

        ElementTemplate template = child.getTemplate();
        if (!(template instanceof BasicElementTemplate)) {
            childNode.put(Keys.TEMPLATE, template);
        }
        if (!(this instanceof BasicElementTemplate)) {
            childNode.put(Keys.PARENT_TEMPLATE, this);
        }

        List<Component> components = child.getTemplate()
                .getComponents(childNode, false);
        for (int i = components.size() - 1; i >= 0; i--) {
            // Parent before child
            components.get(i).elementAttached();
        }
    }

    @Override
    public Element getParent(StateNode node) {
        StateNode parentNode = node.getParent();
        if (parentNode == null || parentNode == node.getRoot()) {
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
        assert sessionLocked(node, element);
        StateNode childNode = element.getNode();

        getChildrenList(node).ifPresent(list -> {
            // Detach event while still attached to the DOM
            List<Component> components = element.getTemplate()
                    .getComponents(childNode, false);
            for (int i = components.size() - 1; i >= 0; i--) {
                // Parent before child
                components.get(i).elementDetached();
            }

            if (list.remove(childNode)) {
                childNode.remove(Keys.TEMPLATE);
                childNode.remove(Keys.PARENT_TEMPLATE);

            }
        });

    }

    private boolean sessionLocked(StateNode node, Element element) {
        // Verify the appropriate session is locked
        RootNode root = node.getRoot();
        if (root == null) {
            return true;
        }
        StateNode bodyNode = root.get("containerElement", StateNode.class);

        Root r = (Root) getComponents(bodyNode, false).get(0);
        if (r != null) {
            VaadinSession parentSession = r.getUI().getSession();
            if (parentSession != null && !parentSession.hasLock()) {
                String message = "Cannot remove from parent when the session is not locked.";
                if (VaadinService.isOtherSessionLocked(parentSession)) {
                    message += " Furthermore, there is another locked session, indicating that the component might be about to be moved from one session to another.";
                }
                throw new IllegalStateException(message);
            }
        }

        return true;
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<Component> getComponents(StateNode node,
            boolean createIfNeeded) {
        StateNode dataNode = getElementDataNode(node, createIfNeeded);
        if (dataNode == null
                || !dataNode.containsKey(Component.class) && !createIfNeeded) {
            return Collections.emptyList();
        }

        return (List) dataNode.getMultiValued(Component.class);
    }

    // Class used as a key to ensure the value is never sent to the client
    private static class RunBeforeClientResponseKey {
        // This class has intentionally been left empty
    }

    @Override
    public void runBeforeNextClientResponse(Runnable runnable, StateNode node) {
        StateNode dataNode = getElementDataNode(node, true);
        @SuppressWarnings("unchecked")
        Set<Runnable> pendingRunnables = dataNode
                .get(RunBeforeClientResponseKey.class, Set.class);
        if (pendingRunnables == null) {
            pendingRunnables = new HashSet<>();
            dataNode.put(RunBeforeClientResponseKey.class, pendingRunnables);
            dataNode.addChangeListener(new NodeChangeListener() {
                @Override
                public void onChange(StateNode stateNode,
                        List<NodeChange> changes) {
                    dataNode.removeChangeListener(this);

                    @SuppressWarnings("unchecked")
                    Set<Runnable> pendingRunnables = (Set<Runnable>) dataNode
                            .remove(RunBeforeClientResponseKey.class);
                    assert pendingRunnables != null;

                    pendingRunnables.forEach(Runnable::run);
                }
            });
        }
        pendingRunnables.add(runnable);
    }

}
