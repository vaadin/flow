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
package com.vaadin.flow.signals;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;

import com.vaadin.flow.signals.impl.Transaction;
import com.vaadin.flow.signals.operations.SignalOperation;
import com.vaadin.flow.signals.operations.SignalOperation.Result;
import com.vaadin.flow.signals.operations.SignalOperation.ResultOrError;
import com.vaadin.flow.signals.shared.AbstractSignal;
import com.vaadin.flow.signals.shared.SignalUtils;
import com.vaadin.flow.signals.shared.impl.SignalTree;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtil {
    public static SignalCommand writeRootValueCommand(String value) {
        return new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new StringNode(value));
    }

    public static SignalCommand writeRootValueCommand() {
        return writeRootValueCommand("value");
    }

    public static SignalCommand failingCommand() {
        // Fails because the target node doesn't exist (or very unlikely)
        return new SignalCommand.SetCommand(Id.random(), Id.random(), null);
    }

    public static @Nullable JsonNode readConfirmedRootValue(SignalTree tree) {
        return tree.confirmed().data(Id.ZERO).get().value();
    }

    public static @Nullable JsonNode readSubmittedRootValue(SignalTree tree) {
        return tree.submitted().data(Id.ZERO).get().value();
    }

    public static @Nullable JsonNode readTransactionRootValue(
            SignalTree tree) {
        return Transaction.getCurrent().read(tree).data(Id.ZERO).get().value();
    }

    // Result.value() is @Nullable T but successful results always have a value
    @SuppressWarnings("NullAway")
    public static <T> T assertSuccess(SignalOperation<T> operation) {
        if (assertCompleted(operation) instanceof Result<T> result) {
            return result.value();
        } else {
            throw new AssertionError();
        }
    }

    public static void assertFailure(SignalOperation<?> operation) {
        ResultOrError<?> resultOrError = assertCompleted(operation);

        assertFalse(resultOrError.successful());
    }

    private static <T> ResultOrError<T> assertCompleted(
            SignalOperation<T> operation) {
        assertTrue(operation.result().isDone());

        try {
            return operation.result().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    /*
     * Helper to run package-private tree getter from tests in sub packages.
     */
    public static SignalTree tree(AbstractSignal<?> signal) {
        return SignalUtils.treeOf(signal);
    }
}
