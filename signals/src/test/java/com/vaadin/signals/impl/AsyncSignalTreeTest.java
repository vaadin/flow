package com.vaadin.signals.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.TextNode;
import com.vaadin.signals.Id;
import com.vaadin.signals.ListSignal.ListPosition;
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

    @Test
    void isPinned_newTree_notPinned() {
        AsyncTestTree tree = new AsyncTestTree();

        assertFalse(tree.isPinned());
    }

    @Test
    void isPinned_unconfirmedCommand_pinnedUntilConfirmed() {
        AsyncTestTree tree = new AsyncTestTree();

        tree.applyChange(TestUtil.rootValueCommand("value"));
        assertTrue(tree.isPinned());

        // Confirm an unrelated command
        tree.confirm(List.of(TestUtil.rootValueCommand("unrelated")));
        assertTrue(tree.isPinned());

        tree.confirmSubmitted();
        assertFalse(tree.isPinned());
    }

    @Test
    void isPinned_partialConfirm_unpinnedAfterFullConfirm() {
        AsyncTestTree tree = new AsyncTestTree();

        tree.applyChange(TestUtil.rootValueCommand());
        tree.applyChange(TestUtil.rootValueCommand());

        tree.confirm(tree.submitted.remove(0));
        assertTrue(tree.isPinned());

        tree.confirm(tree.submitted.remove(0));
        assertFalse(tree.isPinned());
    }

    @Test
    void pin_pinAndUnpin_pinnedUntilUnpinned() {
        AsyncTestTree tree = new AsyncTestTree();

        Runnable unpin = tree.pin();
        assertTrue(tree.isPinned());

        unpin.run();
        assertFalse(tree.isPinned());
    }

    @Test
    void pin_multiplePinned_pinnedUntilAllUnpinned() {
        AsyncTestTree tree = new AsyncTestTree();

        Runnable pin1 = tree.pin();
        Runnable pin2 = tree.pin();
        Runnable pin3 = tree.pin();

        pin1.run();
        pin3.run();
        assertTrue(tree.isPinned());

        pin2.run();
        assertFalse(tree.isPinned());
    }

    @Test
    void pin_unpinMultipleTimes_countedOnce() {
        AsyncTestTree tree = new AsyncTestTree();

        Runnable pin1 = tree.pin();
        Runnable pin2 = tree.pin();

        pin1.run();
        pin1.run();
        assertTrue(tree.isPinned());

        pin2.run();
        assertFalse(tree.isPinned());
    }

    @Test
    void onPinStatusChanged_pinAndUnpin_runWithLockAtEdge() {
        List<Boolean> pinStatusChanges = new ArrayList<>();

        AsyncTestTree tree = new AsyncTestTree() {
            @Override
            protected void onPinStatusChanged(boolean pinned) {
                super.onPinStatusChanged(pinned);
                pinStatusChanges.add(pinned);
                assertTrue(hasLock());
            }
        };
        assertEquals(List.of(), pinStatusChanges);

        Runnable pin = tree.pin();
        assertEquals(List.of(true), pinStatusChanges);

        Runnable pin2 = tree.pin();
        assertEquals(List.of(true), pinStatusChanges);

        pin.run();
        assertEquals(List.of(true), pinStatusChanges);
        pin.run();
        assertEquals(List.of(true), pinStatusChanges);

        pin2.run();
        assertEquals(List.of(true, false), pinStatusChanges);
    }

    @Test
    void onPinStatusChanged_pinWhenUnpinned_pinnedAgain() {
        AtomicBoolean pinAgain = new AtomicBoolean(true);
        List<Runnable> pins = new ArrayList<>();

        AsyncTestTree tree = new AsyncTestTree() {
            @Override
            protected void onPinStatusChanged(boolean pinned) {
                super.onPinStatusChanged(pinned);

                if (!pinned && pinAgain.get()) {
                    pins.add(pin());
                }
            }
        };

        Runnable original = tree.pin();
        assertEquals(List.of(), pins);

        original.run();
        assertTrue(tree.isPinned());
        assertEquals(1, pins.size());

        pinAgain.set(false);
        pins.remove(0).run();
        assertFalse(tree.isPinned());
        assertEquals(List.of(), pins);
    }

    @Test
    void pin_scopeOwnerNodePresent_nodeRemovedAndAddedBack() {
        AsyncTestTree tree = new AsyncTestTree();

        Runnable pin = tree.pin();

        SignalCommand.InsertCommand insert = new SignalCommand.InsertCommand(
                Id.random(), Id.ZERO, tree.id(), new TextNode("value"),
                ListPosition.last());
        tree.applyChange(insert);
        tree.confirmSubmitted();
        assertEquals(Map.of(insert.commandId(), insert),
                tree.confirmed().originalInserts());
        assertEquals(1,
                tree.confirmed().data(Id.ZERO).get().listChildren().size());

        pin.run();
        assertFalse(false,
                "The pending ClearOwnerCommand should not pin the tree");
        assertEquals(List.of(),
                tree.submitted().data(Id.ZERO).get().listChildren());

        tree.pin();
        tree.confirmSubmitted();

        assertEquals(1,
                tree.confirmed().data(Id.ZERO).get().listChildren().size());
        assertEquals(Map.of(insert.commandId(), insert),
                tree.confirmed().originalInserts());
    }
}
