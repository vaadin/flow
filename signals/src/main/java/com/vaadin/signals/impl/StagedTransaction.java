/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.signals.impl;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.vaadin.signals.impl.CommandsAndHandlers.CommandResultHandler;

import com.vaadin.signals.Id;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.SignalCommand.TransactionCommand;
import com.vaadin.signals.impl.CommandResult.Reject;
import com.vaadin.signals.impl.SignalTree.PendingCommit;
import com.vaadin.signals.operations.SignalOperation;
import com.vaadin.signals.operations.SignalOperation.ResultOrError;

/**
 * A conventional read-write transaction that stages commands to be submitted as
 * a single commit. Commits by incorporating staged changes into the outer
 * transaction if it's another staged transaction and otherwise performs a
 * two-phase commit to atomically apply changes to all participating trees.
 * Provides repeatable reads that are supplemented by changes from any staged
 * commands.
 */
public class StagedTransaction extends Transaction {
    /**
     * Submits a successful result if all registered dependencies submit
     * successful results and an error result if any dependency submits an
     * error.
     */
    static class ResultCollector {
        private final HashSet<Object> unresolvedDependencies;
        private final Consumer<ResultOrError<Void>> resultHandler;
        private final Object lock = new Object();

        private Boolean state = null;

        public ResultCollector(Collection<?> dependencies,
                Consumer<ResultOrError<Void>> resultHandler) {
            this.unresolvedDependencies = new HashSet<>(dependencies);
            this.resultHandler = resultHandler;
        }

        public CommandResultHandler registerDependency(Object dependency) {
            assert unresolvedDependencies.contains(dependency);

            return result -> {
                synchronized (lock) {
                    if (!unresolvedDependencies.remove(dependency)) {
                        assert false;
                    }
                    if (state != null) {
                        return;
                    }
                    if (result instanceof CommandResult.Reject error) {
                        state = Boolean.FALSE;
                        resultHandler.accept(
                                new SignalOperation.Error<>(error.reason()));
                    } else if (unresolvedDependencies.isEmpty()) {
                        state = Boolean.TRUE;
                        resultHandler
                                .accept(new SignalOperation.Result<>(null));
                    }
                }
            };
        }
    }

    static class TreeState {
        final CommandsAndHandlers staged = new CommandsAndHandlers();
        boolean failing = false;

        TreeRevision base;
        MutableTreeRevision revision;

        public TreeState(TreeRevision base) {
            this.base = base;
            revision = new MutableTreeRevision(base);
        }

        private MutableTreeRevision mutableBase() {
            if (base instanceof MutableTreeRevision mutable) {
                return mutable;
            } else {
                MutableTreeRevision mutable = new MutableTreeRevision(base);
                base = mutable;
                return mutable;
            }
        }

        void rebase(List<SignalCommand> baseCommands) {
            // Update base
            MutableTreeRevision base = mutableBase();
            base.apply(baseCommands);

            // Reset derived state and derive again
            failing = false;
            revision = new MutableTreeRevision(base);
            updateRevision(staged.getCommands());
        }

        void updateRevision(List<SignalCommand> commands) {
            Map<Id, CommandResult> results = revision
                    .applyAndGetResults(commands);

            failing |= results.values().stream().anyMatch(r -> !r.accepted());
        }
    }

    private final Map<SignalTree, TreeState> openTrees = new HashMap<>();

    private boolean failing = false;

    /**
     * Indicates that a commit is in progress or already performed. Application
     * code can run as part of notifying listeners at the end of a commit. At
     * that point, this transaction is still the current transaction which means
     * that any applied changes would end up ignored. To avoid that, writes
     * during a commit are delegated to the outer transaction. Reads also need
     * to be delegated so that any writes can be read.
     */
    private boolean committing = false;

    private final Transaction outer;

    /**
     * Creates a new staged transaction for the given outer transaction.
     *
     * @param outer
     *            the outer transaction to wrap, not <code>null</code>
     */
    public StagedTransaction(Transaction outer) {
        assert outer != null;
        this.outer = outer;
    }

    @Override
    protected void commit(Consumer<ResultOrError<Void>> resultHandler) {
        assert !committing;
        committing = true;

        if (openTrees.isEmpty()) {
            resultHandler.accept(new SignalOperation.Result<>(null));
            return;
        }

        if (outer instanceof StagedTransaction outerTx) {
            ResultCollector collector = new ResultCollector(openTrees.keySet(),
                    resultHandler);
            for (SignalTree tree : openTrees.keySet()) {
                outerTx.include(tree, createChange(tree, collector), true);
            }
        } else {
            commitTwoPhase(resultHandler);

            for (SignalTree tree : openTrees.keySet()) {
                CommandsAndHandlers staged = openTrees.get(tree).staged;

                TransactionCommand command = new SignalCommand.TransactionCommand(
                        Id.random(), staged.getCommands());

                outer.include(tree, command, null, false);
            }
        }
    }

