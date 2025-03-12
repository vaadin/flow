package com.vaadin.signals.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.vaadin.signals.Id;
import com.vaadin.signals.SignalCommand;

/**
 * A list of signal commands together with their result handlers.
 */
public class CommandsAndHandlers {
    private final List<SignalCommand> commands = new ArrayList<>();
    private final Map<Id, Consumer<CommandResult>> resultHandlers = new HashMap<>();

    /**
     * Creates a new empty command list.
     */
    public CommandsAndHandlers() {

    }

    /**
     * Creates a new command list with the given commands and result handlers.
     *
     * @param commands
     *            the commands to use, not <code>null</code>
     * @param resultHandlers
     *            the result handlers to use, not <code>null</code>
     */
    public CommandsAndHandlers(List<SignalCommand> commands,
            Map<Id, Consumer<CommandResult>> resultHandlers) {
        this.commands.addAll(commands);
        this.resultHandlers.putAll(resultHandlers);
    }

    /**
     * Creates a new command list with a single command and optional result
     * handler.
     *
     * @param command
     *            the command to use, not <code>null</code>
     * @param resultHandler
     *            the result handler to use, or <code>null</code> to not use a
     *            result handler
     */
    public CommandsAndHandlers(SignalCommand command,
            Consumer<CommandResult> resultHandler) {
        assert command != null;
        commands.add(command);
        if (resultHandler != null) {
            resultHandlers.put(command.commandId(), resultHandler);
        }
    }

    /**
     * Removes commands based on a collection of handled commands. Note that the
     * corresponding result handlers are not removed but there's instead an
     * assumption that the caller will invoke {@link #notifyResultHandlers(Map)}
     * separately.
     *
     * @param handledCommandIds
     *            a collection of handled commands ids, not <code>null</code>
     */
    public void removeHandledCommands(Collection<Id> handledCommandIds) {
        commands.removeIf(
                command -> handledCommandIds.contains(command.commandId()));
    }

    /**
     * Notifies and removes result handlers for the given results.
     *
     * @param results
     *            a map of command results, not <code>null</code>
     */
    public void notifyResultHandlers(Map<Id, CommandResult> results) {
        notifyResultHandlers(results, commands);
    }

    /**
     * Notifies and removes result handlers for the given results in the given
     * order. Commands in the order that have no corresponding result are
     * ignored.
     *
     * @param results
     *            the map of command results, not <code>null</code>
     * @param commandOrder
     *            a list of commands in the order the results should be applied.
     */
    public void notifyResultHandlers(Map<Id, CommandResult> results,
            List<SignalCommand> commandOrder) {
        for (SignalCommand command : commandOrder) {
            if (command instanceof SignalCommand.TransactionCommand tx) {
                notifyResultHandlers(results, tx.commands());
            }
            Consumer<CommandResult> handler = resultHandlers
                    .remove(command.commandId());
            if (handler != null) {
                handler.accept(results.get(command.commandId()));
            }
        }
    }

    /**
     * Gets an unmodifiable view of the commands.
     *
     * @return an unmodifiable list of commands, not <code>null</code>
     */
    public List<SignalCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    /**
     * Gets an unmodifiable map of the result handlers.
     *
     * @return an unmodifiable map of result handlers, not <code>null</code>
     */
    public Map<Id, Consumer<CommandResult>> getResultHandlers() {
        return Collections.unmodifiableMap(resultHandlers);
    }

    /**
     * Adds another collection of commands and handlers to this one.
     *
     *
     * @param other
     *            the instance to import entries from, not <code>null</code>
     */
    public void add(CommandsAndHandlers other) {
        this.commands.addAll(other.commands);
        this.resultHandlers.putAll(other.resultHandlers);
    }

    /**
     * Checks whether there are any commands in this list.
     *
     * @return <code>true</code> if there are no commands in this list,
     *         <code>false</code> if there are commands
     */
    public boolean isEmpty() {
        return commands.isEmpty();
    }
}
