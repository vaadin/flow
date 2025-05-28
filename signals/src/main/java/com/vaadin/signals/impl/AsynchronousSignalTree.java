package com.vaadin.signals.impl;

import java.util.List;
import java.util.Map;

import com.vaadin.signals.Id;
import com.vaadin.signals.SignalCommand;

/**
 * A signal tree that submits commands to an event log and asynchronously waits
 * for external confirmation before completing handling of the command. This
 * means that {@link #submitted} may contain changes that are not yet in
 * {@link #confirmed} and might never end up there if a concurrent change causes
 * a conflict. This type of tree is intended for signals that are synchronized
 * across a cluster.
 */
public abstract class AsynchronousSignalTree extends SignalTree {
    private final CommandsAndHandlers unconfirmedCommands = new CommandsAndHandlers();

    private Snapshot confirmed = new Snapshot(id(), true);

    private Snapshot submitted = new Snapshot(id(), true);

    /**
     * Creates a new asynchronous signal tree.
     */
    protected AsynchronousSignalTree() {
        super(Type.ASYNCHRONOUS);
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

            notifyObservers(oldSubmitted, submitted);

            unconfirmedCommands.notifyResultHandlers(results, commands);

            notifyProcessedCommandSubscribers(commands, results);
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

                notifyObservers(oldSnapshot, newSnapshot);

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
}
