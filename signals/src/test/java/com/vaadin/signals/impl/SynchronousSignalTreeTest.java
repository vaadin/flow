package com.vaadin.signals.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vaadin.signals.Id;
import com.vaadin.signals.ListSignal.ListPosition;
import com.vaadin.signals.Node;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.impl.CommandResult.Accept;
import com.vaadin.signals.impl.CommandResult.Reject;
import com.vaadin.signals.impl.SignalTree.PendingCommit;
import com.vaadin.signals.impl.SignalTree.Type;

public class SynchronousSignalTreeTest {

    @Test
    void constructor_false_regularSyncTree() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        assertEquals(Type.SYNCHRONOUS, tree.type());
    }

    @Test
    void constructor_true_computedTree() {
        SynchronousSignalTree tree = new SynchronousSignalTree(true);

        assertEquals(Type.COMPUTED, tree.type());
    }

    @Test
    void hasLock_newInstance_notLocked() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        assertFalse(tree.hasLock());
    }

    @Test
    void getWithLock_usesTheLock() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Boolean hasLock = tree.getWithLock(() -> {
            return tree.hasLock();
        });

        assertTrue(hasLock);
    }

    @Test
    void runWithLock_usesTheLock() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        AtomicBoolean hasLock = new AtomicBoolean();

        tree.runWithLock(() -> {
            hasLock.set(tree.hasLock());
        });

        assertTrue(hasLock.get());
    }

    @Test
    void wrapWithLock_usesTheLock() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        AtomicBoolean hasLock = new AtomicBoolean();

        Runnable wrapped = tree.wrapWithLock(() -> {
            hasLock.set(tree.hasLock());
        });

        assertFalse(hasLock.get());

        wrapped.run();

        assertTrue(hasLock.get());
    }

    @Test
    void emptyTree_noChanges_hasEmptyRootNode() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Map<Id, Node> nodes = tree.confirmed().nodes();
        assertEquals(1, nodes.size());

        Data root = (Data) nodes.get(Id.ZERO);
        assertNotNull(root);

        assertNull(root.value());
        assertEquals(0, root.listChildren().size());
        assertEquals(0, root.mapChildren().size());
    }

    @Test
    void commit_withoutLock_throws() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        assertThrows(AssertionError.class, () -> {
            tree.prepareCommit(new CommandsAndHandlers());
        });
    }

    @Test
    void commit_acceptableCommand_changeAppliedAndPublished() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        tree.getLock().lock();

        AtomicReference<CommandResult> result = new AtomicReference<>();

        PendingCommit commit = tree.prepareCommit(new CommandsAndHandlers(
                TestUtil.writeRootValueCommand(), result::set));

        assertNull(TestUtil.readConfirmedRootValue(tree));
        assertNull(result.get());

        assertTrue(commit.canCommit());

        commit.applyChanges();

        assertNotNull(TestUtil.readConfirmedRootValue(tree));
        assertSame(tree.confirmed(), tree.submitted());
        assertNull(result.get());

        commit.publishChanges();
        assertInstanceOf(Accept.class, result.get());
    }

    @Test
    void commit_failingCommand_cannotCommitAndErrorReported() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        tree.getLock().lock();

        AtomicReference<CommandResult> result = new AtomicReference<>();

        PendingCommit commit = tree.prepareCommit(new CommandsAndHandlers(
                TestUtil.failingCommand(), result::set));

        assertNull(result.get());
        assertFalse(commit.canCommit());

        commit.markAsAborted();

        assertInstanceOf(Reject.class, result.get());
    }

    @Test
    void commit_failingCommand_applyAndPublishTrows() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        tree.getLock().lock();

        PendingCommit commit = tree.prepareCommit(
                new CommandsAndHandlers(TestUtil.failingCommand(), ignore -> {
                }));

        assertThrows(AssertionError.class, () -> {
            commit.applyChanges();
        });

        assertThrows(AssertionError.class, () -> {
            commit.publishChanges();
        });
    }

    @Test
    void apply_multipleAcceptableCommands_allApplied() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        tree.getLock().lock();

        Id a = Id.random();
        AtomicReference<CommandResult> aResult = new AtomicReference<>();
        Id b = Id.random();
        AtomicReference<CommandResult> bResult = new AtomicReference<>();

        PendingCommit commit = tree.prepareCommit(new CommandsAndHandlers(
                List.of(new SignalCommand.SetCommand(a, Id.ZERO,
                        new DoubleNode(41)),
                        new SignalCommand.IncrementCommand(b, Id.ZERO, 1)),
                Map.of(a, aResult::set, b, bResult::set)));

        commit.applyChanges();
        commit.publishChanges();

        assertInstanceOf(Accept.class, aResult.get());
        assertInstanceOf(Accept.class, bResult.get());
        assertEquals(new DoubleNode(42), TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void apply_secondCommandInvalidBecauseOfFirst_noneApplied() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        tree.getLock().lock();

        Id a = Id.random();
        AtomicReference<CommandResult> aResult = new AtomicReference<>();
        Id b = Id.random();
        AtomicReference<CommandResult> bResult = new AtomicReference<>();

        PendingCommit commit = tree.prepareCommit(new CommandsAndHandlers(
                List.of(new SignalCommand.SetCommand(a, Id.ZERO,
                        new TextNode("text")),
                        new SignalCommand.IncrementCommand(b, Id.ZERO, 1)),
                Map.of(a, aResult::set, b, bResult::set)));

        assertFalse(commit.canCommit());

        commit.markAsAborted();

        assertEquals("Transaction aborted", ((Reject) aResult.get()).reason());
        assertEquals("Value is not numeric", ((Reject) bResult.get()).reason());
        assertNull(TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void applyChange_goodCommand_applied() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        tree.commitSingleCommand(TestUtil.writeRootValueCommand());

        assertNotNull(TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void applyChange_badCommnad_errorReported() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        AtomicReference<CommandResult> result = new AtomicReference<>();

        tree.commitSingleCommand(TestUtil.failingCommand(), result::set);

        assertInstanceOf(Reject.class, result.get());
    }

    @Test
    void applyChange_multipleChanges_allApplied() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        tree.commitSingleCommand(new SignalCommand.SetCommand(Id.random(),
                Id.ZERO, new DoubleNode(41)));
        tree.commitSingleCommand(
                new SignalCommand.IncrementCommand(Id.random(), Id.ZERO, 1));

        assertNotNull(TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void observe_multipleChanges_invokedOnce() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        AtomicInteger count = new AtomicInteger();

        tree.observeNextChange(Id.ZERO, count::incrementAndGet);

        tree.commitSingleCommand(new SignalCommand.SetCommand(Id.random(),
                Id.ZERO, new DoubleNode(2)));

        assertEquals(1, count.get());

        tree.commitSingleCommand(new SignalCommand.SetCommand(Id.random(),
                Id.ZERO, new DoubleNode(3)));

        assertEquals(1, count.get());
    }

    @Test
    void observe_cancelled_notInvoked() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Runnable canceler = tree.observeNextChange(Id.ZERO, Assertions::fail);
        canceler.run();

        tree.commitSingleCommand(new SignalCommand.SetCommand(Id.random(),
                Id.ZERO, new DoubleNode(2)));
    }

    @Test
    void observe_otherNodeChanged_notInvoked() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Id child = Id.random();
        tree.commitSingleCommand(new SignalCommand.InsertCommand(child, Id.ZERO,
                null, null, ListPosition.first()));

        tree.observeNextChange(Id.ZERO, Assertions::fail);

        tree.commitSingleCommand(new SignalCommand.SetCommand(Id.random(),
                child, new TextNode("value")));
    }

    @Test
    void observe_observeInCallback_registeredAgain() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        AtomicInteger count = new AtomicInteger();

        tree.observeNextChange(Id.ZERO, () -> {
            tree.observeNextChange(Id.ZERO, () -> {
                count.incrementAndGet();
            });
        });

        tree.commitSingleCommand(new SignalCommand.SetCommand(Id.random(),
                Id.ZERO, new DoubleNode(2)));

        assertEquals(0, count.get());

        tree.commitSingleCommand(new SignalCommand.SetCommand(Id.random(),
                Id.ZERO, new DoubleNode(3)));

        assertEquals(1, count.get());
    }

}