    private void commitTwoPhase(Consumer<ResultOrError<Void>> resultHandler) {
        /*
         * Order by id to ensure all transactions lock trees in the same order.
         * Without this, there could be a deadlock if one transaction has
         * already locked A and tries to lock B while another transaction has
         * already locked B and tries to lock A.
         */
        List<SignalTree> trees = openTrees.entrySet().stream()
                .filter(entry -> !entry.getValue().staged.isEmpty())
                .map(Entry::getKey).sorted(Comparator.comparing(SignalTree::id))
                .toList();

        ResultCollector collector = new ResultCollector(trees, resultHandler);

        try {
            trees.forEach(tree -> tree.getLock().lock());

            List<PendingCommit> pendingCommits = trees.stream().map(
                    tree -> tree.prepareCommit(createChange(tree, collector)))
                    .toList();

            if (pendingCommits.stream().allMatch(PendingCommit::canCommit)) {
                pendingCommits.forEach(PendingCommit::applyChanges);
                pendingCommits.forEach(PendingCommit::publishChanges);
            } else {
                pendingCommits.forEach(PendingCommit::markAsAborted);
            }
        } finally {
            trees.forEach(tree -> tree.getLock().unlock());
        }
    }

    private CommandsAndHandlers createChange(SignalTree tree,
            ResultCollector collector) {
        Id txId = Id.random();

        CommandsAndHandlers change = openTrees.get(tree).staged;

        HashMap<Id, CommandResultHandler> handlers = new HashMap<>(
                change.getResultHandlers());
        handlers.put(txId, collector.registerDependency(tree));

        return new CommandsAndHandlers(
                List.of(new SignalCommand.TransactionCommand(txId,
                        change.getCommands())),
                handlers);
    }

    private TreeState getOrCreateTreeState(SignalTree tree) {
        TreeState treeState = openTrees.get(tree);
        if (treeState == null) {
            validateTreeTypes(tree);

            treeState = new TreeState(outer.read(tree));
            openTrees.put(tree, treeState);
        }

        return treeState;
    }

    @Override
    public void include(SignalTree tree, SignalCommand command,
            CommandResultHandler resultHandler, boolean applyToTree) {
        if (committing) {
            outer.include(tree, command, resultHandler, applyToTree);
            return;
        }

        include(tree, new CommandsAndHandlers(command, resultHandler),
                applyToTree);
    }

    private void include(SignalTree tree, CommandsAndHandlers commands,
            boolean applyToTree) {
        TreeState state = getOrCreateTreeState(tree);

        if (applyToTree) {
            state.staged.add(commands);

            state.updateRevision(commands.getCommands());
            failing |= state.failing;
        } else {
            state.rebase(commands.getCommands());
            failing = openTrees.values().stream().anyMatch(s -> s.failing);
        }
    }

    @Override
    public TreeRevision read(SignalTree tree) {
        if (committing) {
            return outer.read(tree);
        }

        TreeState state = getOrCreateTreeState(tree);
        return failing ? state.base : state.revision;
    }

    private void validateTreeTypes(SignalTree tree) {
        SignalTree.Type treeType = tree.type();
        if (treeType == SignalTree.Type.ASYNCHRONOUS) {
            if (!openTreeTypes().allMatch(SignalTree.Type.COMPUTED::equals)) {
                throw new IllegalStateException(
                        "An asynchronous signal can only share transaction with computed signals and other asynchronous signals that belong to the same tree.");
            }
        } else if (treeType == SignalTree.Type.SYNCHRONOUS) {
            if (openTreeTypes()
                    .anyMatch(SignalTree.Type.ASYNCHRONOUS::equals)) {
                throw new IllegalStateException(
                        "A synchronous signal cannot share transaction with asynchronous signals.");
            }
        }
    }

    private Stream<SignalTree.Type> openTreeTypes() {
        return openTrees.keySet().stream().map(SignalTree::type);
    }

    @Override
    protected void rollback() {
        Reject result = CommandResult.fail("Rolled back");

        for (TreeState state : openTrees.values()) {
            Map<Id, CommandResult> results = new HashMap<>();

            for (SignalCommand command : state.staged.getCommands()) {
                results.put(command.commandId(), result);
            }

            state.staged.notifyResultHandlers(results);
        }
    }
}
