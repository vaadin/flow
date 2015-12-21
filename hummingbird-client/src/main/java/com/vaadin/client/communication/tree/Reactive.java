package com.vaadin.client.communication.tree;

import java.util.HashSet;

import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.JsSet;
import com.vaadin.client.Profiler;
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

        private final JsArrayObject<HandlerRegistration> registrations = JsArrayObject
                .create();

        public KeepUpToDateListener(Runnable runnable) {
            this.runnable = runnable;
            refreshValue();
        }

        private void refreshValue() {
            Profiler.enter("KeepUpToDateListener.refreshValue");

            JsSet<TreeNodeProperty> accessedProperties = collectAccessedProperies(
                    runnable);

            JsSet.forEach(accessedProperties, treeNodeProperty -> {
                HandlerRegistration registration = treeNodeProperty
                        .addPropertyChangeListener(this);
                registrations.add(registration);
            });
            Profiler.leave("KeepUpToDateListener.refreshValue");
        }

        @Override
        public void changeValue(Object oldValue, Object newValue) {
            Profiler.enter("KeepUpToDateListener.changeValue");

            unregister();
            pendingFlushes.add(flushListener);

            Profiler.leave("KeepUpToDateListener.changeValue");
        }

        private void unregister() {
            for (int i = 0; i < registrations.size(); i++) {
                HandlerRegistration handlerRegistration = registrations.get(i);
                handlerRegistration.removeHandler();
            }
            registrations.clear();
        }
    }

    private static JsSet<TreeNodeProperty> collector;

    public static JsSet<TreeNodeProperty> collectAccessedProperies(
            Runnable runnable) {
        JsSet<TreeNodeProperty> previousCollector = collector;
        JsSet<TreeNodeProperty> ownCollector = JsSet.create();
        collector = ownCollector;

        try {
            runnable.run();
        } finally {
            if (previousCollector != null) {
                JsSet.forEach(ownCollector, previousCollector::add);
            }
            collector = previousCollector;
        }
        return ownCollector;
    }

    public static void setAccessed(TreeNodeProperty treeNodeProperty) {
        if (collector != null) {
            // XXX Should kind of ignore property here if it's computed, except
            // that we would then have to find its original dependencies and add
            // those to the collector instead
            collector.add(treeNodeProperty);
        }
    }

    public static HandlerRegistration keepUpToDate(Runnable runnable) {
        Profiler.enter("Reactive.keepUpToDate");

        KeepUpToDateListener listener = new KeepUpToDateListener(runnable);
        HandlerRegistration handlerRegistration = new HandlerRegistration() {
            @Override
            public void removeHandler() {
                listener.unregister();
            }
        };

        Profiler.leave("Reactive.keepUpToDate");
        return handlerRegistration;
    }

    private static final HashSet<FlushListener> pendingFlushes = new HashSet<>();

    public static void flush() {
        Profiler.enter("Reactive.flush");

        while (!pendingFlushes.isEmpty()) {
            HashSet<FlushListener> currentFlushes = new HashSet<>(
                    pendingFlushes);
            pendingFlushes.clear();
            for (FlushListener flushListener : currentFlushes) {
                flushListener.onFlush();
            }
        }

        Profiler.leave("Reactive.flush");
    }
}
