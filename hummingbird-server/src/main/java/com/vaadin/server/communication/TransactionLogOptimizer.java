package com.vaadin.server.communication;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.NodeListChange;
import com.vaadin.ui.UI;

public class TransactionLogOptimizer {

    private UI ui;
    private LinkedHashMap<StateNode, List<NodeChange>> changes;
    private Set<ElementTemplate> templates;

    public TransactionLogOptimizer(UI ui,
            LinkedHashMap<StateNode, List<NodeChange>> changes,
            Set<ElementTemplate> templates) {
        this.ui = ui;
        this.changes = optimizeChanges(changes);
        assert!hasDetachedNodes(
                this.changes) : "There must be no detached nodes in the optimized list";
        this.templates = optimizeTemplates(templates);
    }

    private boolean hasDetachedNodes(
            LinkedHashMap<StateNode, List<NodeChange>> changes) {
        for (StateNode n : changes.keySet()) {
            if (!n.isAttached()) {
                return true;
            }
        }

        return false;

    }

    public Set<ElementTemplate> getTemplates() {
        return templates;
    }

    public LinkedHashMap<StateNode, List<NodeChange>> getChanges() {
        return changes;
    }

    private Set<ElementTemplate> optimizeTemplates(
            Set<ElementTemplate> templates) {
        HashSet<ElementTemplate> newTemplates = new HashSet<>();
        for (ElementTemplate t : templates) {
            if (!ui.knowsTemplate(t)) {
                newTemplates.add(t);
            }
        }
        return newTemplates;
    }

    private LinkedHashMap<StateNode, List<NodeChange>> optimizeChanges(
            LinkedHashMap<StateNode, List<NodeChange>> changes) {

        removeAddedAndRemovedListItems(changes);

        removeDetachedNodes(changes);

        return changes;
    }

    private void removeAddedAndRemovedListItems(
            LinkedHashMap<StateNode, List<NodeChange>> changes) {
        for (StateNode node : changes.keySet()) {
            removeAddedAndRemovedListItems(node, changes.get(node));
        }

    }

    private void removeAddedAndRemovedListItems(StateNode node,
            List<NodeChange> list) {
        Map<Object, Integer> removedValues = new HashMap<>();

        for (int i = list.size() - 1; i >= 0; i--) {
            NodeChange change = list.get(i);
            if (change instanceof ListRemoveChange) {
                Object removedValue = ((ListRemoveChange) change).getValue();
                removedValues.put(removedValue, i);
            } else if (change instanceof ListInsertChange) {
                Object addedValue = ((ListInsertChange) change).getValue();
                if (removedValues.containsKey(addedValue)) {
                    // This was added and removed so there is no point in adding
                    // it at all
                    int removeChangeIndex = removedValues.remove(addedValue);
                    int addChangeIndex = i;

                    // Need to fix adds/replaces/removes between the add and
                    // remove

                    for (int j = addChangeIndex
                            + 1; j < removeChangeIndex; j++) {
                        NodeChange c = list.get(j);
                        if (c instanceof NodeListChange) {
                            NodeListChange nlc = (NodeListChange) list.get(j);
                            if (nlc.getIndex() > addChangeIndex) {
                                // Add/remove/replace after insert index?
                                // Decrement
                                nlc.setIndex(nlc.getIndex() - 1);
                            }
                        }
                    }

                    // The previously found removes are also off by one
                    for (Object value : removedValues.keySet()) {
                        Integer index = removedValues.get(value);
                        if (index > addChangeIndex) {
                            removedValues.put(value, index - 1);
                        }
                    }
                    list.remove(addChangeIndex);
                    list.remove(removeChangeIndex - 1); // -1 because add is
                                                        // already removed

                }
            }
        }

    }

    private void removeDetachedNodes(
            LinkedHashMap<StateNode, List<NodeChange>> changes) {
        changes.keySet().removeIf(n -> !n.isAttached());
    }

}
