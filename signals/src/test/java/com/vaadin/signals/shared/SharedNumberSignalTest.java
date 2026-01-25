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
package com.vaadin.signals.shared;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.SignalTestBase;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.core.Signal;
import com.vaadin.signals.operations.SignalOperation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SharedNumberSignalTest extends SignalTestBase {
    @Test
    void constructor_noArgs_zeroValue() {
        SharedNumberSignal signal = new SharedNumberSignal();

        assertEquals(0, signal.value());
    }

    @Test
    void constructor_initialValue_initialValue() {
        SharedNumberSignal signal = new SharedNumberSignal(42);

        assertEquals(42, signal.value());
    }

    @Test
    void incrementBy_concurrentIncrements_allIncrementsConsidered() {
        SharedNumberSignal signal = new SharedNumberSignal();

        SignalOperation<Double> operation = Signal.runInTransaction(() -> {
            SignalOperation<Double> operationInner = signal.incrementBy(1);

            Signal.runWithoutTransaction(() -> {
                signal.incrementBy(2);
                assertEquals(2, signal.value());
            });
            assertEquals(1, signal.value());

            return operationInner;
        }).returnValue();

        assertEquals(3, signal.value());

        Double result = TestUtil.assertSuccess(operation);
        assertEquals(3, result);
    }

    @Test
    void valueAsInt_decimalValue_valueIsTruncated() {
        SharedNumberSignal signal = new SharedNumberSignal(2.718);

        assertEquals(2, signal.valueAsInt());
    }

    @Test
    void value_intOverload_setsTheValue() {
        SharedNumberSignal signal = new SharedNumberSignal();

        signal.value(2);

        assertEquals(2, signal.value());
    }

    @Test
    void withValidator_spyingValidator_seesParentAndChildOperations() {
        SharedNumberSignal signal = new SharedNumberSignal();
        List<SignalCommand> validatedCommands = new ArrayList<>();

        SharedNumberSignal wrapper = signal.withValidator(command -> {
            validatedCommands.add(command);
            return true;
        });

        wrapper.incrementBy(1);

        assertEquals(1, validatedCommands.size());
        assertInstanceOf(SignalCommand.IncrementCommand.class,
                validatedCommands.get(0));
    }

    @Test
    void readonly_makeChangesToListAndChild_allChangesRejected() {
        SharedNumberSignal signal = new SharedNumberSignal();

        SharedNumberSignal readonly = signal.asReadonly();

        assertThrows(UnsupportedOperationException.class, () -> {
            readonly.incrementBy(1);
        });
    }

    @Test
    void mapIntValue_simpleIntMapper_valueIsMapped() {
        SharedNumberSignal signal = new SharedNumberSignal();

        Signal<Integer> doubled = signal.mapIntValue(value -> value * 2);
        assertEquals(0, doubled.value());

        signal.value(5);
        assertEquals(10, doubled.value());
    }

    @Test
    void equalsHashCode() {
        SharedNumberSignal signal = new SharedNumberSignal();
        assertEquals(signal, signal);

        SharedNumberSignal copy = new SharedNumberSignal(signal.tree(),
                signal.id(), signal.validator());
        assertEquals(signal, copy);
        assertEquals(signal.hashCode(), copy.hashCode());

        SharedNumberSignal asValue = signal.asNode().asNumber();
        assertEquals(signal, asValue);
        assertEquals(signal.hashCode(), asValue.hashCode());

        assertNotEquals(signal, new SharedNumberSignal());
        assertNotEquals(signal, signal.asReadonly());
        assertNotEquals(signal, signal.asNode());
        assertNotEquals(signal, signal.asNode().asValue(Double.class));
        assertNotEquals(signal.asNode().asValue(Double.class), signal);
    }

    @Test
    void toString_includesValue() {
        SharedNumberSignal signal = new SharedNumberSignal(1);

        assertEquals("SharedNumberSignal[1.0]", signal.toString());
    }

}
