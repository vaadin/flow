package com.vaadin.server.communication;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.ui.UI;

public class TransactionLogPruner {

    public static LinkedHashMap<StateNode, List<NodeChange>> prune(
            LinkedHashMap<StateNode, List<NodeChange>> changes, UI ui) {
        Map<Integer, Set<NodeChange>> ignoreChanges = ui.getIgnoreChanges();

        Iterator<StateNode> iterator = changes.keySet().iterator();
        while (iterator.hasNext()) {
            StateNode node = iterator.next();
            List<NodeChange> nodeChanges = changes.get(node);
            Set<NodeChange> nodeIgnores = ignoreChanges.get(node.getId());
            if (nodeIgnores != null) {
                nodeChanges.removeAll(nodeIgnores);
                if (nodeChanges.isEmpty()) {
                    iterator.remove();
                }
            }
        }
        ignoreChanges.clear();

        return changes;
    }

}
