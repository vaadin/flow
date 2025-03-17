package com.vaadin.signals.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.operations.SignalOperation;
import com.vaadin.signals.operations.SignalOperation.ResultOrError;
import com.vaadin.signals.operations.TransactionOperation;

/**
 * A context for running commands that might be related to each other. The
 * current transaction is registered as a {@link ThreadLocal} that is used by
 * all signal operations running on that thread. Transactions can be nested so
 * that changes from an inner transaction are rolled up to the outer
 * transaction.
 */
public abstract class Transaction {
    /**
     * A transaction that applies commands directly to the underlying tree
     * immediately rather than collecting them for an eventual commit operation.
     * Committing and rolling back are no-ops since all commands have already
     * been applied.
     */
    private static abstract class ImmediateTransaction extends Transaction {
        private final boolean readonly;

        /**
         * Creates a new immediate transaction.
         *
         * @param readonly
         *            <code>true</code> if the transaction should reject regular
         *            writes
         */
        public ImmediateTransaction(boolean readonly) {
            this.readonly = readonly;
        }

        @Override
        public void include(SignalTree tree, SignalCommand command,
                Consumer<CommandResult> resultHandler, boolean applyToTree) {
            if (readonly() && tree.type() != SignalTree.Type.COMPUTED) {
                throw new IllegalStateException(
                        "Cannot make changes in a read-only transaction.");
            }

            if (applyToTree) {
                tree.commitSingleCommand(command, resultHandler);
            }
        }

        @Override
        protected void commit(Consumer<ResultOrError<Void>> resultHandler) {
            resultHandler.accept(new SignalOperation.Result<>(null));
        }

        @Override
        protected void rollback() {
            // nop
        }

        @Override
        protected boolean readonly() {
            return readonly;
        }
    }

    /**
     * An immediate transaction with repeatable reads. Stores a mutable tree
     * revision for each participating tree that is updated based on any
     * commands applied within the transaction. It is safe to add trees on
     * demand since we don't give any guarantees about ordering of operations
     * between separate trees.
     */
    private static class RepeatableReadTransaction
            extends ImmediateTransaction {

        private final Map<SignalTree, MutableTreeRevision> trees = new HashMap<>();
        private final Transaction outer;

        public RepeatableReadTransaction(boolean readonly, Transaction outer) {
            super(readonly);
            assert outer != null;
            this.outer = outer;
        }

        private MutableTreeRevision getOrCreateReadRevision(SignalTree tree) {
            return trees.computeIfAbsent(tree,
                    newTree -> new MutableTreeRevision(outer.read(newTree)));
        }

        @Override
        public void include(SignalTree tree, SignalCommand command,
                Consumer<CommandResult> resultHandler, boolean applyToTree) {
            super.include(tree, command, resultHandler, applyToTree);

            getOrCreateReadRevision(tree).apply(command, null);

            // Let an outer transaction update its own read revision
            outer.include(tree, command, null, false);
        }

        @Override
        public TreeRevision read(SignalTree tree) {
            return getOrCreateReadRevision(tree);
        }
    }

    /**
     * The default stateless transaction logic that is used when no transaction
     * is active. Applies commands directly to the underlying tree and reads
     * directly from the tree. This is basically not a transaction at all but
     * still expressed as a transaction so that transaction-aware logic doesn't
     * have to check whether a transaction is active.
     */
    private static final Transaction ROOT = new ImmediateTransaction(false) {
        @Override
        public TreeRevision read(SignalTree tree) {
            return tree.submitted();
        }
    };

