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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.Id;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.SignalCommand.TransactionCommand;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.impl.CommandResult.Accept;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandsAndHandlersTest {

    @Test
    void constructor_emptyInstance_isEmpty() {
        CommandsAndHandlers commands = new CommandsAndHandlers();

        assertTrue(commands.isEmpty());
        assertEquals(List.of(), commands.getCommands());
        assertEquals(Map.of(), commands.getResultHandlers());
    }

    @Test
    void contructor_singleCommandNoHandler_hasCommandButNoHandler() {
        SignalCommand command = TestUtil.writeRootValueCommand();

        CommandsAndHandlers commands = new CommandsAndHandlers(command, null);

        assertFalse(commands.isEmpty());
        assertEquals(List.of(command), commands.getCommands());
        assertEquals(Map.of(), commands.getResultHandlers());
    }

    @Test
    void constructor_singleCommandAndHandler_hasCommandAndHandler() {
        SignalCommand command = TestUtil.writeRootValueCommand();
        Consumer<CommandResult> handler = dummyHandler();

        CommandsAndHandlers commands = new CommandsAndHandlers(command,
                handler);

        assertFalse(commands.isEmpty());
        assertEquals(List.of(command), commands.getCommands());
        assertEquals(Map.of(command.commandId(), handler),
                commands.getResultHandlers());
    }

    @Test
    void constructor_multipleCommandsAndHandlers_hasCopiesOfEverything() {
        SignalCommand c1 = TestUtil.writeRootValueCommand();
        SignalCommand c2 = TestUtil.writeRootValueCommand();
        Consumer<CommandResult> handler = dummyHandler();

        List<SignalCommand> list = Arrays.asList(c1, c2);
        Map<Id, Consumer<CommandResult>> map = new HashMap<>();
        map.put(c1.commandId(), handler);

        CommandsAndHandlers commands = new CommandsAndHandlers(list, map);

        list.set(0, null);
        map.clear();

        assertEquals(List.of(c1, c2), commands.getCommands());
        assertEquals(Map.of(c1.commandId(), handler),
                commands.getResultHandlers());
    }

    @Test
    void removeHandledCommands_commandsInList_commandsRemovedHandlersRetained() {
        SignalCommand c1 = TestUtil.writeRootValueCommand();
        SignalCommand c2 = TestUtil.writeRootValueCommand();
        SignalCommand c3 = TestUtil.writeRootValueCommand();
        Consumer<CommandResult> handler = dummyHandler();

        CommandsAndHandlers commands = new CommandsAndHandlers(
                List.of(c1, c2, c3), Map.of(c2.commandId(), handler));

        commands.removeHandledCommands(List.of(c2.commandId()));

        assertEquals(List.of(c1, c3), commands.getCommands());
        assertEquals(Map.of(c2.commandId(), handler),
                commands.getResultHandlers());
    }

    @Test
    void removeHandledCommands_commandsNotInList_noChange() {
        CommandsAndHandlers commands = new CommandsAndHandlers(
                TestUtil.writeRootValueCommand(), dummyHandler());

        commands.removeHandledCommands(List.of(Id.random()));

        assertEquals(1, commands.getCommands().size());
        assertEquals(1, commands.getResultHandlers().size());
    }

    @Test
    void notifyResultHandlers_handlerPresent_handlerInvokedAndRemoved() {
        SignalCommand c1 = TestUtil.writeRootValueCommand();
        ResultHandler h1 = new ResultHandler();

        CommandsAndHandlers commands = new CommandsAndHandlers(c1, h1);

        Accept result = CommandResult.ok();
        commands.notifyResultHandlers(Map.of(c1.commandId(), result));

        assertEquals(result, h1.result);
        assertEquals(Map.of(), commands.getResultHandlers());
    }

    @Test
    void notifyResultHandlers_multipleResults_invokedInOrder() {
        SignalCommand c1 = TestUtil.writeRootValueCommand();
        SignalCommand c2 = TestUtil.writeRootValueCommand();
        Accept r1 = CommandResult.ok();
        Accept r2 = CommandResult.ok();

        List<CommandResult> results = new ArrayList<>();

        // Defining everything else with 2 before 1 but notifies with 1 first
        CommandsAndHandlers commands = new CommandsAndHandlers(List.of(c2, c1),
                Map.of(c2.commandId(), result -> {
                    assertSame(r2, result);
                    results.add(result);
                }, c1.commandId(), result -> {
                    assertSame(r1, result);
                    results.add(result);
                }));

        commands.notifyResultHandlers(
                Map.of(c2.commandId(), r2, c1.commandId(), r1),
                List.of(c1, c2));

        assertEquals(List.of(r1, r2), results);
    }

    @Test
    void notifyResultHandlers_nestedTransactionCommands_childHandlersInvoked() {
        SignalCommand c1 = TestUtil.writeRootValueCommand();
        TransactionCommand tx1 = new SignalCommand.TransactionCommand(
                Id.random(), List.of(c1));
        TransactionCommand tx2 = new SignalCommand.TransactionCommand(
                Id.random(), List.of(tx1));

        Accept r1 = CommandResult.ok();
        Accept r2 = CommandResult.ok();
        Accept r3 = CommandResult.ok();

        ResultHandler h1 = new ResultHandler();
        ResultHandler h2 = new ResultHandler();
        ResultHandler h3 = new ResultHandler();

        CommandsAndHandlers commands = new CommandsAndHandlers(List.of(tx2),
                Map.of(c1.commandId(), h1, tx1.commandId(), h2, tx2.commandId(),
                        h3));

        commands.notifyResultHandlers(Map.of(c1.commandId(), r1,
                tx1.commandId(), r2, tx2.commandId(), r3));

        assertEquals(r1, h1.result);
        assertEquals(r2, h2.result);
        assertEquals(r3, h3.result);
    }

    @Test
    void add_otherCommands_added() {
        SignalCommand c1 = TestUtil.writeRootValueCommand();
        SignalCommand c2 = TestUtil.writeRootValueCommand();
        Consumer<CommandResult> h1 = dummyHandler();
        Consumer<CommandResult> h2 = dummyHandler();

        CommandsAndHandlers commands = new CommandsAndHandlers(c1, h1);
        commands.add(new CommandsAndHandlers(c2, h2));

        assertEquals(List.of(c1, c2), commands.getCommands());
        assertEquals(Map.of(c1.commandId(), h1, c2.commandId(), h2),
                commands.getResultHandlers());
    }

    static class ResultHandler implements Consumer<CommandResult> {
        CommandResult result;

        @Override
        public void accept(CommandResult result) {
            assertNull(this.result);
            this.result = result;
        }
    }

    private static Consumer<CommandResult> dummyHandler() {
        return ignore -> {
        };
    }
}
