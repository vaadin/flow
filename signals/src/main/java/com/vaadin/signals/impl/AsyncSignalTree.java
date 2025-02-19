package com.vaadin.signals.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.signals.Id;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.SignalCommand.SnapshotCommand;

/**
 * A signal tree that submits commands to an event log and asynchronously waits
 * for external confirmation before completing handling of the command. This
 * means that {@link #confirmed} may contain changes that are not yet in
 * {@link #submitted} and might never end up there if a concurrent change causes
 * a conflict. This type of tree is intended for signals that are synchronized
 * across a cluster.
 */
public abstract class AsyncSignalTree extends SignalTree {
    private final CommandsAndHandlers unconfirmedCommands = new CommandsAndHandlers();

    private Runnable unconfirmedCommandsPin;

    private Snapshot confirmed = new Snapshot(id(), true);

    private Snapshot submitted = new Snapshot(id(), true);

    private final Set<Object> pins = new HashSet<>();

    private List<SignalCommand> submitWhenPinned = new ArrayList<>();

    protected AsyncSignalTree() {
        super(Type.ASYNC);
    }

    /**
     * Submits a sequence of commands to the event log. It is expected that the
     * same sequence of commands will eventually be passed back to
     * {@link #confirm(List)}.
     *
     * @param commands
     *            the list of commands to submit, not <code>null</code>
     */
    protected abstract void submit(List<SignalCommand> commands);

    /**
     * Adds a sequence of commands to the confirmed snapshot. The commands might
     * originate from this tree instance through {@link #submit(List)} or from a
     * different tree in the same cluster that uses the same underlying event
     * log. Any remaining commands that have been submitted but not yet
     * confirmed will be re-applied on top of the new confirmed state.
     *
     * @param commands
     *            the sequence of confirmed commands, not <code>null</code>
     */
    public void confirm(List<SignalCommand> commands) {
        runWithLock(() -> {
            MutableTreeRevision builder = new MutableTreeRevision(confirmed);

            Map<Id, CommandResult> results = builder
                    .applyAndGetResults(commands);

            confirmed = new Snapshot(builder);

            unconfirmedCommands.removeHandledCommands(results.keySet());

            Snapshot oldSubmitted = submitted;

            /*
             * TODO: could skip this part if the newly confirmed commands were
             * at the head of unconfirmedCommands since submitted doesn't change
             * in that case
             */
            if (!unconfirmedCommands.isEmpty()) {
                builder.apply(unconfirmedCommands.getCommands());

                submitted = new Snapshot(builder);
            } else {
                submitted = confirmed;
            }

            notifyDependents(oldSubmitted, submitted);

            unconfirmedCommands.notifyResultHandlers(results, commands);

            if (unconfirmedCommandsPin != null
                    && !hasPinnableUnconfirmedCommand()) {
                Runnable oldPin = unconfirmedCommandsPin;
                unconfirmedCommandsPin = null;
                oldPin.run();
            }
        });
    }

    @Override
    public PendingCommit prepareCommit(CommandsAndHandlers changes) {
        assert hasLock();

        Snapshot oldSnapshot = submitted;

        MutableTreeRevision builder = new MutableTreeRevision(submitted);

        builder.apply(changes.getCommands());

        Snapshot newSnapshot = new Snapshot(builder);

        return new PendingCommit() {
            @Override
            public boolean canCommit() {
                assert hasLock();

                // Can always "commit" since conflicts will be resolved only
                // when confirmed
                return true;
            }

            @Override
            public void applyChanges() {
                assert hasLock();

                unconfirmedCommands.add(changes);
                submitted = newSnapshot;
            }

            @Override
            public void publishChanges() {
                assert hasLock();

                notifyDependents(oldSnapshot, newSnapshot);

                if (unconfirmedCommandsPin == null
                        && hasPinnableUnconfirmedCommand()) {
                    unconfirmedCommandsPin = pin();
                }

                submit(changes.getCommands());
            }

            @Override
            public void markAsAborted() {
                // Async transactions cannot be aborted
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Snapshot confirmed() {
        return getWithLock(() -> confirmed);
    }

    @Override
    public Snapshot submitted() {
        return getWithLock(() -> submitted);
    }

    /**
     * Creates a new snapshot command based on the currently confirmed nodes.
     *
     * @return the new snapshot command, not <code>null</code>
     */
    public SnapshotCommand createSnapshot() {
        return getWithLock(() -> {
            return new SnapshotCommand(Id.random(), confirmed.nodes());
        });
    }

    @Override
    public Runnable pin() {
        Object pin = new Object();

        runWithLock(() -> {
            boolean wasEmpty = pins.isEmpty();

            pins.add(pin);

            if (wasEmpty) {
                onPinStatusChanged(true);
            }
        });

        return wrapWithLock(() -> {
            if (pins.remove(pin) && pins.isEmpty()) {
                onPinStatusChanged(false);
            }
        });
    }

    /**
     * Checks whether this tree is currently pinned.
     *
     * @return <code>true</code> if the tree is pinned
     */
    public boolean isPinned() {
        return getWithLock(() -> !pins.isEmpty());
    }

    /**
     * Callback that is run whenever the first pin is created or the last one is
     * cleared. This allows the tree instance to react when the value of
     * {@link #isPinned()} is toggled.
     *
     * @param pinned
     *            <code>true</code> if the tree is now pinned,
     *            <code>false</code> if it's now unpinned
     */
    protected void onPinStatusChanged(boolean pinned) {
        if (pinned) {
            if (!submitWhenPinned.isEmpty()) {
                submitWhenPinned.forEach(insert -> applyChange(insert));
                submitWhenPinned.clear();
            }
        } else {
            var originalInserts = submitted().originalInserts();
            if (!originalInserts.isEmpty()) {
                submitWhenPinned.addAll(originalInserts.values());
                applyChange(
                        new SignalCommand.ClearOwnerCommand(Id.random(), id()));
            }
        }
    }

    private boolean hasPinnableUnconfirmedCommand() {
        /*
         * Don't pin for clear owner commands since those are emitted when
         * unpinned. Ignoring this type of command is fine since it's not a
         * user-issued command which in turn means that the user will not wait
         * for the command to be confirmed.
         */
        return unconfirmedCommands.getCommands().stream().anyMatch(
                command -> !(command instanceof SignalCommand.ClearOwnerCommand));
    }
}
