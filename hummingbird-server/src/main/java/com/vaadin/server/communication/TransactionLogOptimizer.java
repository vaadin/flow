package com.vaadin.server.communication;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.change.NodeChange;
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
        this.templates = optimizeTemplates(templates);
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
        return changes;
    }

}
