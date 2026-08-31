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
package com.vaadin.flow.devloop.daemon;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The engine itself needs a running app, so what is unit-tested here is the one
 * verdict it reaches from the app log alone: whether the dev server refused the
 * change. Everything else is covered by {@code flow-tests/test-devloop}.
 */
class TransactionEngineTest {

    private static final String VITE_ERROR = "14.32.37 [vite] Internal server"
            + " error: Transform failed with 1 error:";

    @Test
    void devServerFailure_isTheVerdictForAnErrorLoggedWhenTheFileWasSaved() {
        // Vite compiles on save, so its error is in the log before apply even
        // starts - which is why it is carried across the window boundary - and
        // nothing else the daemon can see says the change is not live.
        TransactionEngine.Transaction tx = frontendChange();
        tx.carriedLogErrors = List.of(VITE_ERROR);

        assertEquals(Optional.of(VITE_ERROR),
                TransactionEngine.devServerFailure(tx));
    }

    @Test
    void devServerFailure_ignoresAnErrorTheApplicationLoggedItself() {
        // An app is free to log an error of its own; failing an apply over one
        // would be a worse answer than the truth.
        TransactionEngine.Transaction tx = frontendChange();
        tx.logErrors = List
                .of("2026-08-31 ERROR 1 --- [http-nio-8080-exec-1] c.e.Service"
                        + "  : could not reach the pricing API");

        assertTrue(TransactionEngine.devServerFailure(tx).isEmpty());
    }

    @Test
    void devServerFailure_needsAFrontendFileInTheChangeSet() {
        // Somebody else's save, mid-apply: this change touched no frontend
        // file, so the dev server cannot be complaining about it.
        TransactionEngine.Transaction tx = new TransactionEngine.Transaction(1);
        tx.logErrors = List.of(VITE_ERROR);

        assertTrue(TransactionEngine.devServerFailure(tx).isEmpty());
    }

    private static TransactionEngine.Transaction frontendChange() {
        TransactionEngine.Transaction tx = new TransactionEngine.Transaction(1);
        tx.frontendFiles = 1;
        return tx;
    }
}
