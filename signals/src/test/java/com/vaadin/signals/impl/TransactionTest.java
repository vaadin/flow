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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.SignalCommand.TransactionCommand;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.impl.AsynchronousSignalTreeTest.AsyncTestTree;
import com.vaadin.signals.impl.CommandResult.Accept;
import com.vaadin.signals.impl.CommandResult.Reject;
import com.vaadin.signals.impl.CommandsAndHandlersTest.ResultHandler;
import com.vaadin.signals.impl.Transaction.Type;
import com.vaadin.signals.operations.SignalOperation;
import com.vaadin.signals.operations.TransactionOperation;

public class TransactionTest {
    @Test
    void getCurrentTransaction_noTransaction_rootTransaction() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Transaction transaction = Transaction.getCurrent();
        assertFalse(Transaction.inTransaction());

        TreeRevision revision = transaction.read(tree);
        assertSame(tree.submitted(), revision);

        transaction.include(tree, TestUtil.writeRootValueCommand(), null);
        assertNotNull(TestUtil.readSubmittedRootValue(tree));
    }

    @Test
    void runInTransaction_supplier_valueReturned() {
        String value = "the value";

        TransactionOperation<String> operation = Transaction
                .runInTransaction(() -> {
                    assertTrue(Transaction.inTransaction());
                    return value;
                });

        assertSame(value, operation.returnValue());
    }

    @Test
    void runInTransaction_defaultTransactionType_isFull() {
        Transaction transaction = Transaction.runInTransaction(() -> {
            return Transaction.getCurrent();
        }).returnValue();

        assertInstanceOf(StagedTransaction.class, transaction);
    }

    @Test
    void runInTransaction_successfulFull_committed()
            throws InterruptedException, ExecutionException {
        AsyncTestTree tree = new AsyncTestTree();
        SignalCommand command = TestUtil.writeRootValueCommand();

        SignalOperation<Void> operation = Transaction.runInTransaction(() -> {
            Transaction transaction = Transaction.getCurrent();
            assertInstanceOf(StagedTransaction.class, transaction);

            transaction.include(tree, command, null);
            assertEquals(List.of(), tree.submitted,
                    "Nothing should be submitted before the transaction ends");
        });

        List<SignalCommand> commands = tree.submitted.get(0);
        assertEquals(1, commands.size());
        assertInstanceOf(SignalCommand.TransactionCommand.class,
                commands.get(0));

        SignalCommand.TransactionCommand tx = (TransactionCommand) commands
                .get(0);
        assertEquals(List.of(command), tx.commands());

        assertFalse(operation.result().isDone());

        tree.confirmSubmitted();

        assertTrue(operation.result().isDone());
        assertTrue(operation.result().get().successful());
    }

    @Test
    void runInTransaction_throwingFull_rolledBack() {
        AsyncTestTree tree = new AsyncTestTree();
        ResultHandler handler = new ResultHandler();

        assertThrows(RuntimeException.class, () -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrent().include(tree,
                        TestUtil.writeRootValueCommand(), handler);
                throw new RuntimeException();
            });
        });

        assertFalse(Transaction.inTransaction());
        assertEquals(List.of(), tree.submitted);
        assertInstanceOf(Reject.class, handler.result);
    }

    @Test
    void runInTransaction_throwingWriteThrough_appliedAsExecuted() {
        AsyncTestTree tree = new AsyncTestTree();
        ResultHandler handler = new ResultHandler();

        assertThrows(RuntimeException.class, () -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrent().include(tree,
                        TestUtil.writeRootValueCommand(), handler);
                throw new RuntimeException();
            }, Type.WRITE_THROUGH);
        });

        assertEquals(1, tree.submitted.size());

        tree.confirmSubmitted();
        assertInstanceOf(Accept.class, handler.result);
    }

    @Test
    void runWithoutTransaction_runnable_isRun() {
        AtomicBoolean invoked = new AtomicBoolean();

        Transaction.runWithoutTransaction(() -> {
            assertFalse(Transaction.inTransaction());

            invoked.set(true);
        });

        assertTrue(invoked.get());
    }

    @Test
    void runWithoutTransaction_supplier_valueIsReturned() {
        String value = "value";

        String result = Transaction.runWithoutTransaction(() -> {
            return value;
        });

        assertSame(value, result);
    }

    @Test
    void writeThrough_acceptedChange_operationResultSuccessful()
            throws InterruptedException, ExecutionException {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        ResultHandler handler = new ResultHandler();

        TransactionOperation<Void> operation = Transaction
                .runInTransaction(() -> {
                    Transaction.getCurrent().include(tree,
                            TestUtil.writeRootValueCommand(), handler);
                }, Type.WRITE_THROUGH);

        assertTrue(operation.result().isDone());
        assertTrue(operation.result().get().successful());

        assertTrue(handler.result.accepted());
    }

    @Test
    void writeThrough_rejectedChange_operationResultSuccessful()
            throws InterruptedException, ExecutionException {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);
        ResultHandler handler = new ResultHandler();

        TransactionOperation<Void> operation = Transaction
                .runInTransaction(() -> {
                    Transaction.getCurrent().include(tree,
                            TestUtil.failingCommand(), handler);
                }, Type.WRITE_THROUGH);

        assertTrue(operation.result().isDone());
        assertTrue(operation.result().get().successful());

        assertFalse(handler.result.accepted());
    }

    @Test
    void writeThrough_externalChange_repeatableRead() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Transaction.runInTransaction(() -> {
            JsonNode beforeUpdate = TestUtil.readTransactionRootValue(tree);

            // Writes directly to the tree, skipping the transaction
            tree.commitSingleCommand(TestUtil.writeRootValueCommand());

            JsonNode afterUpdate = TestUtil.readTransactionRootValue(tree);
            assertNull(afterUpdate);
            assertSame(beforeUpdate, afterUpdate);
        }, Type.WRITE_THROUGH);

        JsonNode outsideTransaction = TestUtil.readTransactionRootValue(tree);
        assertNotNull(outsideTransaction);
    }

    @Test
    void writeThrough_changeThroughTransaction_visibleAndWrittenImmediately() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Transaction.runInTransaction(() -> {
            JsonNode beforeUpdate = TestUtil.readTransactionRootValue(tree);
            assertNull(beforeUpdate);

            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand(), null);

            assertNotNull(TestUtil.readSubmittedRootValue(tree));

            JsonNode afterUpdate = TestUtil.readTransactionRootValue(tree);
            assertNotNull(afterUpdate);
        }, Type.WRITE_THROUGH);
    }

    @Test
    void writeThrough_writesBypassingTransaction_readValuesLockedAfterFirstTxUse() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Transaction.runInTransaction(() -> {
            tree.commitSingleCommand(TestUtil.writeRootValueCommand("value"));

            String value = TestUtil.readTransactionRootValue(tree).textValue();
            assertEquals("value", value);

            tree.commitSingleCommand(TestUtil.writeRootValueCommand("value2"));

            String value2 = TestUtil.readTransactionRootValue(tree).textValue();
            assertEquals("value", value2);
        }, Type.WRITE_THROUGH);
    }

    @Test
    void writeThrough_readInNestedTx_readsFromOuterTx() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Transaction.runInTransaction(() -> {
            // Read just to participate
            TestUtil.readTransactionRootValue(tree);

            // Make an external change that isn't visible in the transaction
            tree.commitSingleCommand(TestUtil.writeRootValueCommand());

            Transaction.runInTransaction(() -> {
                assertNull(TestUtil.readTransactionRootValue(tree),
                        "Inner transaction should read values from the outer transaction");
            });
        }, Type.WRITE_THROUGH);
    }

    @Test
    void writeThrough_writeInNestedTx_visibleInOuterTx() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Transaction.runInTransaction(() -> {
            // Read just to participate
            TestUtil.readTransactionRootValue(tree);

            Transaction.runInTransaction(() -> {
                Transaction.getCurrent().include(tree,
                        TestUtil.writeRootValueCommand(), null);
            }, Type.WRITE_THROUGH);

            assertNotNull(TestUtil.readTransactionRootValue(tree));
        }, Type.WRITE_THROUGH);
    }

    @Test
    void readonly_externalChange_repeatableRead() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Transaction.runInTransaction(() -> {
            JsonNode beforeUpdate = TestUtil.readTransactionRootValue(tree);

            // Writes directly to the tree, skipping the transaction
            tree.commitSingleCommand(TestUtil.writeRootValueCommand());

            JsonNode afterUpdate = TestUtil.readTransactionRootValue(tree);
            assertNull(afterUpdate);
            assertSame(beforeUpdate, afterUpdate);
        }, Type.READ_ONLY);

        JsonNode outsideTransaction = TestUtil.readTransactionRootValue(tree);
        assertNotNull(outsideTransaction);
    }

    @Test
    void readonly_writeSyncTree_throws() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        assertThrows(IllegalStateException.class, () -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrent().include(tree,
                        TestUtil.writeRootValueCommand(), null);
            }, Type.READ_ONLY);
        });
    }

    @Test
    void readonly_writeAsyncTree_throws() {
        AsyncTestTree tree = new AsyncTestTree();

        assertThrows(IllegalStateException.class, () -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrent().include(tree,
                        TestUtil.writeRootValueCommand(), null);
            }, Type.READ_ONLY);
        });
    }

    @Test
    void readonly_writeComputedTree_accepted() {
        SynchronousSignalTree tree = new SynchronousSignalTree(true);

        Transaction.runInTransaction(() -> {
            Transaction.getCurrent().include(tree,
                    TestUtil.writeRootValueCommand(), null);
        }, Type.READ_ONLY);

        assertNotNull(TestUtil.readSubmittedRootValue(tree));
    }

    @Test
    void readonly_writeInNoTransaction_acceptedButIgnoredInTransaction() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        Transaction.runInTransaction(() -> {
            Transaction.runWithoutTransaction(() -> {
                Transaction.getCurrent().include(tree,
                        TestUtil.writeRootValueCommand(), null);
            });
        }, Type.READ_ONLY);

        assertNotNull(TestUtil.readSubmittedRootValue(tree));
    }

    @Test
    void transactionWrapping_inFull_acceptAll() {
        Transaction.runInTransaction(() -> {
            Transaction.runInTransaction(dummyTask(), Type.READ_ONLY);
            Transaction.runInTransaction(dummyTask(), Type.STAGED);
            Transaction.runInTransaction(dummyTask(), Type.WRITE_THROUGH);
            Transaction.runWithoutTransaction(dummyTask());
        }, Type.STAGED);
    }

    @Test
    void transactionWrapping_inWriteThrough_acceptAll() {
        Transaction.runInTransaction(() -> {
            Transaction.runInTransaction(dummyTask(), Type.READ_ONLY);
            Transaction.runInTransaction(dummyTask(), Type.STAGED);
            Transaction.runInTransaction(dummyTask(), Type.WRITE_THROUGH);
            Transaction.runWithoutTransaction(dummyTask());
        }, Type.WRITE_THROUGH);
    }

    @Test
    void transactionWrapping_readOnly_acceptOnlyReadOnlyAndNoTransaction() {
        Transaction.runInTransaction(() -> {
            Transaction.runInTransaction(dummyTask(), Type.READ_ONLY);

            assertThrows(IllegalStateException.class, () -> {
                Transaction.runInTransaction(dummyTask(), Type.STAGED);
            });

            assertThrows(IllegalStateException.class, () -> {
                Transaction.runInTransaction(dummyTask(), Type.WRITE_THROUGH);
            });

            Transaction.runWithoutTransaction(dummyTask());
        }, Type.READ_ONLY);
    }

    private static Runnable dummyTask() {
        return () -> {
        };
    }
}
