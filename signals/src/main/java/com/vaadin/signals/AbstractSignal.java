package com.vaadin.signals;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.impl.CommandResult;
import com.vaadin.signals.impl.SignalTree;
import com.vaadin.signals.impl.StagedTransaction;
import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.impl.TransientListener;
import com.vaadin.signals.impl.TreeRevision;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;
import com.vaadin.signals.operations.InsertOperation;
import com.vaadin.signals.operations.SignalOperation;

/**
 * Base type for full-featured signals that are backed by a transactional signal
 * tree.
 * <p>
 * This signal may be synchronized across a cluster. In that case, changes to
 * the signal value are only confirmed asynchronously. The regular signal
 * {@link #value()} returns the assumed value based on local modifications
 * whereas {@link #peekConfirmed()} gives access to the confirmed value.
 *
 * @param <T>
 *            the signal value type
 */
public abstract class AbstractSignal<T> implements Signal<T> {
    private final SignalTree tree;
    private final Id id;
    private final Predicate<SignalCommand> validator;

    private static final ObjectMapper OBJECT_MAPPER;
    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    /**
     * Signal validator that accepts anything. This is defined as a constant to
     * enable using <code>==</code> to detect and optimize cases where no
     * validation is applied.
     */
    protected static final Predicate<SignalCommand> ANYTHING_GOES = anything -> true;

    /**
     * Creates a new signal instance with the given id and validator for the
     * given signal tree.
     *
     * @param tree
     *            the signal tree that contains the value for this signal, not
     *            <code>null</code>
     * @param id
     *            the id of the signal node within the signal tree, not
     *            <code>null</code>
     * @param validator
     *            the validator to check operations submitted to this singal,
     *            not <code>null</code>
     */
    protected AbstractSignal(SignalTree tree, Id id,
            Predicate<SignalCommand> validator) {
        this.tree = Objects.requireNonNull(tree);
        this.validator = Objects.requireNonNull(validator);
        this.id = Objects.requireNonNull(id);
    }

    /**
     * Gets the data node for this signal in the given tree revision.
     *
     * @param revision
     *            the tree revision to read from, not <code>null</code>
     * @return the data node, or <code>null</code> if there is no node for this
     *         signal in the revision
     */
    protected Data data(TreeRevision revision) {
        return revision.data(id()).orElse(null);
    }

    /**
     * Gets the data node for this signal in the given transaction.
     *
     * @param transaction
     *            the transaction to read from, not <code>null</code>
     * @return the data node, or <code>null</code> if there is no node for this
     *         signal in the transaction
     */
    protected Data data(Transaction transaction) {
        return data(transaction.read(tree()));
    }

    @Override
    public T value() {
        Transaction transaction = Transaction.getCurrent();
        Data data = data(transaction);

        if (transaction instanceof StagedTransaction && data != null) {
            /*
             * This could be optimized to avoid creating the command if
             * lastUpdate has already been set in the same transaction
             */
            submit(new SignalCommand.LastUpdateCondition(Id.random(), id(),
                    data.lastUpdate()));
        }

        /*
         * Extract value before registering since extracting sets up state that
         * is used by registerDepedency in the case of computed signals.
         */
        T value = extractValue(data);
        if (UsageTracker.isActive()) {
            UsageTracker.registerUsage(createUsage(transaction));
        }
        return value;
    }

    /**
     * Reads the value without setting up any dependencies. This method returns
     * the same value as {@link #value()} but without creating a dependency when
     * used inside a transaction, effect or computed signal.
     *
     * @return the signal value
     */
    public T peek() {
        return extractValue(data(Transaction.getCurrent()));
    }

    /**
     * Reads the confirmed value without setting up any dependencies. The
     * confirmed value doesn't consider changes in the current transaction or
     * changes that have been submitted but not yet confirmed in a cluster.
     *
     * @return the confirmed signal value
     */
    public T peekConfirmed() {
        return extractValue(data(tree().confirmed()));
    }

