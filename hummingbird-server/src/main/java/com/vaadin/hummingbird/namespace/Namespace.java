package com.vaadin.hummingbird.namespace;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.NodeChange;

public abstract class Namespace implements Serializable {
    private static int nextNamespaceId = 0;

    // Non-private for testing purposes
    static final Map<Class<? extends Namespace>, NamespaceData> namespaces = new HashMap<>();

    private static class NamespaceData implements Serializable {
        private Function<StateNode, ? extends Namespace> factory;
        private int id = nextNamespaceId++;

        public <T extends Namespace> NamespaceData(Class<T> type,
                Function<StateNode, T> factory) {
            this.factory = factory;
        }
    }

    private static <T extends Namespace> void registerNamespace(Class<T> type,
            Function<StateNode, T> factory) {
        namespaces.put(type, new NamespaceData(type, factory));
    }

    static {
        registerNamespace(ElementDataNamespace.class,
                ElementDataNamespace::new);
        registerNamespace(ElementPropertiesNamespace.class,
                ElementPropertiesNamespace::new);
        registerNamespace(ElementChildrenNamespace.class,
                ElementChildrenNamespace::new);
    }

    private final StateNode node;

    public Namespace(StateNode node) {
        this.node = node;
    }

    public StateNode getNode() {
        return node;
    }

    public static Namespace create(Class<? extends Namespace> namespaceType,
            StateNode node) {
        assert node != null;

        return getData(namespaceType).factory.apply(node);
    }

    public abstract void collectChanges(Consumer<NodeChange> collector);

    public abstract void resetChanges();

    protected void attachPotentialChild(Object child) {
        if (child instanceof StateNode) {
            StateNode childNode = (StateNode) child;
            childNode.setParent(getNode());
        }
    }

    protected void detatchPotentialChild(Object child) {
        if (child instanceof StateNode) {
            StateNode childNode = (StateNode) child;
            childNode.setParent(null);
        }
    }

    public static int getId(Class<? extends Namespace> namespace) {
        return getData(namespace).id;
    }

    private static NamespaceData getData(Class<? extends Namespace> namespace) {
        assert namespace != null;

        NamespaceData data = namespaces.get(namespace);

        assert data != null;

        return data;
    }
}
