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

import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The engine itself needs a running app, so what is unit-tested here are the
 * verdicts it reaches without one: whether the dev server refused the change,
 * and whether the answer is still this transaction's to give. Everything else
 * is covered by {@code flow-tests/test-devloop}.
 */
class TransactionEngineTest {

    private static final String VITE_ERROR = "14.32.37 [vite] Internal server"
            + " error: Transform failed with 1 error:";

    @Test
    void onConnectorClosed_willNotClearAConnectorARestartReplaced() {
        // The predecessor's socket closes on its own schedule, which can be
        // after the new app has registered; clearing then would leave the
        // daemon unable to reach a live app.
        TransactionEngine engine = new TransactionEngine(null, null);
        Connector previous = new Connector(
                new PrintWriter(Writer.nullWriter()));
        Connector restarted = new Connector(
                new PrintWriter(Writer.nullWriter()));
        engine.onConnector(previous);
        engine.onConnector(restarted);

        engine.onConnectorClosed(previous);

        assertSame(restarted, engine.connector());

        engine.onConnectorClosed(restarted);

        assertNull(engine.connector());
    }

    @Test
    void finish_willNotReportSuccessForATransactionANewerApplyTookOver() {
        // Every leg ends here, which is why the check is here: a redefine that
        // held, a restart that came back up and a push that landed are all
        // answers about a state the newer apply has already taken over.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = new TransactionEngine.Transaction(1);
        tx.superseded = true;

        engine.finish(tx, TransactionEngine.Outcome.STABLE, "", "hot-reload",
                "", System.nanoTime());

        assertEquals(TransactionEngine.Outcome.SUPERSEDED, tx.outcome);
        // And it is not what "what is the state?" answers with either.
        assertTrue(engine.lastTransaction().isEmpty());
    }

    @Test
    void finish_keepsAFailureWorthReportingOnASupersededTransaction() {
        // A compile error was true when it happened and is the useful thing to
        // say; only the answers that claim success are rewritten.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = new TransactionEngine.Transaction(1);
        tx.superseded = true;

        engine.finish(tx, TransactionEngine.Outcome.FAILED, "compile", "none",
                "fix the compile error", System.nanoTime());

        assertEquals(TransactionEngine.Outcome.FAILED, tx.outcome);
        assertEquals("compile", tx.reason);
    }

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
    void devServerFailure_isTheVerdictWhenTheDevServerRefusedTheModule() {
        // Asked rather than overheard. Vite compiles a module when something
        // requests it, so with no browser re-fetching there is nothing in the
        // log at all - and the apply used to call that Stable.
        TransactionEngine.Transaction tx = frontendChange();
        tx.devServerAsked = true;
        tx.devServerRefusal = "greeting.ts: Transform failed with 1 error:"
                + " [PARSE_ERROR] Expected `}` but found `EOF`";

        assertEquals(Optional.of(tx.devServerRefusal),
                TransactionEngine.devServerFailure(tx));
    }

    @Test
    void devServerFailure_aServedModuleOverrulesTheErrorStillInTheLog() {
        // The edit that fixes the file: the dev server serves it now, but the
        // log still holds the report from before - and the daemon's own
        // request for the broken version is one of the things that put it
        // there. Trusting the log here fails the apply that fixed the problem.
        TransactionEngine.Transaction tx = frontendChange();
        tx.devServerAsked = true;
        tx.carriedLogErrors = List.of(VITE_ERROR);

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
