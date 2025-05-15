package com.vaadin.signals.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.TextNode;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.impl.CommandResult.Accept;
import com.vaadin.signals.impl.CommandResult.Reject;
import com.vaadin.signals.impl.SignalTree.Type;

public class AsynchronousSignalTreeTest {

    public static class AsyncTestTree extends AsynchronousSignalTree {
        List<List<SignalCommand>> submitted = new ArrayList<>();

        @Override
        protected void submit(List<SignalCommand> commands) {
            submitted.add(commands);
        }

        public void confirmSubmitted() {
            var oldSubmitted = submitted;
            submitted = new ArrayList<>();

            oldSubmitted.forEach(this::confirm);
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

        assertEquals(new TextNode("Submitted"),
                TestUtil.readSubmittedRootValue(tree));
        assertEquals(new TextNode("Confirmed"),
                TestUtil.readConfirmedRootValue(tree));

        tree.confirmSubmitted();

        assertEquals(new TextNode("Submitted"),
                TestUtil.readSubmittedRootValue(tree));
        assertEquals(new TextNode("Submitted"),
                TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void subscribeToProcessed_noChanges_doesNotReceive() {
        AsyncTestTree tree = new AsyncTestTree();
        AtomicReference<SignalCommand> resultContainer = new AtomicReference<>();

        tree.subscribeToProcessed(resultContainer::set);

        assertNull(resultContainer.get());
    }

    @Test
    void subscribeToProcessed_changesConfirmed_receives() {
        AsyncTestTree tree = new AsyncTestTree();
        AtomicReference<SignalCommand> resultContainer = new AtomicReference<>();

        tree.subscribeToProcessed(resultContainer::set);

        SignalCommand command = TestUtil.writeRootValueCommand("submitted");
        tree.commitSingleCommand(command);

        assertNull(resultContainer.get());

        // Directly confirm another command:
        tree.confirm(List.of(TestUtil.writeRootValueCommand("confirmed")));

        assertEquals(new TextNode("confirmed"),
                ((SignalCommand.SetCommand) resultContainer.get()).value());

        tree.confirmSubmitted();
        assertEquals(new TextNode("submitted"),
                ((SignalCommand.SetCommand) resultContainer.get()).value());

        resultContainer.set(null);

        // No new things to confirm, no events to publish:
        tree.confirmSubmitted();
        assertNull(resultContainer.get());
    }

    @Test
    void subscribeToProcessed_failingCommandConfirmed_receives() {
        AsyncTestTree tree = new AsyncTestTree();
        AtomicReference<SignalCommand> resultContainer = new AtomicReference<>();

        tree.subscribeToProcessed(resultContainer::set);

        SignalCommand command = TestUtil.failingCommand();
        tree.commitSingleCommand(command);

        assertNull(resultContainer.get());

        tree.confirmSubmitted();

        assertEquals(command, resultContainer.get());
    }
}
