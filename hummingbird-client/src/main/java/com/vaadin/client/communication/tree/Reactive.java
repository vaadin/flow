package com.vaadin.client.communication.tree;

import java.util.Collection;
import java.util.HashSet;

public class Reactive {

    private static Collection<TreeNodeProperty> collector;

    public static Collection<TreeNodeProperty> collectAccessedProperies(
            Runnable runnable) {
        Collection<TreeNodeProperty> previousCollector = collector;
        Collection<TreeNodeProperty> ownCollector = new HashSet<>();
        collector = ownCollector;

        try {
            runnable.run();
        } finally {
            if (previousCollector != null) {
                previousCollector.addAll(ownCollector);
            }
            collector = previousCollector;
        }
        return ownCollector;
    }

    public static void setAccessed(TreeNodeProperty treeNodeProperty) {
        if (collector != null
                && !(treeNodeProperty instanceof ComputedTreeNodeProperty)) {
            collector.add(treeNodeProperty);
        }
    }
}
