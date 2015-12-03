package com.vaadin.server.communication;

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

        changes.forEach((node, nodeChanges) -> {
            Set<NodeChange> nodeIgnores = ignoreChanges.get(node.getId());
            if (nodeIgnores != null) {
                nodeChanges.removeAll(nodeIgnores);
            }
        });

        ignoreChanges.clear();

        return changes;
    }

}
