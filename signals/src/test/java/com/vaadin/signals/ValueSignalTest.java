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
package com.vaadin.signals;

import static com.vaadin.signals.TestUtil.assertFailure;
import static com.vaadin.signals.TestUtil.assertSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.SignalCommand.SetCommand;
import com.vaadin.signals.impl.AsynchronousSignalTreeTest.AsyncTestTree;
import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;
import com.vaadin.signals.operations.CancelableOperation;
import com.vaadin.signals.operations.SignalOperation;
import com.vaadin.signals.operations.TransactionOperation;

public class ValueSignalTest extends SignalTestBase {
    /*
     * Note that there's no test specific to the generic Signal class but most
     * of that functionality is instead tested through its simplest subclass,
     * i.e. ValueSignal
     */

    @Test
    void constructor_type_noValueAndTypeIsUsed() {
        ValueSignal<String> signal = new ValueSignal<>(String.class);
        assertNull(signal.value());

        signal.value("a string");
        assertEquals("a string", signal.value());

        assertThrows(AssertionError.class, () -> {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            ValueSignal<Object> raw = ((ValueSignal) signal);

            raw.value(new Object());
        });
        assertEquals("a string", signal.value());
    }

    @Test
    void constructor_initialValue_valueUsedAndTypeIsInferred() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        assertEquals("initial", signal.value());

        signal.value("a string");
        assertEquals("a string", signal.value());