    /**
     * Gets the validator used by this signal instance.
     *
     * @return the used validator, not <code>null</code>
     */
    protected Predicate<SignalCommand> validator() {
        return validator;
    }

    /**
     * Merges the validator used by this signal with the given validator. This
     * chains the two validators so that both must accept any change but it
     * additionally avoids redundant chaining in case either validator is
     * {@link #ANYTHING_GOES}.
     *
     * @param validator
     *            the validator to merge, not <code>null</code>
     * @return a combined validator, not <code>null</code>
     */
    protected Predicate<SignalCommand> mergeValidators(
            Predicate<SignalCommand> validator) {
        Predicate<SignalCommand> own = validator();
        if (own == ANYTHING_GOES) {
            return validator;
        } else if (validator == ANYTHING_GOES) {
            return own;
        } else {
            return own.and(validator);
        }
    }

    /**
     * Extracts the value for this signal from the given signal data node.
     *
     * @param data
     *            the data node to extract the value from, or <code>null</code>
     *            if the node doesn't exist in the tree
     * @return the signal value
     */
    protected abstract T extractValue(Data data);

    /**
     * Gets a reference value that will be used to determine whether a
     * dependency based on previous usage should be invalidated. This is done by
     * getting one reference value when the dependency occurs and then comparing
     * that to the current value to determine if the value has changed.
     * <p>
     * The implementation should return an object that changes if and only if
     * the {@link #value()} of this signal changes.
     *
     * @param data
     *            the data node to read from, not <code>null</code>
     * @return a reference value to use for validity checks, may be
     *         <code>null</code>
     */
    protected abstract Object usageChangeValue(Data data);

    boolean isValid(SignalCommand command) {
        if (command instanceof SignalCommand.ConditionCommand) {
            return true;
        } else if (command instanceof SignalCommand.TransactionCommand tx) {
            return tx.commands().stream().allMatch(this::isValid);
        } else {
            return validator().test(command);
        }
    }

    /**
     * Submits a command for this signal and updates the given operation using
     * the given result converter once the command result is confirmed. The
     * command is submitted through the current {@link Transaction} and it uses
     * {@link SignalEnvironment#getCurrentResultNotifier()} for delivering the
     * result update.
     *
     * @param <R>
     *            the result type
     * @param <O>
     *            the operation type
     * @param command
     *            the command to submit, not <code>null</code>
     * @param resultConverter
     *            a callback for creating an operation result value based on the
     *            command result, not <code>null</code>
     * @param operation
     *            the operation to update with the eventual result, not
     *            <code>null</code>
     * @return the provided operation, for chaining
     */
    protected <R, O extends SignalOperation<R>> O submit(SignalCommand command,
            Function<CommandResult.Accept, R> resultConverter, O operation) {
        // Remove is issued through the parent but targets the child
        assert command instanceof SignalCommand.RemoveCommand
                || id().equals(command.targetNodeId());

        if (!isValid(command)) {
            throw new UnsupportedOperationException();
        }

        Executor notifier = SignalEnvironment.getCurrentResultNotifier();

        Transaction.getCurrent().include(tree(), command, result -> {
            operation.result().completeAsync(() -> {
                if (result instanceof CommandResult.Accept accept) {
                    return new SignalOperation.Result<>(
                            resultConverter.apply(accept));
                } else if (result instanceof CommandResult.Reject reject) {
                    return new SignalOperation.Error<>(reject.reason());
                } else {
                    throw new RuntimeException(
                            "Unsupported result type: " + result);
                }
            }, notifier);
        });

        return operation;
    }

    /**
     * Submits a command for this signal and updates the given operation without
     * a value once the command result is confirmed. This is a shorthand for
     * {@link #submit(SignalCommand, Function, SignalOperation)} in the case of
     * operations that don't have a result value.
     *
     * @param <O>
     *            the operation type
     * @param command
     *            the command to submit, not <code>null</code>
     * @param operation
     *            the operation to update with the eventual result, not
     *            <code>null</code>
     * @return the provided operation, for chaining
     */
    protected <O extends SignalOperation<Void>> O submitVoidOperation(
            SignalCommand command, O operation) {
        return submit(command, success -> null, operation);
    }

