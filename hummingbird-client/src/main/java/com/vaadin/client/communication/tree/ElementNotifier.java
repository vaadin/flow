package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.client.communication.tree.NodeListener.ListInsertChange;
import com.vaadin.client.communication.tree.NodeListener.ListInsertNodeChange;
import com.vaadin.client.communication.tree.NodeListener.ListRemoveChange;
import com.vaadin.client.communication.tree.NodeListener.PutChange;
import com.vaadin.client.communication.tree.NodeListener.PutNodeChange;
import com.vaadin.client.communication.tree.NodeListener.PutOverrideChange;
import com.vaadin.client.communication.tree.NodeListener.RemoveChange;

import elemental.json.JsonObject;

public class ElementNotifier {
    public interface ElementUpdater {

        void putNode(String scope, PutNodeChange change);

        void putOverride(String scope, PutOverrideChange change);

        void remove(String scope, RemoveChange change);

        void listRemove(String scope, ListRemoveChange change);

        void listInsert(String scope, ListInsertChange change);

        void listInsertNode(String scope, ListInsertNodeChange change);

        void put(String scope, PutChange change);
    }

    private final List<ElementUpdater> updaters = new ArrayList<>();

    public ElementNotifier(TreeUpdater treeUpdater, JsonObject node,
            String scope) {
        treeUpdater.addNodeListener(node, new NodeListener() {
            @Override
            public void putNode(PutNodeChange change) {
                String parameter = scope + change.getKey();
                for (ElementUpdater updater : new ArrayList<>(updaters)) {
                    updater.putNode(parameter, change);
                }
            }

            @Override
            public void put(PutChange change) {
                String parameter = scope + change.getKey();
                for (ElementUpdater updater : new ArrayList<>(updaters)) {
                    updater.put(parameter, change);
                }
            }

            @Override
            public void listInsertNode(ListInsertNodeChange change) {
                String parameter = scope + change.getKey();
                for (ElementUpdater updater : new ArrayList<>(updaters)) {
                    updater.listInsertNode(parameter, change);
                }
            }

            @Override
            public void listInsert(ListInsertChange change) {
                String parameter = scope + change.getKey();
                for (ElementUpdater updater : new ArrayList<>(updaters)) {
                    updater.listInsert(parameter, change);
                }
            }

            @Override
            public void listRemove(ListRemoveChange change) {
                String parameter = scope + change.getKey();
                for (ElementUpdater updater : new ArrayList<>(updaters)) {
                    updater.listRemove(parameter, change);
                }
            }

            @Override
            public void remove(RemoveChange change) {
                String parameter = scope + change.getKey();
                for (ElementUpdater updater : new ArrayList<>(updaters)) {
                    updater.remove(parameter, change);
                }
            }

            @Override
            public void putOverride(PutOverrideChange change) {
                for (ElementUpdater updater : new ArrayList<>(updaters)) {
                    updater.putOverride(scope, change);
                }
            }
        });
    }

    public void addUpdater(ElementUpdater updater) {
        updaters.add(updater);
    }

    public void removeUpdater(ElementUpdater updater) {
        updaters.remove(updater);
    }
}