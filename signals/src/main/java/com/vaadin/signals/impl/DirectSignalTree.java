package com.vaadin.signals.impl;

import java.util.Map;

import com.vaadin.signals.Id;
import com.vaadin.signals.SignalCommand;

/**
 * A signal tree that immediately confirms commands without waiting for an
 * external system to determine command ordering. This means that
 * {@link #confirmed()} and {@link #submitted()} return the same value. This
 * type of tree is intended for signals that are used only within a single JVM.
 */
public class DirectSignalTree extends SignalTree {

    /**
     * Creates a new direct signal tree.
     *
     * @param computedSignal
     *            <code>true</code> if the tree is used for backing a computed
     *            signal, <code>false</code> if its a standalone tree
     */
    public DirectSignalTree(boolean computedSignal) {
        super(computedSignal ? Type.COMPUTED : Type.DIRECT);
    }

    private Snapshot snapshot = new Snapshot(id(), false);

    @Override
    public Snapshot confirmed() {
        return getWithLock(() -> snapshot);
    }

    @Override
    public Snapshot submitted() {
        return confirmed();
    }

    @Override
    public PendingCommit prepareCommit(CommandsAndHandlers changes) {
        assert hasLock();

        Snapshot oldSnapshot = snapshot;

        MutableTreeRevision builder = new MutableTreeRevision(snapshot);
        Map<Id, CommandResult> results = builder
                .applyAndGetResults(changes.getCommands());

        boolean valid = changes.getCommands().stream()
                .map(SignalCommand::commandId).map(results::get)
                .allMatch(CommandResult::accepted);

        return new PendingCommit() {
            @Override
            public boolean canCommit() {
                assert hasLock();
                return valid;
            }

            @Override
            public void applyChanges() {
                assert hasLock();
                assert valid;

                snapshot = new Snapshot(builder);
            }

            @Override
            public void publishChanges() {
                assert hasLock();
                assert valid;

                notifyDependents(oldSnapshot, snapshot);
                changes.notifyResultHandlers(results);
            }

            @Override
            public void markAsAborted() {
                changes.notifyResultHandlers(CommandResult.rejectAll(results,
                        "Transaction aborted"));
            }
        };
    }

    @Override
    public Runnable pin() {
        // no-op for a direct tree
        return () -> {
        };
    }
}