    /**
     * Submits a command for this signal and creates and insert operation that
     * is updated once the command result is confirmed. This is a shorthand for
     * {@link #submit(SignalCommand, Function, SignalOperation)} in the case of
     * insert operations.
     *
     * @param <I>
     *            the insert operation type
     * @param command
     *            the command to submit, not <code>null</code>
     * @param childFactory
     *            callback used to create a signal instance in the insert
     *            operation, not <code>null</code>
     * @return the created insert operation, not <code>null</code>
     */
    protected <I extends AbstractSignal<?>> InsertOperation<I> submitInsert(
            SignalCommand command, Function<Id, I> childFactory) {
        return submitVoidOperation(command,
                new InsertOperation<>(childFactory.apply(command.commandId())));
    }

    /**
     * Submits a command for this signal and uses the provided result converter
     * to updates the created operation once the command result is confirmed.
     * This is a shorthand for
     * {@link #submit(SignalCommand, Function, SignalOperation)} in the case of
     * using the default operation type.
     *
     * @param <R>
     *            the operation result value
     * @param command
     *            the command to submit, not <code>null</code>
     * @param resultConverter
     *            a callback for creating an operation result value based on the
     *            command result, not <code>null</code>
     * @return the created operation instance, not <code>null</code>
     */
    protected <R> SignalOperation<R> submit(SignalCommand command,
            Function<CommandResult.Accept, R> resultConverter) {
        return submit(command, resultConverter, new SignalOperation<R>());
    }

    /**
     * Submits a command for this signal and updates the created operation
     * without a value once the command result is confirmed. This is a shorthand
     * for {@link #submit(SignalCommand, Function, SignalOperation)} in the case
     * of using the default operation type and no result value.
     *
     * @param command
     *            the command to submit, not <code>null</code>
     * @return the created operation instance, not <code>null</code>
     */
    protected SignalOperation<Void> submit(SignalCommand command) {
        return submitVoidOperation(command, new SignalOperation<Void>());
    }

    /**
     * Gets the unique id of this signal instance. The id will be the same for
     * other signal instances backed by the same data, e.g. in the case of using
     * {@link #asNode()} to create a signal of different type.
     *
     * @return the signal id, not null
     */
    public Id id() {
        return id;
    }

    /**
     * Gets the signal tree that stores the value for this signal.
     *
     * @return the signal tree, not <code>null</code>
     */
    protected SignalTree tree() {
        return tree;
    }

    /**
     * Creates a usage instance based on the current state of this signal.
     *
     * @param transaction
     *            the transaction for which the usage occurs, not
     *            <code>null</code>
     * @return a usage instance, not <code>null</code>
     */
    protected Usage createUsage(Transaction transaction) {
        Data data = data(transaction);
        if (data == null) {
            // Node is removed so no usage to track
            return UsageTracker.NO_USAGE;
        }

        // Capture so that we can use it later
        Object originalValue = usageChangeValue(data);

        return new Usage() {
            @Override
            public boolean hasChanges() {
                Data currentData = data(Transaction.getCurrent());

                return currentData != null && !Objects.equals(originalValue,
                        usageChangeValue(currentData));
            }

            @Override
            public Runnable onNextChange(TransientListener listener) {
                SignalTree tree = tree();

                /*
                 * Lock the tree to eliminate the risk that it processes a
                 * change after checking for previous changes but before adding
                 * a listener to the tree, since the listener would in that case
                 * miss that change
                 */
                tree.getLock().lock();
                try {
                    /*
                     * Run the listener right away if there's already a change.
                     */
                    if (hasChanges()) {
                        boolean listenToNext = listener.invoke();
                        /*
                         * If the listener is no longer interested in changes
                         * after an initial invocation, then return without
                         * adding a listener to the tree and thus without
                         * anything to clean up.
                         */
                        if (!listenToNext) {
                            return () -> {
                            };
                        }
                    }

                    return tree.observeNextChange(id(), () -> {
                        /*
                         * Only invoke the listener if the tree change is
                         * relevant in the context of this usage instance
                         */
                        if (hasChanges()) {
                            /*
                             * Run listener and let it decide if we should keep
                             * listening to the tree
                             */
                            return listener.invoke();
                        } else {
                            /*
                             * Keep listening to the tree since the listener
                             * hasn't yet been invoked
                             */
                            return true;
                        }
                    });
                } finally {
                    tree.getLock().unlock();
                }

            }
        };
    }

