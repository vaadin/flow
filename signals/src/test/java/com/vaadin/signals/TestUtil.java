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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;
import com.vaadin.signals.impl.SignalTree;
import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.operations.SignalOperation;
import com.vaadin.signals.operations.SignalOperation.Result;
import com.vaadin.signals.operations.SignalOperation.ResultOrError;

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

    public static JsonNode readConfirmedRootValue(SignalTree tree) {
        return tree.confirmed().data(Id.ZERO).get().value();
    }

    public static JsonNode readSubmittedRootValue(SignalTree tree) {
        return tree.submitted().data(Id.ZERO).get().value();
    }

    public static JsonNode readTransactionRootValue(SignalTree tree) {
        return Transaction.getCurrent().read(tree).data(Id.ZERO).get().value();
    }

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
        return signal.tree();
    }
}
