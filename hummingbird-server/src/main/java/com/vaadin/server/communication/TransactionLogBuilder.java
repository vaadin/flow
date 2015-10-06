package com.vaadin.server.communication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.vaadin.hummingbird.kernel.AbstractElementTemplate;
import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.NodeChangeVisitor;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;

public class TransactionLogBuilder {

    /**
     * List of changes. This assumes that {@link NodeChangeVisitor} visits the
     * nodes one by one so the order of nodes in this map is in the order the
     * changes should be sent
     */
    private LinkedHashMap<StateNode, List<NodeChange>> changes = new LinkedHashMap<>();
    private Set<ElementTemplate> templates = new HashSet<>();
    private NodeVisitor visitor;

    public TransactionLogBuilder() {
        visitor = new NodeVisitor(this);
    }

    private void addChange(StateNode node, NodeChange change) {
        changes.putIfAbsent(node, new ArrayList<>());
        changes.get(node).add(change);
    }

    private void handleTemplate(Object value) {
        if (value instanceof ElementTemplate) {
            if (templates.contains(value)) {
                return;
            }

            templates.add((ElementTemplate) value);
            if (value instanceof BoundElementTemplate) {
                for (BoundElementTemplate child : ((BoundElementTemplate) value)
                        .getChildTemplates()) {
                    handleTemplate(child);
                }
            }
        }
    }

    public NodeVisitor getVisitor() {
        return visitor;
    }

    public LinkedHashMap<StateNode, List<NodeChange>> getChanges() {
        return changes;
    }

    public Set<ElementTemplate> getTemplates() {
        return templates;
    }

    private static boolean isServerOnly(StateNode node) {
        if (node == null) {
            return false;
        } else if (node.containsKey(AbstractElementTemplate.Keys.SERVER_ONLY)) {
            return true;
        } else {
            return isServerOnly(node.getParent());
        }
    }

    private static boolean isServerOnlyKey(Object key) {
        if (key == null) {
            return false;
        } else if (key instanceof Class) {
            return true;
        } else {
            return false;
        }
    }

    public static class NodeVisitor implements NodeChangeVisitor {

        private TransactionLogBuilder logBuilder;

        public NodeVisitor(TransactionLogBuilder logBuilder) {
            this.logBuilder = logBuilder;
        }

        @Override
        public void visitRemoveChange(StateNode node,
                RemoveChange removeChange) {
            if (isServerOnly(node)) {
                return;
            }
            if (removeChange.getValue() instanceof StateNode
                    && isServerOnly((StateNode) removeChange.getValue())) {
                return;
            }
            Object key = removeChange.getKey();
            if (isServerOnlyKey(key)) {
                return;
            }

            logBuilder.addChange(node, removeChange);
        }

        @Override
        public void visitPutChange(StateNode node, PutChange putChange) {
            if (isServerOnly(node)) {
                return;
            }
            Object key = putChange.getKey();
            if (isServerOnlyKey(key)) {
                return;
            }

            Object value = putChange.getValue();
            if (value instanceof StateNode) {
                StateNode childNode = (StateNode) value;
                if (isServerOnly(childNode)) {
                    return;
                }
                logBuilder.addChange(node, putChange);
                logBuilder.handleTemplate(key);
            } else {
                logBuilder.addChange(node, putChange);
                logBuilder.handleTemplate(value);
            }
            assert key instanceof String || key instanceof Enum
                    || key instanceof Integer : "key type "
                            + key.getClass().getName() + " not supported";

        }

        @Override
        public void visitParentChange(StateNode node,
                ParentChange parentChange) {
            // Ignore
        }

        @Override
        public void visitIdChange(StateNode node, IdChange idChange) {
            // Ignore
        }

        @Override
        public void visitListInsertChange(StateNode node,
                ListInsertChange listInsertChange) {
            if (isServerOnly(node)) {
                return;
            }
            Object key = listInsertChange.getKey();
            if (isServerOnlyKey(key)) {
                return;
            }

            assert isValidListKey(key);
            logBuilder.addChange(node, listInsertChange);
        }

        @Override
        public void visitListRemoveChange(StateNode node,
                ListRemoveChange listRemoveChange) {
            if (isServerOnly(node)) {
                return;
            }
            Object key = listRemoveChange.getKey();
            if (isServerOnlyKey(key)) {
                return;
            }
            Object removedValue = listRemoveChange.getValue();
            if (removedValue instanceof StateNode) {
                if (isServerOnly(((StateNode) removedValue))) {
                    return;
                }
            }

            assert isValidListKey(key);
            logBuilder.addChange(node, listRemoveChange);
        }

        private boolean isValidListKey(Object key) {
            return key instanceof String || key instanceof Enum;
        }

        @Override
        public void visitListReplaceChange(StateNode node,
                ListReplaceChange listReplaceChange) {
            if (isServerOnly(node)) {
                return;
            }
            Object key = listReplaceChange.getKey();
            if (isServerOnlyKey(key)) {
                return;
            }

            assert isValidListKey(key);
            logBuilder.addChange(node, listReplaceChange);
        }
    }

}
