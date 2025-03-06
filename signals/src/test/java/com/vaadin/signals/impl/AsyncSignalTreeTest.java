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

public class AsyncSignalTreeTest {

    static class AsyncTestTree extends AsyncSignalTree {
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
    void newInstance_type_async() {
        AsyncTestTree tree = new AsyncTestTree();

        assertEquals(Type.ASYNC, tree.type());
    }

    @Test
    void applyChange_happyPath_submittedThenConfirmed() {
        AsyncTestTree tree = new AsyncTestTree();

        AtomicReference<CommandResult> result = new AtomicReference<>();

        SignalCommand command = TestUtil.rootValueCommand();
        tree.applyChange(command, result::set);

        assertNull(result.get());
        assertNull(TestUtil.confirmedRootValue(tree));
        assertNotNull(TestUtil.submittedRootValue(tree));
        assertEquals(List.of(List.of(command)), tree.submitted);

        tree.confirmSubmitted();

        assertInstanceOf(Accept.class, result.get());
        assertNotNull(TestUtil.confirmedRootValue(tree));
        assertNotNull(TestUtil.submittedRootValue(tree));
    }

    @Test
    void applyChange_invalidCommand_submittedThenConfirmed() {
        AsyncTestTree tree = new AsyncTestTree();

        AtomicReference<CommandResult> result = new AtomicReference<>();

        SignalCommand command = TestUtil.failingCommand();
        tree.applyChange(command, result::set);

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

        tree.confirm(List.of(TestUtil.rootValueCommand()));

        assertNotNull(TestUtil.confirmedRootValue(tree));
        assertNotNull(TestUtil.submittedRootValue(tree));
    }

    @Test
    void confirm_overwritingSubmitted_submittedWins() {
        AsyncTestTree tree = new AsyncTestTree();

        tree.applyChange(TestUtil.rootValueCommand("Submitted"));

        tree.confirm(List.of(TestUtil.rootValueCommand("Confirmed")));

        assertEquals(new TextNode("Submitted"),
                TestUtil.submittedRootValue(tree));
        assertEquals(new TextNode("Confirmed"),
                TestUtil.confirmedRootValue(tree));

        tree.confirmSubmitted();

        assertEquals(new TextNode("Submitted"),
                TestUtil.submittedRootValue(tree));
        assertEquals(new TextNode("Submitted"),
                TestUtil.confirmedRootValue(tree));
    }
}
