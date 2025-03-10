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

public class DirectSignalTreeTest {

    @Test
    void constructor_false_regularDirectTree() {
        DirectSignalTree tree = new DirectSignalTree(false);

        assertEquals(Type.DIRECT, tree.type());
    }

    @Test
    void constructor_true_computedTree() {
        DirectSignalTree tree = new DirectSignalTree(true);

        assertEquals(Type.COMPUTED, tree.type());
    }

    @Test
    void hasLock_newInstance_notLocked() {
        DirectSignalTree tree = new DirectSignalTree(false);

        assertFalse(tree.hasLock());
    }

    @Test
    void getWithLock_usesTheLock() {
        DirectSignalTree tree = new DirectSignalTree(false);

        Boolean hasLock = tree.getWithLock(() -> {
            return tree.hasLock();
        });

        assertTrue(hasLock);
    }

    @Test
    void runWithLock_usesTheLock() {
        DirectSignalTree tree = new DirectSignalTree(false);
        AtomicBoolean hasLock = new AtomicBoolean();

        tree.runWithLock(() -> {
            hasLock.set(tree.hasLock());
        });

        assertTrue(hasLock.get());
    }

    @Test
    void wrapWithLock_usesTheLock() {
        DirectSignalTree tree = new DirectSignalTree(false);
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
        DirectSignalTree tree = new DirectSignalTree(false);

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
        DirectSignalTree tree = new DirectSignalTree(false);

        assertThrows(AssertionError.class, () -> {
            tree.prepareCommit(new CommandsAndHandlers());
        });
    }

    @Test
    void commit_acceptableCommand_changeAppliedAndPublished() {
        DirectSignalTree tree = new DirectSignalTree(false);
        tree.getLock().lock();

        AtomicReference<CommandResult> result = new AtomicReference<>();

        PendingCommit commit = tree.prepareCommit(new CommandsAndHandlers(
                TestUtil.rootValueCommand(), result::set));

        assertNull(TestUtil.confirmedRootValue(tree));
        assertNull(result.get());

        assertTrue(commit.canCommit());

        commit.applyChanges();

        assertNotNull(TestUtil.confirmedRootValue(tree));
        assertSame(tree.confirmed(), tree.submitted());
        assertNull(result.get());

        commit.publishChanges();
        assertInstanceOf(Accept.class, result.get());
    }

    @Test
    void commit_failingCommand_cannotCommitAndErrorReported() {
        DirectSignalTree tree = new DirectSignalTree(false);
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
        DirectSignalTree tree = new DirectSignalTree(false);
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
        DirectSignalTree tree = new DirectSignalTree(false);
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
        assertEquals(new DoubleNode(42), TestUtil.confirmedRootValue(tree));
    }

    @Test
    void apply_secondCommandInvalidBecauseOfFirst_noneApplied() {
        DirectSignalTree tree = new DirectSignalTree(false);
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
        assertNull(TestUtil.confirmedRootValue(tree));
    }

    @Test
    void applyChange_goodCommand_applied() {
        DirectSignalTree tree = new DirectSignalTree(false);

        tree.applyChange(TestUtil.rootValueCommand());

        assertNotNull(TestUtil.confirmedRootValue(tree));
    }

    @Test
    void applyChange_badCommnad_errorReported() {
        DirectSignalTree tree = new DirectSignalTree(false);
        AtomicReference<CommandResult> result = new AtomicReference<>();

        tree.applyChange(TestUtil.failingCommand(), result::set);

        assertInstanceOf(Reject.class, result.get());
    }

    @Test
    void applyChange_multipleChanges_allApplied() {
        DirectSignalTree tree = new DirectSignalTree(false);

        tree.applyChange(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new DoubleNode(41)));
        tree.applyChange(
                new SignalCommand.IncrementCommand(Id.random(), Id.ZERO, 1));

        assertNotNull(TestUtil.confirmedRootValue(tree));
    }

    @Test
    void depend_multipleChanges_invokedOnce() {
        DirectSignalTree tree = new DirectSignalTree(false);
        AtomicInteger count = new AtomicInteger();

        tree.depend(Id.ZERO, count::incrementAndGet);

        tree.applyChange(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new DoubleNode(2)));

        assertEquals(1, count.get());

        tree.applyChange(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new DoubleNode(3)));

        assertEquals(1, count.get());
    }

    @Test
    void depend_cancelled_notInvoked() {
        DirectSignalTree tree = new DirectSignalTree(false);

        Runnable canceler = tree.depend(Id.ZERO, Assertions::fail);
        canceler.run();

        tree.applyChange(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new DoubleNode(2)));
    }

    @Test
    void depend_otherNodeChanged_notInvoked() {
        DirectSignalTree tree = new DirectSignalTree(false);

        Id child = Id.random();
        tree.applyChange(new SignalCommand.InsertCommand(child, Id.ZERO, null,
                null, ListPosition.first()));

        tree.depend(Id.ZERO, Assertions::fail);

        tree.applyChange(new SignalCommand.SetCommand(Id.random(), child,
                new TextNode("value")));
    }

    @Test
    void depend_dependInCallback_registeredAgain() {
        DirectSignalTree tree = new DirectSignalTree(false);

        AtomicInteger count = new AtomicInteger();

        tree.depend(Id.ZERO, () -> {
            tree.depend(Id.ZERO, () -> {
                count.incrementAndGet();
            });
        });

        tree.applyChange(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new DoubleNode(2)));

        assertEquals(0, count.get());

        tree.applyChange(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new DoubleNode(3)));

        assertEquals(1, count.get());
    }

}
