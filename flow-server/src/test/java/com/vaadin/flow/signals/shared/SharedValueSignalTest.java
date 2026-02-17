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
package com.vaadin.flow.signals.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.SignalCommand.SetCommand;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.TestUtil;
import com.vaadin.flow.signals.impl.Transaction;
import com.vaadin.flow.signals.impl.UsageTracker;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;
import com.vaadin.flow.signals.operations.CancelableOperation;
import com.vaadin.flow.signals.operations.SignalOperation;
import com.vaadin.flow.signals.operations.TransactionOperation;
import com.vaadin.flow.signals.shared.impl.AsynchronousSignalTreeTest.AsyncTestTree;

import static com.vaadin.flow.signals.TestUtil.assertFailure;
import static com.vaadin.flow.signals.TestUtil.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SharedValueSignalTest extends SignalTestBase {
    /*
     * Note that there's no test specific to the generic Signal class but most
     * of that functionality is instead tested through its simplest subclass,
     * i.e. SharedValueSignal
     */

    @Test
    void constructor_type_noValueAndTypeIsUsed() {
        SharedValueSignal<String> signal = new SharedValueSignal<>(
                String.class);
        assertNull(signal.get());

        signal.set("a string");
        assertEquals("a string", signal.get());

        assertThrows(AssertionError.class, () -> {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            SharedValueSignal<Object> raw = ((SharedValueSignal) signal);

            raw.set(new Object());
        });
        assertEquals("a string", signal.get());
    }

    @Test
    void constructor_initialValue_valueUsedAndTypeIsInferred() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        assertEquals("initial", signal.get());

        signal.set("a string");
        assertEquals("a string", signal.get());

        assertThrows(AssertionError.class, () -> {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            SharedValueSignal<Object> raw = ((SharedValueSignal) signal);

            raw.set(new Object());
        });
        assertEquals("a string", signal.get());
    }

    @Test
    void constructor_nullType_throws() {
        assertThrows(NullPointerException.class, () -> {
            Class<String> type = null;
            new SharedValueSignal<>(type);
        });
    }

    @Test
    void constructor_nullInitialValue_throws() {
        assertThrows(NullPointerException.class, () -> {
            String initial = null;
            new SharedValueSignal<>(initial);
        });
    }

    @Test
    void value_mutateValueInstance_signalValueUnaffected() {
        String[] array = { "initial" };
        SharedValueSignal<String[]> signal = new SharedValueSignal<>(array);

        array[0] = "modified";
        assertEquals("initial", signal.get()[0]);

        signal.get()[0] = "modified";
        assertEquals("initial", signal.get()[0]);
    }

    @Test
    void value_hasPrevious_previousInResult() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        SignalOperation<String> operation = signal.set("update");
        assertEquals("update", signal.get());

        String resultValue = assertSuccess(operation);
        assertEquals("initial", resultValue);
    }

    @Test
    void peekConfirmed_hasUnconfirmedChange_changeIngored() {
        AsyncSharedValueSignal signal = new AsyncSharedValueSignal();
        signal.set("update");

        assertEquals("update", signal.get());
        assertNull(signal.peekConfirmed());

        signal.tree().confirmSubmitted();

        assertEquals("update", signal.peekConfirmed());
    }

    @Test
    void replace_expectedValue_successfulResult() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("expected");

        SignalOperation<Void> operation = signal.replace("expected", "update");
        assertEquals("update", signal.get());

        assertSuccess(operation);
    }

    @Test
    void replace_unexpectedValue_failedlResult() {
        SharedValueSignal<String> signal = new SharedValueSignal<>(
                "unexpected");

        SignalOperation<Void> operation = signal.replace("expected", "update");
        assertEquals("unexpected", signal.get());

        assertFailure(operation);
    }

    @Test
    void update_noConflict_updatedWithPreviousValueInResult() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        CancelableOperation<String> operation = signal.update(previous -> {
            assertEquals("initial", previous);
            return "update";
        });

        assertEquals("initial", assertSuccess(operation));
    }

    @Test
    void update_cancelWithConflict_noFurtherInvocationsAndCancelledResult() {
        AsyncSharedValueSignal signal = new AsyncSharedValueSignal();

        CancelableOperation<String> operation = signal.update(previous -> {
            assertNull(previous);

            return "update";
        });

        operation.cancel();

        signal.tree()
                .confirm(List.of(TestUtil.writeRootValueCommand("unexpected")));
        assertFalse(operation.result().isDone());

        signal.tree().confirmSubmitted();
        assertTrue(operation.result().isCancelled());
    }

    @Test
    void update_cancelWithoutConflict_succeeds() {
        AsyncSharedValueSignal signal = new AsyncSharedValueSignal();

        CancelableOperation<String> operation = signal.update(previous -> {
            assertNull(previous);

            return "update";
        });

        operation.cancel();

        signal.tree().confirmSubmitted();
        assertNull(assertSuccess(operation));
        assertEquals("update", signal.get());
    }

    @Test
    void update_conflictsNoCancel_eventuallySucceeds() {
        SharedValueSignal<Integer> signal = new SharedValueSignal<>(
                Integer.valueOf(0));

        CancelableOperation<Integer> operation = signal.update(previous -> {
            if (previous < 5) {
                // Provoke a conflict while still making progress
                Signal.runWithoutTransaction(() -> {
                    signal.set(previous + 1);
                });
            }

            return previous + 1;
        });

        assertEquals(5, assertSuccess(operation));
        assertEquals(6, signal.get());
    }

    @Test
    void verifyValue_expectedValue_operationSuccessful() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("expected");

        SignalOperation<Void> operation = signal.verifyValue("expected");

        assertSuccess(operation);
    }

    @Test
    void verifyValue_unexpectedValue_operationSuccessful() {
        SharedValueSignal<String> signal = new SharedValueSignal<>(
                "unexpected");

        SignalOperation<Void> operation = signal.verifyValue("expected");

        assertFailure(operation);
    }

    @Test
    void withValidator_acceptsOperation_operationAccepted() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        List<SignalCommand> validatedCommands = new ArrayList<>();

        SharedValueSignal<String> wrapper = signal.withValidator(command -> {
            validatedCommands.add(command);
            return true;
        });

        wrapper.set("update");

        assertEquals("update", signal.get());
        assertEquals(1, validatedCommands.size());
        assertInstanceOf(SignalCommand.SetCommand.class,
                validatedCommands.get(0));
    }

    @Test
    void withValidator_rejectsNullValues_nullRejectedAndOtherAccepted() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        SharedValueSignal<String> wrapper = signal.withValidator(command -> {
            if (command instanceof SetCommand set) {
                return !set.value().isNull();
            }
            return true;
        });

        SignalOperation<String> updateResult = wrapper.set("update");
        assertSuccess(updateResult);
        assertEquals("update", wrapper.get());

        assertThrows(UnsupportedOperationException.class, () -> {
            wrapper.set(null);
        });
        assertEquals("update", wrapper.get());
    }

    @Test
    void withValidator_changeThroughOriginal_validatorNotInvokedAndWrapperUpdated() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        SharedValueSignal<String> wrapper = signal.withValidator(command -> {
            throw new RuntimeException();
        });

        signal.set("update");

        assertEquals("update", wrapper.get());
    }

    @Test
    void withValidator_verifyCommand_validatorNotInvoked() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        SharedValueSignal<String> wrapper = signal.withValidator(command -> {
            throw new RuntimeException();
        });

        SignalOperation<Void> result = wrapper.verifyValue("initial");
        assertSuccess(result);
    }

    @Test
    void withValidator_inTransaction_validatorInvokedForChildren() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        List<SignalCommand> validatedCommands = new ArrayList<>();

        SharedValueSignal<String> wrapper = signal.withValidator(command -> {
            validatedCommands.add(command);
            return true;
        });

        Signal.runInTransaction(() -> {
            wrapper.set("update");
        });

        assertEquals(1, validatedCommands.size());
        assertInstanceOf(SetCommand.class, validatedCommands.get(0));
    }

    @Test
    void readonly_makeChanges_changesRejected() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        SharedValueSignal<String> readonly = signal.asReadonly();

        assertThrows(UnsupportedOperationException.class, () -> {
            readonly.set("Update");
        });

        assertEquals("initial", readonly.get());
    }

    @Test
    void usageTracking_changingSignalValue_usageDetectsValueChange() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        AtomicInteger count = new AtomicInteger();

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        assertFalse(usage.hasChanges());

        usage.onNextChange(immediate -> {
            count.incrementAndGet();
            return false;
        });

        signal.set("update");
        assertEquals(1, count.intValue());
        assertTrue(usage.hasChanges());

        signal.set("anohter");
        assertEquals(1, count.intValue());
    }

    @Test
    void usageTracking_repeatingChangeListener_usageDetectsFollowingValueChange() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        AtomicInteger count = new AtomicInteger();

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        usage.onNextChange(immediate -> {
            count.incrementAndGet();
            return true;
        });

        signal.set("update");
        assertEquals(1, count.intValue());

        signal.set("anohter");
        assertEquals(2, count.intValue());
    }

    @Test
    void usageTracking_noOpChange_listenerNotNotifiedButRemainsActive() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        AtomicInteger count = new AtomicInteger();

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        usage.onNextChange(immediate -> {
            count.incrementAndGet();
            return false;
        });

        signal.set("initial");
        assertEquals(0, count.intValue());
        assertFalse(usage.hasChanges());

        signal.set("update");
        assertEquals(1, count.intValue());
        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracking_unrelatedChange_listenerNotNotifiedButRemainsActive() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        AtomicInteger count = new AtomicInteger();

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        usage.onNextChange(immediate -> {
            count.incrementAndGet();
            return false;
        });

        signal.asNode().putChildWithValue("key", "value");
        assertEquals(0, count.intValue());
        assertFalse(usage.hasChanges());

        signal.set("update");
        assertEquals(1, count.intValue());
        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracking_registerAfterChange_listenerCalledImmediately() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        AtomicInteger falseCount = new AtomicInteger();
        AtomicInteger trueCount = new AtomicInteger();

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        signal.set("update");

        usage.onNextChange(immediate -> {
            falseCount.incrementAndGet();
            return false;
        });
        assertEquals(1, falseCount.intValue());

        usage.onNextChange(immediate -> {
            trueCount.incrementAndGet();
            return true;
        });
        assertEquals(1, trueCount.intValue());

        signal.set("again");
        assertEquals(1, falseCount.intValue());
        assertEquals(2, trueCount.intValue());
    }

    @Test
    void usageTracking_removeSignalAfterTracking_hasNoChanges() {
        SharedListSignal<String> list = new SharedListSignal<>(String.class);
        SharedValueSignal<String> signal = list.insertLast("value").signal();

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        list.remove(signal);

        assertFalse(usage.hasChanges());
    }

    @Test
    void usageTracking_removeSignalBeforeTracking_hasNoChanges() {
        SharedListSignal<String> list = new SharedListSignal<>(String.class);
        SharedValueSignal<String> signal = list.insertLast("value").signal();

        list.remove(signal);

        Usage usage = UsageTracker.track(() -> {
            signal.get();
        });

        assertFalse(usage.hasChanges());
    }

    @Test
    void result_successfulOperation_resolvedThroughResultNotifier()
            throws Exception {
        TestExecutor notifier = useTestResultNotifier();

        SharedValueSignal<String> signal = new SharedValueSignal<>(
                String.class);
        SignalOperation<String> operation = signal.set("update");

        assertFalse(operation.result().isDone());
        awaitPendingTasks(notifier, 1);

        notifier.runPendingTasks();
        assertTrue(operation.result().isDone());
        assertEquals(0, notifier.countPendingTasks());
    }

    @Test
    void result_failingOperation_resolvedThroughResultNotifier()
            throws Exception {
        TestExecutor notifier = useTestResultNotifier();

        SharedValueSignal<String> signal = new SharedValueSignal<>(
                String.class);
        SignalOperation<Void> operation = signal.replace("other", "update");

        assertFalse(operation.result().isDone());
        awaitPendingTasks(notifier, 1);

        notifier.runPendingTasks();
        assertTrue(operation.result().isDone());
        assertEquals(0, notifier.countPendingTasks());
    }

    private void awaitPendingTasks(TestExecutor executor, int expectedCount)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (executor.countPendingTasks() < expectedCount) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError(
                        "Timed out waiting for pending tasks to reach "
                                + expectedCount);
            }
            Thread.sleep(10);
        }
        assertEquals(expectedCount, executor.countPendingTasks());
    }

    @Test
    void transaction_withSupplier_supplierIsRunAndReturnValueAccessible() {
        AtomicInteger inCount = new AtomicInteger();
        AtomicInteger withoutCount = new AtomicInteger();

        TransactionOperation<String> operation = Signal.runInTransaction(() -> {
            assertTrue(Transaction.inTransaction());
            inCount.incrementAndGet();

            return Signal.runWithoutTransaction(() -> {
                assertFalse(Transaction.inTransaction());
                withoutCount.incrementAndGet();

                return "result";
            });
        });

        assertEquals(1, inCount.intValue());
        assertEquals(1, withoutCount.intValue());
        assertSuccess(operation);
        assertEquals("result", operation.returnValue());
    }

    @Test
    void transaction_withRunnable_runnableIsRun() {
        AtomicInteger inCount = new AtomicInteger();
        AtomicInteger withoutCount = new AtomicInteger();

        TransactionOperation<Void> operation = Signal.runInTransaction(() -> {
            assertTrue(Transaction.inTransaction());
            inCount.incrementAndGet();

            Signal.runWithoutTransaction(() -> {
                assertFalse(Transaction.inTransaction());
                withoutCount.incrementAndGet();
            });
        });

        assertEquals(1, inCount.intValue());
        assertEquals(1, withoutCount.intValue());
        assertSuccess(operation);
        assertNull(operation.returnValue());
    }

    @Test
    void transaction_withFailingSupplier_supplierIsRunAndReturnValueAccessible() {
        AtomicInteger inCount = new AtomicInteger();
        SharedValueSignal<String> signal = new SharedValueSignal<>("value");

        TransactionOperation<String> operation = Signal.runInTransaction(() -> {
            assertTrue(Transaction.inTransaction());
            inCount.incrementAndGet();

            signal.verifyValue("other");

            return "result";
        });

        assertEquals(1, inCount.intValue());
        assertFailure(operation);
        assertEquals("result", operation.returnValue());
    }

    @Test
    void transaction_readValue_readThroughTransaction() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("value");

        Signal.runInTransaction(() -> {
            // Read to make signal participate in transaction
            signal.get();

            Signal.runWithoutTransaction(() -> {
                signal.set("update");
            });

            assertEquals("value", signal.get());
        });
        assertEquals("update", signal.get());
    }

    @Test
    void transaction_writeValue_writtenThroughTransaction() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("value");

        Signal.runInTransaction(() -> {
            signal.set("update");

            Signal.runWithoutTransaction(() -> {
                assertEquals("value", signal.get());
            });
        });
        assertEquals("update", signal.get());
    }

    @Test
    void transaction_readInTransactionAndChangeOutside_transactionFails() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("value");

        TransactionOperation<Void> operation = Signal.runInTransaction(() -> {
            signal.set(signal.get() + " update");

            Signal.runWithoutTransaction(() -> {
                signal.set("update");
            });
        });

        assertFailure(operation);
        assertEquals("update", signal.get());
    }

    @Test
    void transaction_peekInTransactionAndChangeOutside_transactionSuccessful() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("value");

        TransactionOperation<Void> operation = Signal.runInTransaction(() -> {
            signal.set(signal.peek() + " update");

            Signal.runWithoutTransaction(() -> {
                signal.set("update");
            });
        });

        assertSuccess(operation);
        assertEquals("value update", signal.get());
    }

    @Test
    void transaction_peekConfirmedInTransactionAndChangeOutside_transactionSuccessful() {
        SharedValueSignal<String> signal = new SharedValueSignal<>(
                String.class);
        assertSuccess(signal.set("value"));

        TransactionOperation<Void> operation = Signal.runInTransaction(() -> {
            signal.set(signal.peekConfirmed() + " update");

            Signal.runWithoutTransaction(() -> {
                signal.set("update");
            });
        });

        assertSuccess(operation);
        assertEquals("value update", signal.get());
    }

    @Test
    void equalsHashCode() {
        SharedValueSignal<String> signal = new SharedValueSignal<>(
                String.class);
        assertEquals(signal, signal);

        SharedValueSignal<String> copy = new SharedValueSignal<>(signal.tree(),
                signal.id(), signal.validator(), String.class);
        assertEquals(signal, copy);
        assertEquals(signal.hashCode(), copy.hashCode());

        SharedValueSignal<String> asValue = signal.asNode()
                .asValue(String.class);
        assertEquals(signal, asValue);
        assertEquals(signal.hashCode(), asValue.hashCode());

        assertNotEquals(signal, new SharedValueSignal<>(String.class));
        assertNotEquals(signal, signal.asReadonly());
        assertNotEquals(signal, signal.asNode());
        assertNotEquals(signal, signal.asNode().asValue(Double.class));
    }

    @Test
    void toString_includesValue() {
        SharedValueSignal<String> signal = new SharedValueSignal<>(
                "signal value");

        assertEquals("SharedValueSignal[signal value]", signal.toString());
    }

    public static class AsyncSharedValueSignal
            extends SharedValueSignal<String> {
        public AsyncSharedValueSignal() {
            super(new AsyncTestTree(), Id.ZERO, ANYTHING_GOES, String.class);
        }

        @Override
        public AsyncTestTree tree() {
            return (AsyncTestTree) super.tree();
        }
    }
}