    /**
     * The type of a transaction, determining how it handles reads and writes.
     */
    public enum Type {
        /**
         * A conventional read-write transaction that stages commands to be
         * submitted as a single commit. Provides repeatable reads that are
         * updated by changes from any staged commands.
         */
        STAGED {
            @Override
            Transaction create(Transaction outer) {
                if (outer.readonly()) {
                    throw new IllegalStateException();
                }
                return new StagedTransaction(outer);
            }
        },
        /**
         * A transaction that applies commands immediately to the underlying
         * tree while providing repeatable reads that are updated by changes
         * from applied commands.
         */
        WRITE_THROUGH {
            @Override
            Transaction create(Transaction outer) {
                if (outer.readonly()) {
                    throw new IllegalStateException();
                }

                return new RepeatableReadTransaction(false, outer);
            }
        },
        /**
         * A transaction that disallows regular write operations while offering
         * repeatable reads. Writes to computed signal trees are still allowed
         * since those are not user-originated write operations but just side
         * effects of lazy evaluation.
         */
        READ_ONLY {
            @Override
            Transaction create(Transaction outer) {
                return new RepeatableReadTransaction(true, outer);
            }
        };

        /**
         * Creates a new transaction instance of this type as an inner
         * transaction for the provided outer transaction.
         *
         * @param outer
         *            the outer transaction to use, not <code>null</code>
         * @return a new transaction instance, not <code>null</code>
         * @throws IllegalStateException
         *             if this transaction type is not compatible with the
         *             provided outer transaction
         */
        abstract Transaction create(Transaction outer)
                throws IllegalStateException;
    }

    private static final ThreadLocal<Transaction> currentTransaction = new ThreadLocal<>();

    /**
     * Gets the current transaction handler.
     *
     * @return the current transaction handler, not <code>null</code>
     */
    public static Transaction getCurrent() {
        Transaction transaction = currentTransaction.get();
        if (transaction == null) {
            return ROOT;
        } else {
            return transaction;
        }
    }

    /**
     * Checks whether a transaction is currently active on the current thread.
     *
     * @return <code>true</code> if a transaction is active
     */
    public static boolean inTransaction() {
        return currentTransaction.get() != null;
    }

    /**
     * Runs the given supplier in a regular transaction and returns an operation
     * object that wraps the supplier value. The created transaction handler
     * will be available from {@link #getCurrent()}.
     * <p>
     * The transaction will be committed after running the task, or rolled back
     * if the task throws an exception.
     *
     *
     * @param <T>
     *            the supplier type
     * @param transactionTask
     *            the supplier to run in a transaction, not <code>null</code>
     * @return the operation object that wraps the supplier value, not
     *         <code>null</code>
     */
    public static <T> TransactionOperation<T> runInTransaction(
            Supplier<T> transactionTask) {
        return runInTransaction(transactionTask, Type.STAGED);
    }

    /**
     * Runs the given supplier in a transaction of the given type and returns an
     * operation object that wraps the supplier value. The created transaction
     * handler will be available from {@link #getCurrent()}.
     *
     * @param <T>
     *            the supplier type
     * @param transactionTask
     *            the supplier to run in a transaction, not <code>null</code>
     * @param transactionType
     *            the type of the transaction, not <code>null</code>
     * @return the operation object that wraps the supplier value, not
     *         <code>null</code>
     */
    public static <T> TransactionOperation<T> runInTransaction(
            Supplier<T> transactionTask, Type transactionType) {
        Transaction outer = getCurrent();

        Transaction inner = transactionType.create(outer);
        currentTransaction.set(inner);
        try {
            T value = transactionTask.get();
            TransactionOperation<T> op = new TransactionOperation<>(value);

            inner.commit(op.result()::complete);

            return op;
        } catch (RuntimeException e) {
            inner.rollback();

            throw e;
        } finally {
            if (outer == ROOT) {
                currentTransaction.remove();
            } else {
                currentTransaction.set(outer);
            }
        }
    }

    private static Supplier<Void> asSupplier(Runnable runnable) {
        return () -> {
            runnable.run();
            return null;
        };
    }

    /**
     * Runs the given task in a transaction of the given type and returns an
     * operation object without a value. The created transaction handler will be
     * available from {@link #getCurrent()}.
     * <p>
     * The transaction will be committed after running the task, or rolled back
     * if the task throws an exception.
     *
     * @param transactionTask
     *            the task to run, not <code>null</code>
     * @param transactionType
     *            the type of the transaction, not <code>null</code>
     * @return the operation object, not <code>null</code>
     */
    public static TransactionOperation<Void> runInTransaction(
            Runnable transactionTask, Type transactionType) {
        return runInTransaction(asSupplier(transactionTask), transactionType);
    }

