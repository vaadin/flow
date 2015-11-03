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
import com.vaadin.hummingbird.kernel.change.ListInsertManyChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.NodeListChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RangeEndChange;
import com.vaadin.hummingbird.kernel.change.RangeStartChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;
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
        assert !hasDetachedNodes(
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

        perNodeOptimizations(changes);

        removeDetachedNodes(changes);

        return changes;
    }

    private void perNodeOptimizations(
            LinkedHashMap<StateNode, List<NodeChange>> changes) {
        for (StateNode node : changes.keySet()) {
            List<NodeChange> nodeChanges = changes.get(node);
            removeAddedAndRemovedListItems(node, nodeChanges);
            removeDuplicatePuts(node, nodeChanges);
            joinListInserts(node, nodeChanges);
            keepOnlyLastRangeChange(node, nodeChanges);
        }

    }

    /**
     * Combines subsequent list inserts to one insert with many values
     *
     * @param node
     * @param list
     */
    private void joinListInserts(StateNode node, List<NodeChange> nodeChanges) {
        Map<Object, Integer> insertsInProgress = new HashMap<>();

        for (int changeIndex = 0; changeIndex < nodeChanges
                .size(); changeIndex++) {
            NodeChange change = nodeChanges.get(changeIndex);
            if (change instanceof ListInsertChange) {
                ListInsertChange lic = (ListInsertChange) change;
                Object key = lic.getKey();

                if (!insertsInProgress.containsKey(key)) {
                    // First insert for this key
                    insertsInProgress.put(key, changeIndex);
                } else {
                    // Subsequent insert

                    int firstChangeIndex = insertsInProgress.get(key);
                    NodeChange firstChange = nodeChanges.get(firstChangeIndex);
                    int lastInsertIndex;
                    if (firstChange instanceof ListInsertChange) {
                        int firstInsertIndex = ((ListInsertChange) firstChange)
                                .getIndex();
                        if (lic.getIndex() == firstInsertIndex + 1) {
                            // Replace insert change with a multiple value
                            // insert change
                            ListInsertManyChange listInsertManyChange = new ListInsertManyChange(
                                    firstInsertIndex, key,
                                    ((ListInsertChange) firstChange)
                                            .getValue());
                            listInsertManyChange.addValue(lic.getValue());

                            nodeChanges.set(firstChangeIndex,
                                    listInsertManyChange);

                            // Remove current change
                            nodeChanges.remove(changeIndex);
                            changeIndex--;
                        } else {
                            // Not an insert for a subsequent index
                            insertsInProgress.remove(key);
                            continue;
                        }
                    } else {
                        ListInsertManyChange insertManyChange = (ListInsertManyChange) firstChange;
                        lastInsertIndex = insertManyChange.getIndex()
                                + insertManyChange.getValue().size() - 1;
                        if (lic.getIndex() == lastInsertIndex + 1) {
                            // Insert following the last one -> merge
                            insertManyChange.addValue(lic.getValue());
                            // Remove current change
                            nodeChanges.remove(changeIndex);
                            changeIndex--;
                        } else {
                            // Not an insert for a subsequent index
                            insertsInProgress.remove(key);
                            continue;
                        }
                    }

                }
            } else if (change instanceof ListRemoveChange) {
                insertsInProgress.remove(((ListRemoveChange) change).getKey());
            }
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

    /**
     * Optimizes
     *
     * put "foo=bar" remove "foo" put "foo=foo"
     *
     * into
     *
     * put "foo=foo"
     *
     * @param node
     * @param list
     */
    private void removeDuplicatePuts(StateNode node, List<NodeChange> list) {
        Map<Object, Object> finalValues = new HashMap<>();

        for (int i = list.size() - 1; i >= 0; i--) {
            NodeChange change = list.get(i);

            if (change instanceof PutChange) {
                PutChange pc = (PutChange) change;
                if (!finalValues.containsKey(pc.getKey())) {
                    // This is the last put/remove for the key
                    finalValues.put(pc.getKey(), pc.getValue());
                } else {
                    // There has been a later put/remove for the same key - skip
                    // this
                    list.remove(i);
                }
            } else if (change instanceof RemoveChange) {
                RemoveChange rc = (RemoveChange) change;
                if (!finalValues.containsKey(rc.getKey())) {
                    // The key should be removed in the end

                    // If we knew that this was a new key, we could skip the
                    // change but we do not
                    finalValues.put(rc.getKey(), null);
                } else {
                    // There has been a later put/remove for the same key - skip
                    // this
                    list.remove(i);
                }

            }

        }
    }

    private void removeDetachedNodes(
            LinkedHashMap<StateNode, List<NodeChange>> changes) {
        changes.keySet().removeIf(n -> !n.isAttached());
    }

    private void keepOnlyLastRangeChange(StateNode node,
            List<NodeChange> nodeChanges) {
        Set<Object> rangeStarts = new HashSet<>();
        Set<Object> rangeEnds = new HashSet<>();
        for (int i = nodeChanges.size() - 1; i >= 0; i--) {
            NodeChange change = nodeChanges.get(i);

            if (change instanceof RangeStartChange) {
                RangeStartChange rangeChange = (RangeStartChange) change;
                Object key = rangeChange.getKey();
                if (rangeStarts.contains(key)) {
                    // Earlier start -> not needed
                    nodeChanges.remove(i);
                } else {
                    rangeStarts.add(key);
                }
            } else if (change instanceof RangeEndChange) {
                RangeEndChange rangeChange = (RangeEndChange) change;
                Object key = rangeChange.getKey();
                if (rangeEnds.contains(key)) {
                    // Earlier start -> not needed
                    nodeChanges.remove(i);
                } else {
                    rangeEnds.add(key);
                }
            }
        }

    }

}
