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
import com.vaadin.signals.impl.AsyncSignalTreeTest.AsyncTestTree;
import com.vaadin.signals.impl.CommandResult.Accept;
import com.vaadin.signals.impl.CommandResult.Reject;
import com.vaadin.signals.impl.CommandsAndHandlersTest.ResultHandler;
import com.vaadin.signals.impl.Transaction.Type;
import com.vaadin.signals.operations.TransactionOperation;

public class TransactionTest {
    @Test
    void getCurrentTransaction_noTransaction_rootTransaction() {
        DirectSignalTree tree = new DirectSignalTree(false);

        Transaction transaction = Transaction.getCurrentTransaction();
        assertFalse(Transaction.inTransaction());

        TreeRevision revision = transaction.read(tree);
        assertSame(tree.submitted(), revision);

        transaction.apply(tree, TestUtil.rootValueCommand(), null);
        assertNotNull(TestUtil.submittedRootValue(tree));
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
    void runInTransaction_runnable_nullReturned() {
        AtomicBoolean invoked = new AtomicBoolean();
        TransactionOperation<Void> operation = Transaction
                .runInTransaction(() -> {
                    assertTrue(Transaction.inTransaction());
                    invoked.set(true);
                });

        assertTrue(invoked.get());
        assertNull(operation.returnValue());
    }

    @Test
    void runInTransaction_defaultTransactionType_isFull() {
        Transaction transaction = Transaction.runInTransaction(() -> {
            return Transaction.getCurrentTransaction();
        }).returnValue();

        assertInstanceOf(StagedTransaction.class, transaction);
    }

    @Test
    void runInTransaction_successfulFull_committed()
            throws InterruptedException, ExecutionException {
        AsyncTestTree tree = new AsyncTestTree();
        SignalCommand command = TestUtil.rootValueCommand();

        TransactionOperation<Void> operation = Transaction
                .runInTransaction(() -> {
                    Transaction transaction = Transaction
                            .getCurrentTransaction();
                    assertInstanceOf(StagedTransaction.class, transaction);

                    transaction.apply(tree, command, null);
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
                Transaction.getCurrentTransaction().apply(tree,
                        TestUtil.rootValueCommand(), handler);
                throw new RuntimeException();
            });
        });

