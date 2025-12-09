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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.StringNode;

import com.vaadin.signals.Id;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.SignalCommand.TransactionCommand;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.impl.AsynchronousSignalTreeTest.AsyncTestTree;
import com.vaadin.signals.impl.CommandsAndHandlersTest.ResultHandler;
import com.vaadin.signals.impl.StagedTransaction.ResultCollector;
import com.vaadin.signals.impl.Transaction.Type;
import com.vaadin.signals.operations.SignalOperation.ResultOrError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StagedTransactionTest {
    /*
     * Note that much of the logic in this test only interacts with API in
     * Transaction and in that way tests logic from StagedTransaction.
     */

    @Test
    void resultCollector_unregisteredDependency_throws() {
        ResultCollector collector = new ResultCollector(List.of(new Object()),
                ignore -> {
                });

        assertThrows(AssertionError.class, () -> {
            collector.registerDependency(new Object());
        });
    }

    @Test
    void resultCollector_allOk_successfulResult() {
        Object d1 = new Object();
        Object d2 = new Object();
        Object d3 = new Object();

        AtomicReference<ResultOrError<Void>> resultHolder = new AtomicReference<>();

        ResultCollector collector = new ResultCollector(List.of(d1, d2, d3),
                resultHolder::set);

        Consumer<CommandResult> c2 = collector.registerDependency(d2);
        Consumer<CommandResult> c3 = collector.registerDependency(d3);
        c2.accept(CommandResult.ok());
        collector.registerDependency(d1).accept(CommandResult.ok());

        assertNull(resultHolder.get());

        c3.accept(CommandResult.ok());

        assertTrue(resultHolder.get().successful());
    }

    @Test
    void resultCollector_oneRejected_unsuccessfulResult() {
        Object d1 = new Object();
        Object d2 = new Object();
        Object d3 = new Object();

        AtomicReference<ResultOrError<Void>> resultHolder = new AtomicReference<>();

        ResultCollector collector = new ResultCollector(List.of(d1, d2, d3),
                resultHolder::set);

        Consumer<CommandResult> c2 = collector.registerDependency(d2);
        Consumer<CommandResult> c3 = collector.registerDependency(d3);
        c2.accept(CommandResult.ok());

        assertNull(resultHolder.get());

        collector.registerDependency(d1).accept(CommandResult.fail("reason"));

        assertFalse(resultHolder.get().successful());

        c3.accept(CommandResult.ok());

        assertFalse(resultHolder.get().successful(),
                "Completing last dependency should not make the result successful");
    }

    @Test
    void resultCollector_resolveTwice_throws() {
        Object d1 = new Object();
        Object d2 = new Object();
        Object d3 = new Object();

        AtomicReference<ResultOrError<Void>> resultHolder = new AtomicReference<>();

        ResultCollector collector = new ResultCollector(List.of(d1, d2, d3),
                resultHolder::set);

        Consumer<CommandResult> c2 = collector.registerDependency(d2);
        c2.accept(CommandResult.ok());

        collector.registerDependency(d1).accept(CommandResult.ok());

        assertThrows(AssertionError.class, () -> {
            c2.accept(CommandResult.ok());
        });
    }

    @Test
    void transaction_goodThenFailingCommands_goodRevertedFromTransactionRead() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Transaction.runInTransaction(() -> {
            Transaction tx = Transaction.getCurrent();

            tx.include(tree, TestUtil.writeRootValueCommand(), null);

            assertNotNull(tx.read(tree).data(Id.ZERO).get().value());

            tx.include(tree, TestUtil.failingCommand(), null);

            assertNull(tx.read(tree).data(Id.ZERO).get().value());
        });
    }

    @Test
    void commit_twoSyncTreesWithGoodCommands_changesApplied()
            throws InterruptedException, ExecutionException {
        SynchronousSignalTree t1 = new SynchronousSignalTree(false);
        SynchronousSignalTree t2 = new SynchronousSignalTree(false);

        ResultHandler h1 = new ResultHandler();
        ResultHandler h2 = new ResultHandler();

        var operation = Transaction.runInTransaction(() -> {
            Transaction tx = Transaction.getCurrent();

            tx.include(t1, TestUtil.writeRootValueCommand(), h1);
            tx.include(t2, TestUtil.writeRootValueCommand(), h2);

            assertNull(h1.result);
            assertNull(h2.result);
        });

        assertTrue(h1.result.accepted());
        assertTrue(h2.result.accepted());

        assertTrue(operation.result().isDone());
        assertTrue(operation.result().get().successful());

        assertNotNull(TestUtil.readConfirmedRootValue(t1));
        assertNotNull(TestUtil.readConfirmedRootValue(t2));
    }

    @Test
    void commit_twoSyncTreesWithOneBadCommand_noChangeApplied()
            throws InterruptedException, ExecutionException {
        SynchronousSignalTree t1 = new SynchronousSignalTree(false);
        SynchronousSignalTree t2 = new SynchronousSignalTree(false);

        ResultHandler h1 = new ResultHandler();
        ResultHandler h2 = new ResultHandler();

        var operation = Transaction.runInTransaction(() -> {
            Transaction tx = Transaction.getCurrent();

            tx.include(t1, TestUtil.writeRootValueCommand(), h1);
            tx.include(t2, TestUtil.failingCommand(), h2);

            assertNull(TestUtil.readTransactionRootValue(t1),
                    "Reading should take expected failure into account");
            assertNull(h1.result);
            assertNull(h2.result);
        });

        assertFalse(h1.result.accepted());
        assertFalse(h2.result.accepted());

        assertTrue(operation.result().isDone());
        assertFalse(operation.result().get().successful());

        assertNull(t1.confirmed().data(Id.ZERO).get().value());
        assertNull(t2.confirmed().data(Id.ZERO).get().value());
    }

    @Test
    void commit_asyncTreeWithGoodCommand_submittedOnCommitAndConfirmedWhenConfirmed()
            throws InterruptedException, ExecutionException {
        AsyncTestTree tree = new AsyncTestTree();
        ResultHandler handler = new ResultHandler();

        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand(), handler);

            assertNull(handler.result);
        });

        assertNull(handler.result);
        assertFalse(operation.result().isDone());
        assertNotNull(tree.submitted().data(Id.ZERO).get().value());
        assertNull(tree.confirmed().data(Id.ZERO).get().value());

        tree.confirmSubmitted();

        assertTrue(handler.result.accepted());
        assertTrue(operation.result().isDone());
        assertTrue(operation.result().get().successful());
        assertNotNull(tree.submitted().data(Id.ZERO).get().value());
        assertNotNull(tree.confirmed().data(Id.ZERO).get().value());
    }

    @Test
    void commit_asyncTreeWithGoodAndBadCommands_nothingChanged()
            throws InterruptedException, ExecutionException {
        AsyncTestTree tree = new AsyncTestTree();

        ResultHandler h1 = new ResultHandler();
        ResultHandler h2 = new ResultHandler();

        var operation = Transaction.runInTransaction(() -> {
            Transaction tx = Transaction.getCurrent();

            tx.include(tree, TestUtil.writeRootValueCommand(), h1);
            tx.include(tree, TestUtil.failingCommand(), h2);

            assertNull(h1.result);
            assertNull(h2.result);
        });

        assertNull(h1.result);
        assertNull(h2.result);
        assertFalse(operation.result().isDone());
        assertNull(tree.submitted().data(Id.ZERO).get().value());
        assertNull(tree.confirmed().data(Id.ZERO).get().value());

        tree.confirmSubmitted();

        assertFalse(h1.result.accepted());
        assertFalse(h1.result.accepted());
        assertTrue(operation.result().isDone());
        assertFalse(operation.result().get().successful());
        assertNull(tree.submitted().data(Id.ZERO).get().value());
        assertNull(tree.confirmed().data(Id.ZERO).get().value());
    }

    @Test
    void commit_outerStagedTransaction_transactionCommandInOuter() {
        AsyncTestTree tree = new AsyncTestTree();
        SignalCommand command = TestUtil.writeRootValueCommand();

        Transaction.runInTransaction(() -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrent().include(tree, command, null);
            });

            assertTrue(tree.submitted.isEmpty());

            assertNotNull(TestUtil.readTransactionRootValue(tree));
        });

        assertEquals(1, tree.submitted.size());
        assertEquals(1, tree.submitted.get(0).size());
        assertInstanceOf(TransactionCommand.class,
                tree.submitted.get(0).get(0));

        TransactionCommand outerTransaction = (TransactionCommand) tree.submitted
                .get(0).get(0);
        assertEquals(1, outerTransaction.commands().size());
        assertInstanceOf(TransactionCommand.class,
                outerTransaction.commands().get(0));

        TransactionCommand innerTransaction = (TransactionCommand) outerTransaction
                .commands().get(0);
        assertEquals(List.of(command), innerTransaction.commands());
    }

    @Test
    void commit_outerWriteThroughTransaction_immediatelyAppliedAndVisible() {
        AsyncTestTree tree = new AsyncTestTree();
        SignalCommand command = TestUtil.writeRootValueCommand();

        Transaction.runInTransaction(() -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrent().include(tree, command, null);
            });

            assertEquals(1, tree.submitted.size());

            assertNotNull(TestUtil.readTransactionRootValue(tree));
        }, Type.WRITE_THROUGH);
    }

    @Test
    void commit_writeThroughInsideStaged_immediatelyAppliedAndVisible() {
        AsyncTestTree tree = new AsyncTestTree();
        SignalCommand command = TestUtil.writeRootValueCommand();

        Transaction.runInTransaction(() -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrent().include(tree, command, null);
            }, Type.WRITE_THROUGH);

            assertEquals(1, tree.submitted.size());

            assertNotNull(TestUtil.readTransactionRootValue(tree));
        });
    }

    @Test
    void commit_failingTransactionSavedBySyncWrite_seemsFailingButIsAccepted()
            throws InterruptedException, ExecutionException {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrent().include(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            new StringNode("expected")),
                    null);
            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand("update"), null);

            // This change is applied outside the transaction and will make it
            // pass
            tree.commitSingleCommand(
                    TestUtil.writeRootValueCommand("expected"));

            // Transaction doesn't "know" about the previous change and "thinks"
            // it will not applied
            assertNull(TestUtil.readTransactionRootValue(tree));
        });

        assertTrue(operation.result().get().successful());
        assertEquals("update",
                TestUtil.readConfirmedRootValue(tree).asString());
    }

    @Test
    void commit_failingTransactionSavedByNestedWriteThroughWrite_seemsFineAndIsAccepted()
            throws InterruptedException, ExecutionException {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrent().include(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            new StringNode("expected")),
                    null);
            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand("update"), null);

            assertNull(TestUtil.readConfirmedRootValue(tree));

            Transaction.runInTransaction(() -> {
                Transaction.getCurrent().include(tree,
                        TestUtil.writeRootValueCommand("expected"), null);
            }, Type.WRITE_THROUGH);

            assertEquals("update",
                    TestUtil.readTransactionRootValue(tree).asString(),
                    "Should take inner transaction changes into account");
        });

        assertTrue(operation.result().get().successful());
        assertEquals("update",
                TestUtil.readConfirmedRootValue(tree).asString());
    }

    @Test
    void commit_goodTransactionRuinedBySyncWrite_seemsFineButFails()
            throws InterruptedException, ExecutionException {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrent().include(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            null),
                    null);
            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand("update"), null);

            tree.commitSingleCommand(
                    TestUtil.writeRootValueCommand("unexpected"));

            assertEquals("update",
                    TestUtil.readTransactionRootValue(tree).asString());
        });

        assertFalse(operation.result().get().successful());
        assertEquals("unexpected",
                TestUtil.readConfirmedRootValue(tree).asString());
    }

    @Test
    void commit_goodTransactionRuinedByNestedWriteThroughWrite_seemsFailingAndFails()
            throws InterruptedException, ExecutionException {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrent().include(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            null),
                    null);
            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand("update"), null);

            Transaction.runInTransaction(() -> {
                Transaction.getCurrent().include(tree,
                        TestUtil.writeRootValueCommand("unexpected"), null);
            }, Type.WRITE_THROUGH);

            assertEquals("unexpected",
                    TestUtil.readTransactionRootValue(tree).asString());
        });

        assertFalse(operation.result().get().successful());
        assertEquals("unexpected",
                TestUtil.readConfirmedRootValue(tree).asString());
    }

    @Test
    void commit_failingTransactionSavedByAsyncConfirm_seemsFailingButIsAccepted()
            throws InterruptedException, ExecutionException {
        AsyncTestTree tree = new AsyncTestTree();

        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrent().include(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            new StringNode("expected")),
                    null);
            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand("update"), null);
        });

        assertNull(TestUtil.readSubmittedRootValue(tree));

        tree.confirm(List.of(TestUtil.writeRootValueCommand("expected")));

        assertEquals("update",
                TestUtil.readSubmittedRootValue(tree).asString());

        tree.confirmSubmitted();

        assertTrue(operation.result().get().successful());
    }

    @Test
    void commit_goodTransactionRuinedByAsyncConfirm_seemsFineButfails()
            throws InterruptedException, ExecutionException {
        AsyncTestTree tree = new AsyncTestTree();
        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrent().include(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            null),
                    null);
            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand("update"), null);
        });

        assertEquals("update",
                TestUtil.readSubmittedRootValue(tree).asString());

        tree.confirm(List.of(TestUtil.writeRootValueCommand("unexpected")));

        assertEquals("unexpected",
                TestUtil.readSubmittedRootValue(tree).asString());

        tree.confirmSubmitted();

        assertFalse(operation.result().get().successful());
    }

    @Test
    void commit_treeWithoutChanges_resultResolved() {
        SynchronousSignalTree t1 = new SynchronousSignalTree(false);
        SynchronousSignalTree t2 = new SynchronousSignalTree(false);

        var operation = Transaction.runInTransaction(() -> {
            TestUtil.readTransactionRootValue(t1);
            Transaction.getCurrent().include(t2,
                    TestUtil.writeRootValueCommand(), null);
        });

        TestUtil.assertSuccess(operation);
    }

    @Test
    void commit_readAndWriteInChangeHandler_bypassesTransaction() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        AtomicReference<String> valueInObserver = new AtomicReference<>();

        tree.observeNextChange(Id.ZERO, immediate -> {
            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand("observer"), null);

            String value = TestUtil.readTransactionRootValue(tree).asString();
            valueInObserver.set(value);

            return false;
        });

        Transaction.runInTransaction(() -> {
            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand("tx"), null);
        });

        assertEquals("observer", valueInObserver.get());
        assertEquals("observer",
                TestUtil.readConfirmedRootValue(tree).asString());
    }

    @Test
    void treeMixing_multipleSyncAndComputed_allIsFine() {
        SignalTree d1 = new SynchronousSignalTree(false);
        SignalTree d2 = new SynchronousSignalTree(false);
        SignalTree c1 = new SynchronousSignalTree(true);
        SignalTree c2 = new SynchronousSignalTree(true);

        Transaction.runInTransaction(() -> {
            TestUtil.readTransactionRootValue(d1);
            TestUtil.readTransactionRootValue(d2);
            TestUtil.readTransactionRootValue(c1);
            TestUtil.readTransactionRootValue(c2);
        });
    }

    @Test
    void treeMixing_singleAsyncAndMultipleComputed_allIsFine() {
        SignalTree a1 = new AsyncTestTree();
        SignalTree c1 = new SynchronousSignalTree(true);
        SignalTree c2 = new SynchronousSignalTree(true);

        // Async first
        Transaction.runInTransaction(() -> {
            TestUtil.readTransactionRootValue(a1);
            TestUtil.readTransactionRootValue(c1);
            TestUtil.readTransactionRootValue(c2);
        });

        // Computed first
        Transaction.runInTransaction(() -> {
            TestUtil.readTransactionRootValue(c1);
            TestUtil.readTransactionRootValue(c2);
            TestUtil.readTransactionRootValue(a1);
        });
    }

    @Test
    void treeMixing_multipleAsync_throws() {
        SignalTree a1 = new AsyncTestTree();
        SignalTree a2 = new AsyncTestTree();

        Transaction.runInTransaction(() -> {
            TestUtil.readTransactionRootValue(a1);

            assertThrows(IllegalStateException.class, () -> {
                TestUtil.readTransactionRootValue(a2);
            });
        });
    }

    @Test
    void treeMixing_asyncAndSync_throws() {
        SignalTree a1 = new AsyncTestTree();
        SignalTree d1 = new SynchronousSignalTree(false);

        // Async first
        Transaction.runInTransaction(() -> {
            TestUtil.readTransactionRootValue(a1);

            assertThrows(IllegalStateException.class, () -> {
                TestUtil.readTransactionRootValue(d1);
            });
        });

        // Sync first
        Transaction.runInTransaction(() -> {
            TestUtil.readTransactionRootValue(d1);

            assertThrows(IllegalStateException.class, () -> {
                TestUtil.readTransactionRootValue(a1);
            });
        });
    }

    @Test
    void commitLocking_multipleTrees_lockOrderConsistent() {
        class LockOrderTree extends SynchronousSignalTree {
            static Set<LockOrderTree> lockedTrees = new HashSet<>();

            private Set<LockOrderTree> previouslyLocked = new HashSet<>();
            private ReentrantLock lockSpy = new ReentrantLock() {
                @Override
                public void lock() {
                    super.lock();
                    previouslyLocked.addAll(lockedTrees);
                    lockedTrees.add(LockOrderTree.this);
                }

                @Override
                public void unlock() {
                    lockedTrees.remove(LockOrderTree.this);
                    super.unlock();
                }
            };

            public LockOrderTree() {
                super(false);
            }

            @Override
            public ReentrantLock getLock() {
                return lockSpy;
            }

            @Override
            protected boolean hasLock() {
                return lockSpy.isHeldByCurrentThread();
            }
        }

        // Explicitly collect to ArrayList to ensure it can be shuffled
        List<LockOrderTree> trees = Stream.generate(LockOrderTree::new)
                .limit(10).collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < 10; i++) {
            Collections.shuffle(trees);

            Transaction.runInTransaction(() -> {
                for (LockOrderTree tree : trees) {
                    Transaction.getCurrent().include(tree,
                            TestUtil.writeRootValueCommand(), null);
                }
            });
        }

        for (LockOrderTree tree : trees) {
            for (LockOrderTree previous : tree.previouslyLocked) {
                assertFalse(previous.previouslyLocked.contains(tree));
            }
        }
    }
}
