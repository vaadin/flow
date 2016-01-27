package com.vaadin.hummingbird.namespace;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.NodeChange;

public abstract class AbstractNamespaceTest<T extends Namespace> {

    public T createNamespace() {
        Class<T> namespaceType = findNamespaceType();

        return createNamespace(namespaceType);
    }

    public static <T extends Namespace> T createNamespace(
            Class<T> namespaceType) {
        StateNode node = new StateNode(Collections.singleton(namespaceType));

        return node.getNamespace(namespaceType);
    }

    @SuppressWarnings("unchecked")
    private Class<T> findNamespaceType() {
        ParameterizedType genericSuperclass = (ParameterizedType) getClass()
                .getGenericSuperclass();

        Class<?> paramType = (Class<?>) genericSuperclass
                .getActualTypeArguments()[0];

        Class<? extends Namespace> namespaceType = paramType
                .asSubclass(Namespace.class);

        return (Class<T>) namespaceType;
    }

    public List<NodeChange> collectChanges(Namespace namespace) {
        List<NodeChange> changes = new ArrayList<>();

        namespace.collectChanges(changes::add);

        return changes;
    }
}