    /**
     * Runs the given task in a regular transaction and returns an operation
     * object without a value. The created transaction handler will be available
     * from {@link #getCurrent()}.
     * <p>
     * The transaction will be committed after running the task, or rolled back
     * if the task throws an exception.
     *
     * @param transactionTask
     *            the task to run, not <code>null</code>
     * @return the operation object, not <code>null</code>
     */
    public static TransactionOperation<Void> runInTransaction(
            Runnable transactionTask) {
        return runInTransaction(asSupplier(transactionTask));
    }

    /**
     * Runs the given supplier outside any transaction and returns the supplied
     * value. The current transaction will be restored after the task has been
     * run.
     *
     * @param <T>
     *            the supplier type
     * @param task
     *            the supplier to run, not <code>null</code>
     * @return the value returned from the supplier
     */
    public static <T> T runWithoutTransaction(Supplier<T> task) {
        Transaction previousTransaction = currentTransaction.get();
        try {
            currentTransaction.set(null);

            return task.get();
        } finally {
            currentTransaction.set(previousTransaction);
        }
    }

    /**
     * Runs the given task outside any transaction. The current transaction will
     * be restored after the task has been run.
     *
     * @param task
     *            the task to run, not <code>null</code>
     */
    public static void runWithoutTransaction(Runnable task) {
        runWithoutTransaction(asSupplier(task));
    }

    /**
     * Includes the given command to the given tree in the context of this
     * transaction and optionally also sets the command to be applied to the
     * underlying signal tree. Depending on the transaction type, an applied
     * command may be applied immediately, collected to be applied upon
     * committing, or rejected with an {@link IllegalStateException}.
     *
     * @param tree
     *            the signal tree against which to run the command, not
     *            <code>null</code>
     * @param command
     *            the command to include, not <code>null</code>
     * @param resultHandler
     *            the handler of the command result, or <code>null</code> to
     *            ignore the result
     * @param applyToTree
     *            <code>true</code> to apply the command to the underlying tree,
     *            <code>false</code> to only update the transaction's
     *            repeatable-read revision
     */
    protected abstract void include(SignalTree tree, SignalCommand command,
            Consumer<CommandResult> resultHandler, boolean applyToTree);

    /**
     * Includes the given command to the given tree in the context of this
     * transaction and sets the command to be applied to the underlying signal
     * tree. Depending on the transaction type, an applied command may be
     * applied immediately, collected to be applied upon committing, or rejected
     * with an {@link IllegalStateException}.
     *
     * @param tree
     *            the signal tree against which to run the command, not
     *            <code>null</code>
     * @param command
     *            the command to apply, not <code>null</code>
     * @param resultHandler
     *            the handler of the command result, or <code>null</code> to
     *            ignore the result
     */
    public void include(SignalTree tree, SignalCommand command,
            Consumer<CommandResult> resultHandler) {
        include(tree, command, resultHandler, true);
    }

    /**
     * Gets a revision for reading from the given tree in the context of this
     * transaction.
     *
     * @param tree
     *            the tree to read from, not <code>null</code>
     * @return a tree revision to read from, not <code>null</code>
     */
    public abstract TreeRevision read(SignalTree tree);

    /**
     * Commits any staged commands in this transaction.
     *
     * @param resultHandler
     *            a consumer to update the result value in the corresponding
     *            transaction operation, not <code>null</code>
     */
    protected abstract void commit(Consumer<ResultOrError<Void>> resultHandler);

    /**
     * Rolls back any staged commands in this transaction and notifies the
     * result handlers for those commands.
     */
    protected abstract void rollback();

    /**
     * Checks whether this is a read-only transaction. A read-only transaction
     * only allows writes to computed signal trees since those are not
     * user-originated write operations but just side effects of lazy
     * evaluation.
     *
     * @return {@code false} if this transaction allows regular writes to the
     *         underlying tree, {@code true} otherwise.
     */
    protected abstract boolean readonly();
}
