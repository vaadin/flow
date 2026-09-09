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
 * whether the answer is still this transaction's to give, and how a finished
 * transaction is rendered. Everything else is covered by
 * {@code flow-tests/test-devloop}.
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
    void devServerFailure_needsAFrontendFileInTheChangeSet() {
        // Somebody else's save, mid-apply: this change touched no frontend
        // file, so the dev server cannot be complaining about it.
        TransactionEngine.Transaction tx = new TransactionEngine.Transaction(1);
        tx.logErrors = List.of(VITE_ERROR);

        assertTrue(TransactionEngine.devServerFailure(tx).isEmpty());
    }

    @Test
    void render_reportsThePushAsWellAsTheRedefineForAMixedChangeSet() {
        // A stylesheet and a Java file in one apply: the Java verdict is what
        // classifies the transaction, but the push happened too, and saying
        // nothing about it reads exactly like a push that was skipped.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = mixedChange();

        List<String> lines = engine.render(tx);

        assertTrue(
                lines.contains("hmr: 1 resource(s) copied, pushed 1"
                        + " stylesheet(s) in place"),
                () -> "the frontend half should be reported: " + lines);
        assertTrue(
                lines.contains("hot-reload: redefineClasses(1);"
                        + " onHotswap completed=true"),
                () -> "the Java half should be reported: " + lines);
        // And the reply from the push is not lost to --json readers either.
        assertTrue(
                tx.json().contains(
                        "\"resourcePush\":\"pushed 1 stylesheet(s) in place\""),
                () -> "the push should be in the JSON: " + tx.json());
    }

    @Test
    void render_saysNothingAboutHmrForAChangeSetWithNoFrontendHalf() {
        // The line has to stay absent when there was no frontend work, or it
        // becomes noise a reader cannot tell from a real push.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = mixedChange();
        tx.resources = 0;
        tx.pushDetail = "";

        assertTrue(
                engine.render(tx).stream()
                        .noneMatch(line -> line.startsWith("hmr:")),
                () -> "a Java-only change has no hmr line: "
                        + engine.render(tx));
    }

    /** A stylesheet pushed and a class redefined, in one apply. */
    private static TransactionEngine.Transaction mixedChange() {
        TransactionEngine.Transaction tx = new TransactionEngine.Transaction(1);
        tx.outcome = TransactionEngine.Outcome.STABLE;
        tx.classification = "hot-reload";
        tx.resources = 1;
        tx.pushDetail = "pushed 1 stylesheet(s) in place";
        tx.hotswapDetail = "redefineClasses(1); onHotswap completed=true";
        return tx;
    }

    private static TransactionEngine.Transaction frontendChange() {
        TransactionEngine.Transaction tx = new TransactionEngine.Transaction(1);
        tx.frontendFiles = 1;
        return tx;
    }

}
