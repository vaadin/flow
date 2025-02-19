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

import com.vaadin.signals.Id;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.SignalCommand.TransactionCommand;
import com.vaadin.signals.impl.CommandResult.Reject;
import com.vaadin.signals.impl.SignalTree.PendingCommit;
import com.vaadin.signals.operations.SignalOperation;
import com.vaadin.signals.operations.SignalOperation.ResultOrError;

/**
 * A conventional read-write transaction that stages commands to be submitted
 * when the transaction is committed. Commits by incorporating staged changes
 * into the outer transaction if it's another staged transaction and otherwise
 * performs a two-phase commit to atomically apply changes to all participating
 * trees. Provides repeatable reads that are supplemented by changes from any
 * staged commands.
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

        private Boolean state = null;

        public ResultCollector(Collection<?> dependencies,
                Consumer<ResultOrError<Void>> resultHandler) {
            this.unresolvedDependencies = new HashSet<>(dependencies);
            this.resultHandler = resultHandler;
        }

        public Consumer<CommandResult> registerDependency(Object dependency) {
            assert unresolvedDependencies.contains(dependency);

            return result -> {
                synchronized (unresolvedDependencies) {
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
                    } else {
                        if (unresolvedDependencies.isEmpty()) {
                            state = Boolean.TRUE;
                            resultHandler
                                    .accept(new SignalOperation.Result<>(null));
                        }
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

    private final Transaction outer;

    /**
     * Creates a new staged transaction for the given outer transaction.
     *
     * @param outer
     *            the outer transaction to wrap, not <code>null</code>
     */
    public StagedTransaction(Transaction outer) {
        this.outer = outer;
    }

    @Override
    protected void commit(Consumer<ResultOrError<Void>> resultHandler) {
        if (openTrees.isEmpty()) {
            resultHandler.accept(new SignalOperation.Result<>(null));
            return;
        }

        ResultCollector collector = new ResultCollector(openTrees.keySet(),
                resultHandler);

        if (outer instanceof StagedTransaction outerTx) {
            for (SignalTree tree : openTrees.keySet()) {
                outerTx.submit(tree, createChange(tree, collector), true);
            }
        } else {
            commitTwoPhase(collector);

            for (SignalTree tree : openTrees.keySet()) {
                CommandsAndHandlers staged = openTrees.get(tree).staged;

                TransactionCommand command = new SignalCommand.TransactionCommand(
                        Id.random(), staged.getCommands());

                outer.submit(tree, command, null, false);
            }
        }
    }

    private void commitTwoPhase(ResultCollector collector) {
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

        HashMap<Id, Consumer<CommandResult>> handlers = new HashMap<>(
                change.getResultHandlers());
        handlers.put(txId, collector.registerDependency(tree));

        return new CommandsAndHandlers(
                List.of(new SignalCommand.TransactionCommand(txId,
                        change.getCommands())),
                handlers);
    }

    private TreeState treeState(SignalTree tree) {
        TreeState treeState = openTrees.get(tree);
        if (treeState == null) {
            validateTreeTypes(tree);

            treeState = new TreeState(outer.read(tree));
            openTrees.put(tree, treeState);
        }

        return treeState;
    }

    @Override
    public void submit(SignalTree tree, SignalCommand command,
            Consumer<CommandResult> resultHandler, boolean apply) {
        submit(tree, new CommandsAndHandlers(command, resultHandler), apply);
    }

    private void submit(SignalTree tree, CommandsAndHandlers commands,
            boolean apply) {
        TreeState state = treeState(tree);

        if (apply) {
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
        TreeState state = treeState(tree);
        return failing ? state.base : state.revision;
    }

    private void validateTreeTypes(SignalTree tree) {
        SignalTree.Type treeType = tree.type();
        if (treeType == SignalTree.Type.ASYNC) {
            if (!openTreeTypes().allMatch(SignalTree.Type.COMPUTED::equals)) {
                throw new IllegalStateException();
            }
        } else if (treeType == SignalTree.Type.DIRECT) {
            if (openTreeTypes().anyMatch(SignalTree.Type.ASYNC::equals)) {
                throw new IllegalStateException();
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

    @Override
    protected boolean readonly() {
        return false;
    }
}
