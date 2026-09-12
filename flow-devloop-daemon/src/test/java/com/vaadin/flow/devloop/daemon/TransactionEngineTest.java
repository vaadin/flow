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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void blockedReason_escalatesForABeanTheRunningApplicationHasNeverHad() {
        // Two half-answers make this verdict: the app says which classes carry
        // a stereotype, the change-set says which of them the application
        // never had. Read from the reply alone the apply answers Stable, and
        // the view that injects the new bean fails with Spring's own exception
        // instead.
        String reply = "OK redefined=1 beans=- structural=-"
                + " stereotypes=com.example.Extra|com.example.TaskService";

        assertEquals(
                Optional.of("new Spring bean (Extra): component scanning ran"
                        + " at startup, so the running context has no"
                        + " definition for it"),
                TransactionEngine.blockedReason(Connector.fields(reply),
                        List.of("com.example.Extra")));
        // The bean the app started with is named in the same field and must
        // not escalate: a method-body change inside it is exactly what the
        // runtime leg exists to swap.
        assertTrue(TransactionEngine
                .blockedReason(Connector.fields(reply), List.of()).isEmpty());
        // Matched on the binary name, so a class that merely shares a simple
        // name with a new one answers for nothing: reporting it would be a
        // restart nobody needed.
        assertTrue(TransactionEngine.blockedReason(
                Connector.fields("OK stereotypes=com.example.Extra"
                        + " entities=- structural=-"),
                List.of("com.example.other.Extra")).isEmpty());
        // And a nested type is its own class, reported under its own name.
        assertEquals(
                Optional.of("new Spring bean (Outer$Inner): component scanning"
                        + " ran at startup, so the running context has no"
                        + " definition for it"),
                TransactionEngine.blockedReason(
                        Connector
                                .fields("OK stereotypes=com.example.Outer$Inner"
                                        + " entities=- structural=-"),
                        List.of("com.example.Outer$Inner")));
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
    void devServerFailure_aServedModuleDoesNotOverruleTheChecker() {
        // The gap a clean fetch cannot close: types are stripped without being
        // checked, so a module with a type error is served with a 200 and the
        // fetch has no opinion on it. Treating that answer as the last word
        // let a broken .tsx through as Stable.
        TransactionEngine.Transaction tx = frontendChange();
        tx.devServerAsked = true;
        tx.checkerFailure = " ERROR(TypeScript)  TS1382: Unexpected token.";

        assertEquals(Optional.of(tx.checkerFailure),
                TransactionEngine.devServerFailure(tx));
    }

    @Test
    void devServerFailure_ignoresACheckerReportTheCheckerHasWithdrawn() {
        // Break a file, save, put it back, save: the checker's report for the
        // broken version is still in the log, but it has since said the
        // project is clean. Reading its errors rather than its verdict failed
        // the apply that repaired the problem.
        TransactionEngine.Transaction tx = frontendChange();
        tx.devServerAsked = true;
        tx.checkerFailure = null;
        tx.carriedLogErrors = List
                .of(" ERROR(TypeScript)  TS2322: Type 'number' is not"
                        + " assignable to type 'string'.");

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
    void reasonRows_keepsTheFirstRowFlushAndIndentsTheRest() {
        // A reason short enough to fit one row is one row, flush left, so it
        // still reads as the line under the verdict and an eye looking for it
        // finds it where it always was.
        assertEquals(List.of("Transform failed"),
                TransactionEngine.reasonRows("Transform failed"));

        // A longer one wraps on a space; the continuation is indented like a
        // quoted log line so it cannot be mistaken for a second reason.
        String reason = "Transform failed with 1 error: [PARSE_ERROR] Expected"
                + " a closing brace but found the end of the file instead,"
                + " which usually means a brace above it was never opened";
        List<String> rows = TransactionEngine.reasonRows(reason);

        assertTrue(rows.size() > 1, rows.toString());
        assertFalse(rows.get(0).startsWith(" "), rows.get(0));
        assertTrue(rows.get(1).startsWith("    "), rows.get(1));
        // Wrapping loses nothing: the rows, re-joined, are the reason back.
        assertEquals(reason,
                String.join(" ", rows).replaceAll("\\s+", " ").strip());
    }

    @Test
    void reasonRows_stopsAtTheRowBudgetWithAnEllipsis() {
        // A compiler can print a wall of text; a verdict quotes only so much of
        // it before it says "...", the same cap a quoted log line gets.
        String wall = ("word ".repeat(400)).strip();

        List<String> rows = TransactionEngine.reasonRows(wall);

        assertEquals("    ...", rows.get(rows.size() - 1));
    }

    @Test
    void finish_dropsAServedDevServerErrorFromTheQuotedLog() {
        // The dev server was asked about these very files and served them all,
        // so a transform error still in the log describes the version this edit
        // replaced - the daemon's own request for the broken one put it there.
        // Quoting it under a Stable verdict reads as a green answer over a
        // broken page, which is the confusion this whole leg exists to prevent.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = frontendChange();
        tx.devServerAsked = true;
        String appOwn = "2026-08-31 ERROR 1 --- [http-nio-8080-exec-1] c.e.Foo"
                + "  : could not reach the pricing API";
        tx.logErrors = List.of(VITE_ERROR, appOwn);

        engine.finish(tx, TransactionEngine.Outcome.STABLE, "", "hmr", "",
                System.nanoTime());

        // The stale dev-server report is gone; the app's own error, which is
        // still about the run, stays and is the one the render quotes.
        assertEquals(List.of(appOwn), tx.logErrors);
        assertTrue(engine.render(tx).stream()
                .noneMatch(line -> line.contains("Transform failed")));
    }

    @Test
    void finish_keepsADevServerErrorTheServerWasNeverAskedAbout() {
        // Only a clean answer overrules the log. When the app could not be
        // asked - too old to know the command, connector unreachable - the log
        // is all there is, so its errors must survive to be quoted.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = frontendChange();
        tx.devServerAsked = false;
        tx.logErrors = List.of(VITE_ERROR);

        engine.finish(tx, TransactionEngine.Outcome.STABLE, "", "hmr", "",
                System.nanoTime());

        assertEquals(List.of(VITE_ERROR), tx.logErrors);
        assertTrue(engine.render(tx).stream()
                .anyMatch(line -> line.startsWith("app log:")));
    }

    @Test
    void finish_dropsACheckerReportTheCheckerHasWithdrawn() {
        // The checker has since said the project type-checks, so a report of
        // its still in the log is superseded and must not be quoted under a
        // verdict that is entirely correct.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = frontendChange();
        tx.checkerFailure = null;
        String checker = " ERROR(TypeScript)  TS2322: Type 'number' is not"
                + " assignable to type 'string'.";
        tx.logErrors = List.of(checker);

        engine.finish(tx, TransactionEngine.Outcome.STABLE, "", "hmr", "",
                System.nanoTime());

        assertTrue(tx.logErrors.isEmpty(), tx.logErrors.toString());
    }

    @Test
    void finish_keepsACheckerReportWhoseVerdictStillStands() {
        // The verdict has not been withdrawn, so its report stays: this is the
        // failure the apply is reporting, not one to filter away.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = frontendChange();
        tx.checkerFailure = "the project does not type-check";
        String checker = " ERROR(TypeScript)  TS2322: Type 'number' is not"
                + " assignable to type 'string'.";
        tx.logErrors = List.of(checker);

        engine.finish(tx, TransactionEngine.Outcome.FAILED,
                "dev server: type error", "hmr", "fix it", System.nanoTime());

        assertEquals(List.of(checker), tx.logErrors);
    }

    @Test
    void finish_mergesCarriedErrorsAheadOfTheWindow() {
        // A dev server compiles on save, so its complaint about a file in this
        // change-set is already in the log before the window opens. It is
        // carried across and folded back in ahead of what the window collected,
        // so no leg reports Stable while an inherited error goes unmentioned.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = frontendChange();
        tx.carriedLogErrors = List.of(VITE_ERROR);
        String appOwn = "2026-08-31 ERROR 1 --- [http-nio-8080-exec-1] c.e.Foo"
                + "  : could not reach the pricing API";
        tx.logErrors = List.of(appOwn);

        engine.finish(tx, TransactionEngine.Outcome.FAILED, "boom", "hmr",
                "fix it", System.nanoTime());

        assertEquals(List.of(VITE_ERROR, appOwn), tx.logErrors);
    }

    @Test
    void render_namesTheViteClauseForAFrontendStable() {
        // A Vite-applied frontend change reports Stable through the hmr leg,
        // and
        // the detail line names what happened to the files and where.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = frontendChange();
        tx.frontendMode = "vite";
        tx.frontend = "up:49401";

        engine.finish(tx, TransactionEngine.Outcome.STABLE, "", "hmr", "",
                System.nanoTime());
        List<String> lines = engine.render(tx);

        assertTrue(lines.get(0).startsWith("frontend → Stable"), lines.get(0));
        assertTrue(
                lines.stream()
                        .anyMatch(line -> line.contains("hmr: ")
                                && line.contains("applied by Vite")
                                && line.contains("up:49401")),
                lines.toString());
    }

    @Test
    void render_wrapsTheReasonUnderAFrontendFailed() {
        // A frontend failure names the frontend phase, not "compiling", and its
        // reason gets its own wrapped row rather than being cut off.
        TransactionEngine engine = new TransactionEngine(null, null);
        TransactionEngine.Transaction tx = frontendChange();
        String reason = "dev server: greeting.ts | Transform failed with 1"
                + " error: [PARSE_ERROR] Expected `}` but found `EOF`";

        engine.finish(tx, TransactionEngine.Outcome.FAILED, reason, "hmr",
                "fix the file", System.nanoTime());
        List<String> lines = engine.render(tx);

        assertEquals("frontend → Failed", lines.get(0));
        assertEquals(reason, String.join(" ", lines.subList(1, lines.size()))
                .replaceAll("\\s+", " ").strip());
    }

    private static TransactionEngine.Transaction frontendChange() {
        TransactionEngine.Transaction tx = new TransactionEngine.Transaction(1);
        tx.frontendFiles = 1;
        return tx;
    }

}
