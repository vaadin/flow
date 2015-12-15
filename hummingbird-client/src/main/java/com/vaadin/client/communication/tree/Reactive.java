package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.communication.tree.TreeNodeProperty.TreeNodePropertyValueChangeListener;

public class Reactive {

    private static interface FlushListener {
        public void onFlush();
    }

    private static class KeepUpToDateListener
            implements TreeNodePropertyValueChangeListener {
        private final Runnable runnable;
        private final FlushListener flushListener = new FlushListener() {
            @Override
            public void onFlush() {
                refreshValue();
            }
        };

        private final ArrayList<HandlerRegistration> registrations = new ArrayList<>();

        public KeepUpToDateListener(Runnable runnable) {
            this.runnable = runnable;
            refreshValue();
        }

        private void refreshValue() {
            Collection<TreeNodeProperty> accessedProperties = collectAccessedProperies(
                    runnable);

            for (TreeNodeProperty treeNodeProperty : accessedProperties) {
                HandlerRegistration registration = treeNodeProperty
                        .addPropertyChangeListener(this);
                registrations.add(registration);
            }
        }

        @Override
        public void changeValue(Object oldValue, Object newValue) {
            unregister();
            pendingFlushes.add(flushListener);
        }

        private void unregister() {
            for (HandlerRegistration handlerRegistration : registrations) {
                handlerRegistration.removeHandler();
            }
            registrations.clear();
        }
    }

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

    public static HandlerRegistration keepUpToDate(Runnable runnable) {
        KeepUpToDateListener listener = new KeepUpToDateListener(runnable);
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                listener.unregister();
            }
        };
    }

    private static final HashSet<FlushListener> pendingFlushes = new HashSet<>();

    public static void flush() {
        while (!pendingFlushes.isEmpty()) {
            HashSet<FlushListener> currentFlushes = new HashSet<>(
                    pendingFlushes);
            pendingFlushes.clear();
            for (FlushListener flushListener : currentFlushes) {
                flushListener.onFlush();
            }
        }
    }
}
