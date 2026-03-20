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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.StringNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.TestUtil;
import com.vaadin.flow.signals.function.CommandValidator;
import com.vaadin.flow.signals.shared.SharedNumberSignal;
import com.vaadin.flow.signals.shared.impl.CommandResult.Accept;
import com.vaadin.flow.signals.shared.impl.CommandResult.Reject;
import com.vaadin.flow.signals.shared.impl.SignalTree.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AsynchronousSignalTreeTest extends SignalsUnitTest {

    public static class AsyncTestTree extends AsynchronousSignalTree {
        public List<List<SignalCommand>> submitted = new ArrayList<>();

        @Override
        protected void submit(List<SignalCommand> commands) {
            submitted.add(commands);
        }

        public void confirmSubmitted() {
            var oldSubmitted = submitted;
            submitted = new ArrayList<>();

            oldSubmitted.forEach(this::confirm);
        }

        /**
         * Confirms all pending commands in reverse order on a background thread
         * (where UI.getCurrent() returns null), simulating the virtual thread
         * scheduling race.
         */
        public void confirmSubmittedInReverseOnBackgroundThread()
                throws InterruptedException {
            var oldSubmitted = submitted;
            submitted = new ArrayList<>();

            Thread bgThread = new Thread(() -> {
                for (int i = oldSubmitted.size() - 1; i >= 0; i--) {
                    confirm(oldSubmitted.get(i));
                }
            });
            bgThread.start();
            bgThread.join();
        }
    }

    static class TestSharedNumberSignal extends SharedNumberSignal {
        TestSharedNumberSignal(SignalTree tree, Id id,
                CommandValidator validator) {
            super(tree, id, validator);
        }
    }

    @Test
    void newInstance_type_asynchronous() {
        AsyncTestTree tree = new AsyncTestTree();

        assertEquals(Type.ASYNCHRONOUS, tree.type());
    }

    @Test
    void applyChange_happyPath_submittedThenConfirmed() {
        AsyncTestTree tree = new AsyncTestTree();

        AtomicReference<CommandResult> result = new AtomicReference<>();

        SignalCommand command = TestUtil.writeRootValueCommand();
        tree.commitSingleCommand(command, result::set);

        assertNull(result.get());
        assertNull(TestUtil.readConfirmedRootValue(tree));
        assertNotNull(TestUtil.readSubmittedRootValue(tree));
        assertEquals(List.of(List.of(command)), tree.submitted);

        tree.confirmSubmitted();

        assertInstanceOf(Accept.class, result.get());
        assertNotNull(TestUtil.readConfirmedRootValue(tree));
        assertNotNull(TestUtil.readSubmittedRootValue(tree));
    }

    @Test
    void applyChange_invalidCommand_submittedThenConfirmed() {
        AsyncTestTree tree = new AsyncTestTree();

        AtomicReference<CommandResult> result = new AtomicReference<>();

        SignalCommand command = TestUtil.failingCommand();
        tree.commitSingleCommand(command, result::set);

        /*
         * It might seem weird that an obviously invalid command wouldn't be
         * rejected right away but there is a theoretical possibility that a
         * concurrent command changes the circumstances so that the command
         * would be accepted.
         */
        assertNull(result.get());
        assertEquals(List.of(List.of(command)), tree.submitted);

        tree.confirmSubmitted();

        assertInstanceOf(Reject.class, result.get());
    }

    @Test
    void confirm_externalCommand_applied() {
        AsyncTestTree tree = new AsyncTestTree();

        tree.confirm(List.of(TestUtil.writeRootValueCommand()));

        assertNotNull(TestUtil.readConfirmedRootValue(tree));
        assertNotNull(TestUtil.readSubmittedRootValue(tree));
    }

    @Test
    void confirm_overwritingSubmitted_submittedWins() {
        AsyncTestTree tree = new AsyncTestTree();

        tree.commitSingleCommand(TestUtil.writeRootValueCommand("Submitted"));

        tree.confirm(List.of(TestUtil.writeRootValueCommand("Confirmed")));

        assertEquals(new StringNode("Submitted"),
                TestUtil.readSubmittedRootValue(tree));
        assertEquals(new StringNode("Confirmed"),
                TestUtil.readConfirmedRootValue(tree));

        tree.confirmSubmitted();

        assertEquals(new StringNode("Submitted"),
                TestUtil.readSubmittedRootValue(tree));
        assertEquals(new StringNode("Submitted"),
                TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void subscribeToProcessed_noChanges_doesNotReceive() {
        AsyncTestTree tree = new AsyncTestTree();
        AtomicReference<Map.Entry<SignalCommand, CommandResult>> resultContainer = new AtomicReference<>();

        tree.subscribeToProcessed((event, result) -> resultContainer
                .set(Map.entry(event, result)));

        assertNull(resultContainer.get());
    }

    @Test
    void subscribeToProcessed_changesConfirmed_receives() {
        AsyncTestTree tree = new AsyncTestTree();
        AtomicReference<Map.@Nullable Entry<SignalCommand, CommandResult>> resultContainer = new AtomicReference<>();

        tree.subscribeToProcessed((event, result) -> resultContainer
                .set(Map.entry(event, result)));

        SignalCommand command = TestUtil.writeRootValueCommand("submitted");
        tree.commitSingleCommand(command);

        assertNull(resultContainer.get());

        // Directly confirm another command:
        tree.confirm(List.of(TestUtil.writeRootValueCommand("confirmed")));

        var confirmedResult = resultContainer.get();
        assertNotNull(confirmedResult);
        assertEquals(new StringNode("confirmed"),
                ((SignalCommand.SetCommand) confirmedResult.getKey()).value());

        tree.confirmSubmitted();
        var submittedResult = resultContainer.get();
        assertNotNull(submittedResult);
        assertEquals(new StringNode("submitted"),
                ((SignalCommand.SetCommand) submittedResult.getKey()).value());

        resultContainer.set(null);

        // No new things to confirm, no events to publish:
        tree.confirmSubmitted();
        assertNull(resultContainer.get());
    }

    @Test
    void subscribeToProcessed_failingCommandConfirmed_receives() {
        AsyncTestTree tree = new AsyncTestTree();
        AtomicReference<Map.Entry<SignalCommand, CommandResult>> resultContainer = new AtomicReference<>();

        tree.subscribeToProcessed((event, result) -> resultContainer
                .set(Map.entry(event, result)));

        SignalCommand command = TestUtil.failingCommand();
        tree.commitSingleCommand(command);

        assertNull(resultContainer.get());

        tree.confirmSubmitted();

        assertEquals(command, resultContainer.get().getKey());
    }

    @Test
    void outOfOrderConfirm_effectRecoversAfterRunPendingAccessTasks()
            throws InterruptedException {
        AsyncTestTree tree = new AsyncTestTree();

        TestSharedNumberSignal signal = new TestSharedNumberSignal(tree,
                Id.ZERO, CommandValidator.ACCEPT_ALL);
        signal.set(100.0);

        Element element = new Element("input");
        UI.getCurrent().getElement().appendChild(element);
        element.bindAttribute("max", signal.map(Object::toString));
        assertEquals("100.0", element.getAttribute("max"));

        signal.set(150.5);
        assertEquals("150.5", element.getAttribute("max"));

        // Confirm in reverse order on a background thread where
        // UI.getCurrent() is null, causing the effect's re-validation
        // to be dispatched via ui.access().
        tree.confirmSubmittedInReverseOnBackgroundThread();

        // Drain the pending access queue so the deferred effect runs.
        VaadinService.getCurrent()
                .runPendingAccessTasks(VaadinSession.getCurrent());

        // After draining, the effect has recovered its observer.
        signal.set(200.0);
        assertEquals("200.0", element.getAttribute("max"));
    }
}
