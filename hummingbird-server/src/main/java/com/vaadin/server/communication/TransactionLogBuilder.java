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
import com.vaadin.hummingbird.kernel.change.NodeContentsChange;
import com.vaadin.hummingbird.kernel.change.NodeDataChange;
import com.vaadin.hummingbird.kernel.change.NodeListChange;
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
        if (isServerOnly(node)) {
            return;
        }

        Object key = null;
        if (change instanceof NodeContentsChange) {
            key = ((NodeContentsChange) change).getKey();
        }

        if (key != null) {
            if (isServerOnlyKey(key)) {
                return;
            }
            // Key types for server only values is not restricted, key types
            // going to the client are restricted
            assert key instanceof String || key instanceof Enum
                    || key instanceof Integer
                    || key instanceof ElementTemplate : "key type "
                            + key.getClass().getName() + " not supported";

            handleTemplate(key);
        }

        Object value = null;
        if (change instanceof NodeDataChange) {
            value = ((NodeDataChange) change).getValue();
        } else if (change instanceof NodeListChange) {
            value = ((NodeListChange) change).getValue();
        }

        if (value != null) {
            if (value instanceof StateNode && isServerOnly((StateNode) value)) {
                return;
            }
            handleTemplate(value);
        }

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
                List<BoundElementTemplate> childTemplates = ((BoundElementTemplate) value)
                        .getChildTemplates();
                if (childTemplates != null) {
                    for (BoundElementTemplate child : childTemplates) {
                        handleTemplate(child);
                    }
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
            logBuilder.addChange(node, removeChange);
        }

        @Override
        public void visitPutChange(StateNode node, PutChange putChange) {
            logBuilder.addChange(node, putChange);
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
            logBuilder.addChange(node, listInsertChange);
        }

        @Override
        public void visitListRemoveChange(StateNode node,
                ListRemoveChange listRemoveChange) {
            logBuilder.addChange(node, listRemoveChange);
        }

        @Override
        public void visitListReplaceChange(StateNode node,
                ListReplaceChange listReplaceChange) {
            logBuilder.addChange(node, listReplaceChange);
        }
    }

}