    /**
     * Converts this signal into a node signal. This allows further conversion
     * into any specific signal type through the methods in {@link NodeSignal}.
     * The converted signal is backed by the same underlying data and uses the
     * same validator as this signal.
     *
     * @return this signal as a node signal, not <code>null</code>
     */
    protected NodeSignal asNode() {
        // This method is protected to avoid exposing in cases where it doesn't
        // make sense
        assert (!(this instanceof NodeSignal));

        return new NodeSignal(tree(), id(), validator());
    }

    /**
     * Helper to submit a clear command. This is a helper is re-defined as
     * public in the signal types where a clear operation makes sense.
     *
     * @return the created signal operation instance, not <code>null</code>
     */
    protected SignalOperation<Void> clear() {
        return submit(new SignalCommand.ClearCommand(Id.random(), id()));
    }

    /**
     * Helper to submit a remove command. This is a helper is re-defined as
     * public in the signal types where a remove operation makes sense.
     *
     * @param child
     *            the child signal to remove, not <code>null</code>
     *
     * @return the created signal operation instance, not <code>null</code>
     */
    protected SignalOperation<Void> remove(AbstractSignal<?> child) {
        return submit(
                new SignalCommand.RemoveCommand(Id.random(), child.id(), id()));
    }

    /**
     * Helper to convert the given object to JSON using the global signal object
     * mapper.
     *
     * @see SignalEnvironment
     *
     * @param value
     *            the object to convert to JSON
     * @return the converted JSON node, not <code>null</code>
     */
    protected static JsonNode toJson(Object value) {
        return OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * Helper to convert the given JSON to a Java instance of the given type
     * using the global signal object mapper.
     *
     * @see SignalEnvironment
     *
     * @param <T>
     *            the target type
     * @param value
     *            the JSON value to convert
     * @param targetType
     *            the target type, not <code>null</code>
     * @return the converted Java instance
     */
    protected static <T> T fromJson(JsonNode value, Class<T> targetType) {
        try {
            return OBJECT_MAPPER.treeToValue(value, targetType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper to convert the value of the given node into Java object of the
     * given type.
     *
     * @param <T>
     *            the Java object type
     * @param node
     *            the signal node to read the value from, not <code>null</code>
     * @param valueType
     *            the type to convert to, not <code>null</code>
     * @return the converted Java instance
     */
    protected static <T> T nodeValue(Node node, Class<T> valueType) {
        assert node instanceof Data;

        return fromJson(((Data) node).value(), valueType);
    }

    /*
     * These are a bunch of public access points to API that is originally
     * implemented in classes in the .impl package. These methods are unit
     * tested from the same class that tests the implementation class.
     */

    /**
     * Creates a computed signal based on a mapper function that is passed the
     * value of this signal. If the mapper function accesses other signal
     * values, then the computed signal will also depend on those signals.
     *
     * @param <C>
     *            the computed signal type
     * @param mapper
     *            the mapper function to use, not <code>null</code>
     * @return the computed signal, not <code>null</code>
     */
    public <C> Signal<C> map(Function<T, C> mapper) {
        return Signal.computed(() -> mapper.apply(value()));
    }
}