        assertThrows(AssertionError.class, () -> {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            ValueSignal<Object> raw = ((ValueSignal) signal);

            raw.value(new Object());
        });
        assertEquals("a string", signal.value());
    }

    @Test
    void constructor_nullType_throws() {
        assertThrows(NullPointerException.class, () -> {
            Class<String> type = null;
            new ValueSignal<>(type);
        });
    }

    @Test
    void constructor_nullInitialValue_throws() {
        assertThrows(NullPointerException.class, () -> {
            String initial = null;
            new ValueSignal<>(initial);
        });
    }

    @Test
    void value_mutateValueInstance_signalValueUnaffected() {
        String[] array = { "initial" };
        ValueSignal<String[]> signal = new ValueSignal<>(array);

        array[0] = "modified";
        assertEquals("initial", signal.value()[0]);

        signal.value()[0] = "modified";
        assertEquals("initial", signal.value()[0]);
    }

    @Test
    void value_hasPrevious_previousInResult() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        SignalOperation<String> operation = signal.value("update");
        assertEquals("update", signal.value());

        String resultValue = assertSuccess(operation);
        assertEquals("initial", resultValue);
    }

    @Test
    void peekConfirmed_hasUnconfirmedChange_changeIngored() {
        AsyncValueSignal signal = new AsyncValueSignal();
        signal.value("update");

        assertEquals("update", signal.value());
        assertNull(signal.peekConfirmed());

        signal.tree().confirmSubmitted();

        assertEquals("update", signal.peekConfirmed());
    }

    @Test
    void replace_expectedValue_successfulResult() {
        ValueSignal<String> signal = new ValueSignal<>("expected");

        SignalOperation<Void> operation = signal.replace("expected", "update");
        assertEquals("update", signal.value());

        assertSuccess(operation);
    }

    @Test
    void replace_unexpectedValue_failedlResult() {
        ValueSignal<String> signal = new ValueSignal<>("unexpected");

        SignalOperation<Void> operation = signal.replace("expected", "update");
        assertEquals("unexpected", signal.value());

        assertFailure(operation);
    }

    @Test
    void update_noConflict_updatedWithPreviousValueInResult() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        CancelableOperation<String> operation = signal.update(previous -> {
            assertEquals("initial", previous);
            return "update";
        });

        assertEquals("initial", assertSuccess(operation));
    }

    @Test
    void update_cancelWithConflict_noFurtherInvocationsAndCancelledResult() {
        AsyncValueSignal signal = new AsyncValueSignal();

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
        AsyncValueSignal signal = new AsyncValueSignal();

        CancelableOperation<String> operation = signal.update(previous -> {
            assertNull(previous);

            return "update";
        });

        operation.cancel();

        signal.tree().confirmSubmitted();
        assertNull(assertSuccess(operation));
        assertEquals("update", signal.value());
    }

    @Test
    void update_conflictsNoCancel_eventuallySucceeds() {
        ValueSignal<Integer> signal = new ValueSignal<>(Integer.valueOf(0));

        CancelableOperation<Integer> operation = signal.update(previous -> {
            if (previous < 5) {
                // Provoke a conflict while still making progress
                Signal.runWithoutTransaction(() -> {
                    signal.value(previous + 1);
                });
            }

            return previous + 1;
        });

        assertEquals(5, assertSuccess(operation));
        assertEquals(6, signal.value());
    }

    @Test
    void verifyValue_expectedValue_operationSuccessful() {
        ValueSignal<String> signal = new ValueSignal<>("expected");

        SignalOperation<Void> operation = signal.verifyValue("expected");

        assertSuccess(operation);
    }

    @Test
    void verifyValue_unexpectedValue_operationSuccessful() {
        ValueSignal<String> signal = new ValueSignal<>("unexpected");

        SignalOperation<Void> operation = signal.verifyValue("expected");

        assertFailure(operation);
    }

    @Test
    void withValidator_acceptsOperation_operationAccepted() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        List<SignalCommand> validatedCommands = new ArrayList<>();

        ValueSignal<String> wrapper = signal.withValidator(command -> {
            validatedCommands.add(command);
            return true;
        });

        wrapper.value("update");

        assertEquals("update", signal.value());
        assertEquals(1, validatedCommands.size());
        assertInstanceOf(SignalCommand.SetCommand.class,
                validatedCommands.get(0));
    }

    @Test
    void withValidator_rejectsNullValues_nullRejectedAndOtherAccepted() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        ValueSignal<String> wrapper = signal.withValidator(command -> {
            if (command instanceof SetCommand set) {
                return !set.value().isNull();
            }
            return true;
        });

        SignalOperation<String> updateResult = wrapper.value("update");
        assertSuccess(updateResult);
        assertEquals("update", wrapper.value());

        assertThrows(UnsupportedOperationException.class, () -> {
            wrapper.value(null);
        });
        assertEquals("update", wrapper.value());
    }

    @Test
    void withValidator_changeThroughOriginal_validatorNotInvokedAndWrapperUpdated() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        ValueSignal<String> wrapper = signal.withValidator(command -> {
            throw new RuntimeException();
        });

        signal.value("update");

        assertEquals("update", wrapper.value());
    }

    @Test
    void withValidator_verifyCommand_validatorNotInvoked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        ValueSignal<String> wrapper = signal.withValidator(command -> {
            throw new RuntimeException();
        });

        SignalOperation<Void> result = wrapper.verifyValue("initial");
        assertSuccess(result);
    }

    @Test
    void withValidator_inTransaction_validatorInvokedForChildren() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        List<SignalCommand> validatedCommands = new ArrayList<>();

        ValueSignal<String> wrapper = signal.withValidator(command -> {
            validatedCommands.add(command);
            return true;
        });

        Signal.runInTransaction(() -> {
            wrapper.value("update");
        });

        assertEquals(1, validatedCommands.size());
        assertInstanceOf(SetCommand.class, validatedCommands.get(0));
    }

    @Test
    void readonly_makeChanges_changesRejected() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        ValueSignal<String> readonly = signal.asReadonly();

        assertThrows(UnsupportedOperationException.class, () -> {
            readonly.value("Update");
        });

        assertEquals("initial", readonly.value());
    }

    @Test
    void usageTracking_changingSignalValue_usageDetectsValueChange() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        AtomicInteger count = new AtomicInteger();

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        assertFalse(usage.hasChanges());

        usage.onNextChange(() -> {
            count.incrementAndGet();
            return false;
        });

        signal.value("update");
        assertEquals(1, count.intValue());
        assertTrue(usage.hasChanges());

        signal.value("anohter");
        assertEquals(1, count.intValue());
    }

    @Test
    void usageTracking_repeatingChangeListener_usageDetectsFollowingValueChange() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        AtomicInteger count = new AtomicInteger();

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        usage.onNextChange(() -> {
            count.incrementAndGet();
            return true;
        });

        signal.value("update");
        assertEquals(1, count.intValue());

        signal.value("anohter");
        assertEquals(2, count.intValue());
    }

    @Test
    void usageTracking_noOpChange_listenerNotNotifiedButRemainsActive() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        AtomicInteger count = new AtomicInteger();

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        usage.onNextChange(() -> {
            count.incrementAndGet();
            return false;
        });

        signal.value("initial");
        assertEquals(0, count.intValue());
        assertFalse(usage.hasChanges());

        signal.value("update");
        assertEquals(1, count.intValue());
        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracking_unrelatedChange_listenerNotNotifiedButRemainsActive() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        AtomicInteger count = new AtomicInteger();

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        usage.onNextChange(() -> {
            count.incrementAndGet();
            return false;
        });

        signal.asNode().putChildWithValue("key", "value");
        assertEquals(0, count.intValue());
        assertFalse(usage.hasChanges());

        signal.value("update");
        assertEquals(1, count.intValue());
        assertTrue(usage.hasChanges());
    }

    @Test
    void usageTracking_registerAfterChange_listenerCalledImmediately() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        AtomicInteger falseCount = new AtomicInteger();
        AtomicInteger trueCount = new AtomicInteger();

        Usage usage = UsageTracker.track(() -> {
            signal.value();
        });

        signal.value("update");

        usage.onNextChange(() -> {
            falseCount.incrementAndGet();
            return false;
        });
        assertEquals(1, falseCount.intValue());

        usage.onNextChange(() -> {
            trueCount.incrementAndGet();
            return true;
        });
        assertEquals(1, trueCount.intValue());

        signal.value("again");
        assertEquals(1, falseCount.intValue());
        assertEquals(2, trueCount.intValue());
    }

    @Test
    void result_successfulOperation_resolvedThroughOverrideDispatcher() {
        TestExecutor dispatcher = useTestOverrideDispatcher();

        ValueSignal<String> signal = new ValueSignal<>(String.class);
        SignalOperation<String> operation = signal.value("update");

        assertFalse(operation.result().isDone());
        assertEquals(1, dispatcher.countPendingTasks());

        dispatcher.runPendingTasks();
        assertTrue(operation.result().isDone());
        assertEquals(0, dispatcher.countPendingTasks());
    }

    @Test
    void result_failingOperation_resolvedThroughOverrideDispatcher() {
        TestExecutor dispatcher = useTestOverrideDispatcher();

        ValueSignal<String> signal = new ValueSignal<>(String.class);
        SignalOperation<Void> operation = signal.replace("other", "update");

        assertFalse(operation.result().isDone());
        assertEquals(1, dispatcher.countPendingTasks());

        dispatcher.runPendingTasks();
        assertTrue(operation.result().isDone());
        assertEquals(0, dispatcher.countPendingTasks());
    }

    @Test
    void result_onlyBaseDispatcher_resolvedDirectly() {
        TestExecutor dispatcher = useTestDispatcher();

        ValueSignal<String> signal = new ValueSignal<>(String.class);
        SignalOperation<Void> operation = signal.replace("other", "update");

        assertTrue(operation.result().isDone());
        assertEquals(0, dispatcher.countPendingTasks());
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
        ValueSignal<String> signal = new ValueSignal<>("value");

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
        ValueSignal<String> signal = new ValueSignal<>("value");

        Signal.runInTransaction(() -> {
            // Read to make signal participate in transaction
            signal.value();

            Signal.runWithoutTransaction(() -> {
                signal.value("update");
            });

            assertEquals("value", signal.value());
        });
        assertEquals("update", signal.value());
    }

    @Test
    void transaction_writeValue_writtenThroughTransaction() {
        ValueSignal<String> signal = new ValueSignal<>("value");

        Signal.runInTransaction(() -> {
            signal.value("update");

            Signal.runWithoutTransaction(() -> {
                assertEquals("value", signal.value());
            });
        });
        assertEquals("update", signal.value());
    }

    @Test
    void transaction_readInTransactionAndChangeOutside_transactionFails() {
        ValueSignal<String> signal = new ValueSignal<>("value");

        TransactionOperation<Void> operation = Signal.runInTransaction(() -> {
            signal.value(signal.value() + " update");

            Signal.runWithoutTransaction(() -> {
                signal.value("update");
            });
        });

        assertFailure(operation);
        assertEquals("update", signal.value());
    }

    @Test
    void transaction_peekInTransactionAndChangeOutside_transactionSuccessful() {
        ValueSignal<String> signal = new ValueSignal<>("value");

        TransactionOperation<Void> operation = Signal.runInTransaction(() -> {
            signal.value(signal.peek() + " update");

            Signal.runWithoutTransaction(() -> {
                signal.value("update");
            });
        });

        assertSuccess(operation);
        assertEquals("value update", signal.value());
    }

    @Test
    void transaction_peekConfirmedInTransactionAndChangeOutside_transactionSuccessful() {
        ValueSignal<String> signal = new ValueSignal<>("value");

        TransactionOperation<Void> operation = Signal.runInTransaction(() -> {
            signal.value(signal.peekConfirmed() + " update");

            Signal.runWithoutTransaction(() -> {
                signal.value("update");
            });
        });

        assertSuccess(operation);
        assertEquals("value update", signal.value());
    }

    @Test
    void equalsHashCode() {
        ValueSignal<String> signal = new ValueSignal<>(String.class);
        assertEquals(signal, signal);

        ValueSignal<String> copy = new ValueSignal<>(signal.tree(), signal.id(),
                signal.validator(), String.class);
        assertEquals(signal, copy);
        assertEquals(signal.hashCode(), copy.hashCode());

        ValueSignal<String> asValue = signal.asNode().asValue(String.class);
        assertEquals(signal, asValue);
        assertEquals(signal.hashCode(), asValue.hashCode());

        assertNotEquals(signal, new ValueSignal<>(String.class));
        assertNotEquals(signal, signal.asReadonly());
        assertNotEquals(signal, signal.asNode());
        assertNotEquals(signal, signal.asNode().asValue(Double.class));
    }

    @Test
    void toString_includesValue() {
        ValueSignal<String> signal = new ValueSignal<>("signal value");

        assertEquals("ValueSignal[signal value]", signal.toString());
    }

    public static class AsyncValueSignal extends ValueSignal<String> {
        public AsyncValueSignal() {
            super(new AsyncTestTree(), Id.ZERO, ANYTHING_GOES, String.class);
        }

        @Override
        public AsyncTestTree tree() {
            return (AsyncTestTree) super.tree();
        }
    }
}
