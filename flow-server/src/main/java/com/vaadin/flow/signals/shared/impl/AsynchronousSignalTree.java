/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.signals.shared.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.SignalCommand;

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

            /*
             * Check if the confirmed commands were at the head of
             * unconfirmedCommands and all were accepted. If so, submitted
             * doesn't change and we can skip re-applying and notifying
             * observers. Rejected commands must still trigger a rebuild since
             * the submitted snapshot has the optimistic changes that need to be
             * rolled back.
             */
            boolean confirmedFromHead = allAccepted(results)
                    && wereAtHead(unconfirmedCommands.getCommands(), commands);

            // Remove any pending commands that are now confirmed from the queue
            unconfirmedCommands.removeHandledCommands(results.keySet());

            Snapshot oldSubmitted = submitted;

            if (confirmedFromHead) {
                // submitted doesn't change so no need to rebuild or notify
            } else if (!unconfirmedCommands.isEmpty()) {
                // Re-apply pending commands that remain in the queue
                builder.apply(unconfirmedCommands.getCommands());

                submitted = new Snapshot(builder);
            } else {
                submitted = confirmed;
            }

            if (!confirmedFromHead) {
                notifyObservers(oldSubmitted, submitted);
            }

            unconfirmedCommands.notifyResultHandlers(results, commands);

            notifyProcessedCommandSubscribers(commands, results);
        });
    }

    /**
     * Checks whether all results were accepted.
     */
    private static boolean allAccepted(Map<Id, CommandResult> results) {
        return results.values().stream().allMatch(CommandResult::accepted);
    }

    /**
     * Checks whether all confirmed commands appear at the head of the
     * unconfirmed command list. This means the confirmed commands were the
     * first commands submitted to this tree and no reordering or external
     * commands were involved. Uses the top-level command list to determine the
     * count rather than the results key set, since results may also include
     * sub-command IDs from transaction commands.
     */
    private static boolean wereAtHead(List<SignalCommand> unconfirmedCommands,
            List<SignalCommand> confirmedCommands) {
        int confirmedCount = confirmedCommands.size();
        if (confirmedCount == 0) {
            return true;
        }
        if (confirmedCount > unconfirmedCommands.size()) {
            return false;
        }
        for (int i = 0; i < confirmedCount; i++) {
            if (!unconfirmedCommands.get(i).commandId()
                    .equals(confirmedCommands.get(i).commandId())) {
                return false;
            }
        }
        return true;
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
        return Objects.requireNonNull(getWithLock(() -> confirmed));
    }

    @Override
    public Snapshot submitted() {
        return Objects.requireNonNull(getWithLock(() -> submitted));
    }
}