        assertEquals(List.of(), tree.submitted);
        assertInstanceOf(Reject.class, handler.result);
    }

    @Test
    void runInTransaction_throwingWriteThrough_appliedAsExecuted() {
        AsyncTestTree tree = new AsyncTestTree();
        ResultHandler handler = new ResultHandler();

        assertThrows(RuntimeException.class, () -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrentTransaction().apply(tree,
                        TestUtil.rootValueCommand(), handler);
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
        DirectSignalTree tree = new DirectSignalTree(false);
        ResultHandler handler = new ResultHandler();

        TransactionOperation<Void> operation = Transaction
                .runInTransaction(() -> {
                    Transaction.getCurrentTransaction().apply(tree,
                            TestUtil.rootValueCommand(), handler);
                }, Type.WRITE_THROUGH);

        assertTrue(operation.result().isDone());
        assertTrue(operation.result().get().successful());

        assertTrue(handler.result.accepted());
    }

    @Test
    void writeThrough_rejectedChange_operationResultSuccessful()
            throws InterruptedException, ExecutionException {
        DirectSignalTree tree = new DirectSignalTree(false);
        ResultHandler handler = new ResultHandler();

        TransactionOperation<Void> operation = Transaction
                .runInTransaction(() -> {
                    Transaction.getCurrentTransaction().apply(tree,
                            TestUtil.failingCommand(), handler);
                }, Type.WRITE_THROUGH);

        assertTrue(operation.result().isDone());
        assertTrue(operation.result().get().successful());

        assertFalse(handler.result.accepted());
    }

    @Test
    void writeThrough_externalChange_repeatableRead() {
        DirectSignalTree tree = new DirectSignalTree(false);

        Transaction.runInTransaction(() -> {
            JsonNode beforeUpdate = TestUtil.transactionRootValue(tree);

            // Writes directly to the tree, skipping the transaction
            tree.applyChange(TestUtil.rootValueCommand());

            JsonNode afterUpdate = TestUtil.transactionRootValue(tree);
            assertNull(afterUpdate);
            assertSame(beforeUpdate, afterUpdate);
        }, Type.WRITE_THROUGH);

        JsonNode outsideTransaction = TestUtil.transactionRootValue(tree);
        assertNotNull(outsideTransaction);
    }

    @Test
    void writeThrough_changeThroughTransaction_visibleAndWrittenImmediately() {
        DirectSignalTree tree = new DirectSignalTree(false);

        Transaction.runInTransaction(() -> {
            JsonNode beforeUpdate = TestUtil.transactionRootValue(tree);
            assertNull(beforeUpdate);

            Transaction.getCurrentTransaction().apply(tree,
                    TestUtil.rootValueCommand(), null);

            assertNotNull(TestUtil.submittedRootValue(tree));

            JsonNode afterUpdate = TestUtil.transactionRootValue(tree);
            assertNotNull(afterUpdate);
        }, Type.WRITE_THROUGH);
    }

    @Test
    void writeThrough_multipleTrees_readValuesLockedAfterFirstTxUse() {
        DirectSignalTree tree1 = new DirectSignalTree(false);
        DirectSignalTree tree2 = new DirectSignalTree(false);

        Transaction.runInTransaction(() -> {
            // Just read to participate, don't care about the value
            TestUtil.transactionRootValue(tree1);

            tree2.applyChange(TestUtil.rootValueCommand("value"));

            String value = TestUtil.transactionRootValue(tree2).textValue();
            assertEquals("value", value);

            tree2.applyChange(TestUtil.rootValueCommand("value2"));

            String value2 = TestUtil.transactionRootValue(tree2).textValue();
            assertEquals("value", value2);
        }, Type.WRITE_THROUGH);
    }

    @Test
    void writeThrough_readInNestedTx_readsFromOuterTx() {
        DirectSignalTree tree = new DirectSignalTree(false);

        Transaction.runInTransaction(() -> {
            // Read just to participate
            TestUtil.transactionRootValue(tree);

            // Make an external change that isn't visible in the transaction
            tree.applyChange(TestUtil.rootValueCommand());

            Transaction.runInTransaction(() -> {
                assertNull(TestUtil.transactionRootValue(tree),
                        "Inner transaction should read values from the outer transaction");
            });
        }, Type.WRITE_THROUGH);
    }

    @Test
    void writeThrough_writeInNestedTx_visibleInOuterTx() {
        DirectSignalTree tree = new DirectSignalTree(false);

        Transaction.runInTransaction(() -> {
            // Read just to participate
            TestUtil.transactionRootValue(tree);

            Transaction.runInTransaction(() -> {
                Transaction.getCurrentTransaction().apply(tree,
                        TestUtil.rootValueCommand(), null);
            }, Type.WRITE_THROUGH);

            assertNotNull(TestUtil.transactionRootValue(tree));
        }, Type.WRITE_THROUGH);
    }

    @Test
    void readonly_externalChange_repeatableRead() {
        DirectSignalTree tree = new DirectSignalTree(false);

        Transaction.runInTransaction(() -> {
            JsonNode beforeUpdate = TestUtil.transactionRootValue(tree);

            // Writes directly to the tree, skipping the transaction
            tree.applyChange(TestUtil.rootValueCommand());

            JsonNode afterUpdate = TestUtil.transactionRootValue(tree);
            assertNull(afterUpdate);
            assertSame(beforeUpdate, afterUpdate);
        }, Type.READ_ONLY);

        JsonNode outsideTransaction = TestUtil.transactionRootValue(tree);
        assertNotNull(outsideTransaction);
    }

    @Test
    void readonly_writeDirectTree_throws() {
        DirectSignalTree tree = new DirectSignalTree(false);

        assertThrows(IllegalStateException.class, () -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrentTransaction().apply(tree,
                        TestUtil.rootValueCommand(), null);
            }, Type.READ_ONLY);
        });
    }

    @Test
    void readonly_writeAsyncTree_throws() {
        AsyncTestTree tree = new AsyncTestTree();

        assertThrows(IllegalStateException.class, () -> {
            Transaction.runInTransaction(() -> {
                Transaction.getCurrentTransaction().apply(tree,
                        TestUtil.rootValueCommand(), null);
            }, Type.READ_ONLY);
        });
    }

    @Test
    void readonly_writeComputedTree_accepted() {
        DirectSignalTree tree = new DirectSignalTree(true);

        Transaction.runInTransaction(() -> {
            Transaction.getCurrentTransaction().apply(tree,
                    TestUtil.rootValueCommand(), null);
        }, Type.READ_ONLY);

        assertNotNull(TestUtil.submittedRootValue(tree));
    }

    @Test
    void readonly_writeInNoTransaction_acceptedButIgnoredInTransaction() {
        DirectSignalTree tree = new DirectSignalTree(false);

        Transaction.runInTransaction(() -> {
            Transaction.runWithoutTransaction(() -> {
                Transaction.getCurrentTransaction().apply(tree,
                        TestUtil.rootValueCommand(), null);
            });
        }, Type.READ_ONLY);

        assertNotNull(TestUtil.submittedRootValue(tree));
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
