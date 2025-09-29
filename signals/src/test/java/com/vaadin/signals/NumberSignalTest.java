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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.operations.SignalOperation;

public class NumberSignalTest extends SignalTestBase {
    @Test
    void constructor_noArgs_zeroValue() {
        NumberSignal signal = new NumberSignal();

        assertEquals(0, signal.value());
    }

    @Test
    void constructor_initialValue_initialValue() {
        NumberSignal signal = new NumberSignal(42);

        assertEquals(42, signal.value());
    }

    @Test
    void incrementBy_concurrentIncrements_allIncrementsConsidered() {
        NumberSignal signal = new NumberSignal();

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
        NumberSignal signal = new NumberSignal(2.718);

        assertEquals(2, signal.valueAsInt());
    }

    @Test
    void value_intOverload_setsTheValue() {
        NumberSignal signal = new NumberSignal();

        signal.value(2);

        assertEquals(2, signal.value());
    }

    @Test
    void withValidator_spyingValidator_seesParentAndChildOperations() {
        NumberSignal signal = new NumberSignal();
        List<SignalCommand> validatedCommands = new ArrayList<>();

        NumberSignal wrapper = signal.withValidator(command -> {
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
        NumberSignal signal = new NumberSignal();

        NumberSignal readonly = signal.asReadonly();

        assertThrows(UnsupportedOperationException.class, () -> {
            readonly.incrementBy(1);
        });
    }

    @Test
    void mapIntValue_simpleIntMapper_valueIsMapped() {
        NumberSignal signal = new NumberSignal();

        Signal<Integer> doubled = signal.mapIntValue(value -> value * 2);
        assertEquals(0, doubled.value());

        signal.value(5);
        assertEquals(10, doubled.value());
    }

    @Test
    void equalsHashCode() {
        NumberSignal signal = new NumberSignal();
        assertEquals(signal, signal);

        NumberSignal copy = new NumberSignal(signal.tree(), signal.id(),
                signal.validator());
        assertEquals(signal, copy);
        assertEquals(signal.hashCode(), copy.hashCode());

        NumberSignal asValue = signal.asNode().asNumber();
        assertEquals(signal, asValue);
        assertEquals(signal.hashCode(), asValue.hashCode());

        assertNotEquals(signal, new NumberSignal());
        assertNotEquals(signal, signal.asReadonly());
        assertNotEquals(signal, signal.asNode());
        assertNotEquals(signal, signal.asNode().asValue(Double.class));
        assertNotEquals(signal.asNode().asValue(Double.class), signal);
    }

    @Test
    void toString_includesValue() {
        NumberSignal signal = new NumberSignal(1);

        assertEquals("NumberSignal[1.0]", signal.toString());
    }

}
