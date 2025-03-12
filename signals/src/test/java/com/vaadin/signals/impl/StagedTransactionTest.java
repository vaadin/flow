package com.vaadin.signals.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.fasterxml.jackson.databind.node.TextNode;
import com.vaadin.signals.Id;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.SignalCommand.TransactionCommand;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.impl.AsyncSignalTreeTest.AsyncTestTree;
import com.vaadin.signals.impl.CommandsAndHandlersTest.ResultHandler;
import com.vaadin.signals.impl.StagedTransaction.ResultCollector;
import com.vaadin.signals.impl.Transaction.Type;
import com.vaadin.signals.operations.SignalOperation.ResultOrError;

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
        DirectSignalTree tree = new DirectSignalTree(false);

        Transaction.runInTransaction(() -> {
            Transaction tx = Transaction.getCurrentTransaction();

            tx.apply(tree, TestUtil.rootValueCommand(), null);

            assertNotNull(tx.read(tree).data(Id.ZERO).get().value());

            tx.apply(tree, TestUtil.failingCommand(), null);

            assertNull(tx.read(tree).data(Id.ZERO).get().value());
        });
    }

    @Test
    void commit_twoDirectTreesWithGoodCommands_changesApplied()
            throws InterruptedException, ExecutionException {
        DirectSignalTree t1 = new DirectSignalTree(false);
        DirectSignalTree t2 = new DirectSignalTree(false);

        ResultHandler h1 = new ResultHandler();
        ResultHandler h2 = new ResultHandler();

        var operation = Transaction.runInTransaction(() -> {
            Transaction tx = Transaction.getCurrentTransaction();

            tx.apply(t1, TestUtil.rootValueCommand(), h1);
            tx.apply(t2, TestUtil.rootValueCommand(), h2);

            assertNull(h1.result);
            assertNull(h2.result);
        });

        assertTrue(h1.result.accepted());
        assertTrue(h2.result.accepted());

        assertTrue(operation.result().isDone());
        assertTrue(operation.result().get().successful());

        assertNotNull(TestUtil.confirmedRootValue(t1));
        assertNotNull(TestUtil.confirmedRootValue(t2));
    }

    @Test
    void commit_twoDirectTreesWithOneBadCommand_noChangeApplied()
            throws InterruptedException, ExecutionException {
        DirectSignalTree t1 = new DirectSignalTree(false);
        DirectSignalTree t2 = new DirectSignalTree(false);

        ResultHandler h1 = new ResultHandler();
        ResultHandler h2 = new ResultHandler();

        var operation = Transaction.runInTransaction(() -> {
            Transaction tx = Transaction.getCurrentTransaction();

            tx.apply(t1, TestUtil.rootValueCommand(), h1);
            tx.apply(t2, TestUtil.failingCommand(), h2);

            assertNull(TestUtil.transactionRootValue(t1),
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
            Transaction.getCurrentTransaction().apply(tree,
                    TestUtil.rootValueCommand(), handler);

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
            Transaction tx = Transaction.getCurrentTransaction();

            tx.apply(tree, TestUtil.rootValueCommand(), h1);
            tx.apply(tree, TestUtil.failingCommand(), h2);

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
        SignalCommand command = TestUtil.rootValueCommand();

        Transaction.runInTransaction(() -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrentTransaction().apply(tree, command, null);
            });

            assertTrue(tree.submitted.isEmpty());

            assertNotNull(TestUtil.transactionRootValue(tree));
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
        SignalCommand command = TestUtil.rootValueCommand();

        Transaction.runInTransaction(() -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrentTransaction().apply(tree, command, null);
            });

            assertEquals(1, tree.submitted.size());

            assertNotNull(TestUtil.transactionRootValue(tree));
        }, Type.WRITE_THROUGH);
    }

    @Test
    void commit_writeThroughInsideStaged_immediatelyAppliedAndVisible() {
        AsyncTestTree tree = new AsyncTestTree();
        SignalCommand command = TestUtil.rootValueCommand();

        Transaction.runInTransaction(() -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrentTransaction().apply(tree, command, null);
            }, Type.WRITE_THROUGH);

            assertEquals(1, tree.submitted.size());

            assertNotNull(TestUtil.transactionRootValue(tree));
        });
    }

    @Test
    void commit_failingTransactionSavedByDirectWrite_seemsFailingButIsAccepted()
            throws InterruptedException, ExecutionException {
        DirectSignalTree tree = new DirectSignalTree(false);

        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrentTransaction().apply(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            new TextNode("expected")),
                    null);
            Transaction.getCurrentTransaction().apply(tree,
                    TestUtil.rootValueCommand("update"), null);

            // This change is applied outside the transaction and will make it
            // pass
            tree.applyChange(TestUtil.rootValueCommand("expected"));

            // Transaction doesn't "know" about the previous change and "thinks"
            // it will not applied
            assertNull(TestUtil.transactionRootValue(tree));
        });

        assertTrue(operation.result().get().successful());
        assertEquals("update", TestUtil.confirmedRootValue(tree).textValue());
    }

    @Test
    void commit_failingTransactionSavedByNestedWriteThroughWrite_seemsFineAndIsAccepted()
            throws InterruptedException, ExecutionException {
        DirectSignalTree tree = new DirectSignalTree(false);

        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrentTransaction().apply(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            new TextNode("expected")),
                    null);
            Transaction.getCurrentTransaction().apply(tree,
                    TestUtil.rootValueCommand("update"), null);

            assertNull(TestUtil.confirmedRootValue(tree));

            Transaction.runInTransaction(() -> {
                Transaction.getCurrentTransaction().apply(tree,
                        TestUtil.rootValueCommand("expected"), null);
            }, Type.WRITE_THROUGH);

            assertEquals("update",
                    TestUtil.transactionRootValue(tree).textValue(),
                    "Should take inner transaction changes into account");
        });

        assertTrue(operation.result().get().successful());
        assertEquals("update", TestUtil.confirmedRootValue(tree).textValue());
    }

    @Test
    void commit_goodTransactionRuinedByDirectWrite_seemsFineButFails()
            throws InterruptedException, ExecutionException {
        DirectSignalTree tree = new DirectSignalTree(false);
        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrentTransaction().apply(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            null),
                    null);
            Transaction.getCurrentTransaction().apply(tree,
                    TestUtil.rootValueCommand("update"), null);

            tree.applyChange(TestUtil.rootValueCommand("unexpected"));

            assertEquals("update",
                    TestUtil.transactionRootValue(tree).textValue());
        });

        assertFalse(operation.result().get().successful());
        assertEquals("unexpected",
                TestUtil.confirmedRootValue(tree).textValue());
    }

    @Test
    void commit_goodTransactionRuinedByNestedWriteThroughWrite_seemsFailingAndFails()
            throws InterruptedException, ExecutionException {
        DirectSignalTree tree = new DirectSignalTree(false);
        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrentTransaction().apply(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            null),
                    null);
            Transaction.getCurrentTransaction().apply(tree,
                    TestUtil.rootValueCommand("update"), null);

            Transaction.runInTransaction(() -> {
                Transaction.getCurrentTransaction().apply(tree,
                        TestUtil.rootValueCommand("unexpected"), null);
            }, Type.WRITE_THROUGH);

            assertEquals("unexpected",
                    TestUtil.transactionRootValue(tree).textValue());
        });

        assertFalse(operation.result().get().successful());
        assertEquals("unexpected",
                TestUtil.confirmedRootValue(tree).textValue());
    }

    @Test
    void commit_failingTransactionSavedByAsyncConfirm_seemsFailingButIsAccepted()
            throws InterruptedException, ExecutionException {
        AsyncTestTree tree = new AsyncTestTree();

        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrentTransaction().apply(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            new TextNode("expected")),
                    null);
            Transaction.getCurrentTransaction().apply(tree,
                    TestUtil.rootValueCommand("update"), null);
        });

        assertNull(TestUtil.submittedRootValue(tree));

        tree.confirm(List.of(TestUtil.rootValueCommand("expected")));

        assertEquals("update", TestUtil.submittedRootValue(tree).textValue());

        tree.confirmSubmitted();

        assertTrue(operation.result().get().successful());
    }

    @Test
    void commit_goodTransactionRuinedByAsyncConfirm_seemsFineButfails()
            throws InterruptedException, ExecutionException {
        AsyncTestTree tree = new AsyncTestTree();
        var operation = Transaction.runInTransaction(() -> {
            Transaction.getCurrentTransaction().apply(tree,
                    new SignalCommand.ValueCondition(Id.random(), Id.ZERO,
                            null),
                    null);
            Transaction.getCurrentTransaction().apply(tree,
                    TestUtil.rootValueCommand("update"), null);
        });

        assertEquals("update", TestUtil.submittedRootValue(tree).textValue());

        tree.confirm(List.of(TestUtil.rootValueCommand("unexpected")));

        assertEquals("unexpected",
                TestUtil.submittedRootValue(tree).textValue());

        tree.confirmSubmitted();

        assertFalse(operation.result().get().successful());
    }

    @Test
    void treeMixing_multipleDirectAndComputed_allIsFine() {
        SignalTree d1 = new DirectSignalTree(false);
        SignalTree d2 = new DirectSignalTree(false);
        SignalTree c1 = new DirectSignalTree(true);
        SignalTree c2 = new DirectSignalTree(true);

        Transaction.runInTransaction(() -> {
            TestUtil.transactionRootValue(d1);
            TestUtil.transactionRootValue(d2);
            TestUtil.transactionRootValue(c1);
            TestUtil.transactionRootValue(c2);
        });
    }

    @Test
    void treeMixing_singleAsyncAndMultipleComputed_allIsFine() {
        SignalTree a1 = new AsyncTestTree();
        SignalTree c1 = new DirectSignalTree(true);
        SignalTree c2 = new DirectSignalTree(true);

        Transaction.runInTransaction(() -> {
            TestUtil.transactionRootValue(a1);
            TestUtil.transactionRootValue(c1);
            TestUtil.transactionRootValue(c2);
        });
    }

    @Test
    void treeMixing_multipleComputedAndSingleAsync_allIsFine() {
        SignalTree a1 = new AsyncTestTree();
        SignalTree c1 = new DirectSignalTree(true);
        SignalTree c2 = new DirectSignalTree(true);

        Transaction.runInTransaction(() -> {
            TestUtil.transactionRootValue(c1);
            TestUtil.transactionRootValue(c2);
            TestUtil.transactionRootValue(a1);
        });
    }

    @Test
    void treeMixing_multipleAsync_throws() {
        SignalTree a1 = new AsyncTestTree();
        SignalTree a2 = new AsyncTestTree();

        Transaction.runInTransaction(() -> {
            TestUtil.transactionRootValue(a1);

            assertThrows(IllegalStateException.class, () -> {
                TestUtil.transactionRootValue(a2);
            });
        });
    }

    @Test
    void treeMixing_asyncAndDirect_throws() {
        SignalTree a1 = new AsyncTestTree();
        SignalTree d1 = new DirectSignalTree(false);

        Transaction.runInTransaction(() -> {
            TestUtil.transactionRootValue(a1);

            assertThrows(IllegalStateException.class, () -> {
                TestUtil.transactionRootValue(d1);
            });
        });
    }

    @Test
    void treeMixing_directAndAsync_throws() {
        SignalTree a1 = new AsyncTestTree();
        SignalTree d1 = new DirectSignalTree(false);

        Transaction.runInTransaction(() -> {
            TestUtil.transactionRootValue(d1);

            assertThrows(IllegalStateException.class, () -> {
                TestUtil.transactionRootValue(a1);
            });
        });
    }

    @Test
    void commitLocking_multipleTrees_lockOrderConsistent() {
        class LockOrderTree extends DirectSignalTree {
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
                    Transaction.getCurrentTransaction().apply(tree,
                            TestUtil.rootValueCommand(), null);
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
