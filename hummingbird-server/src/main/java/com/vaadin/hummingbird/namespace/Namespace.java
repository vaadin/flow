package com.vaadin.hummingbird.namespace;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.NodeChange;

public abstract class Namespace implements Serializable {
    private static Map<Class<? extends Namespace>, Function<StateNode, Namespace>> namespaceFactoies = new HashMap<>();

    static {
        namespaceFactoies.put(ElementDataNamespace.class,
                ElementDataNamespace::new);
        namespaceFactoies.put(ElementPropertiesNamespace.class,
                ElementPropertiesNamespace::new);
        namespaceFactoies.put(ElementChildrenNamespace.class,
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
        assert namespaceType != null;
        assert node != null;

        Function<StateNode, Namespace> factory = namespaceFactoies
                .get(namespaceType);
        assert factory != null;

        return factory.apply(node);
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
}
