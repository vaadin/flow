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
package com.vaadin.flow.devloop.test.it;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A CSS edit is pushed in place: no restart, and the classpath copy the next
 * page load would serve is refreshed too.
 * <p>
 * The stylesheet lives in the sibling module, so this is also the cross-module
 * resource leg. Whether the <em>open page</em> re-renders is a browser question
 * and is asserted in {@link DevLoopBrowserIT}; what is checked here is that the
 * bytes the server serves have actually changed, which is the half a reload
 * would otherwise silently get wrong.
 */
class DevLoopCssIT extends AbstractDevLoopIT {

    private static final Path STYLESHEET = SHARED
            .resolve("src/main/resources/META-INF/resources/task-list.css");

    @Test
    void cssEdit_isPushedWithoutARestart() {
        patch.replace(STYLESHEET, "row-gap: 12px;", "row-gap: 33px;");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        outcome.assertOutputContains("hmr:");
        // A stylesheet cannot break a class, so nothing here may restart
        // the app or recompile anything.
        outcome.assertOutputDoesNotContain("restarting");
        outcome.assertOutputDoesNotContain("compiling");
        // The change-set is relative to the application, so a sibling
        // reads as ../devloop-shared/...
        outcome.assertOutputContains("../devloop-shared/");
    }

    @Test
    void cssEdit_refreshesWhatTheServerServes() throws Exception {
        patch.replace(STYLESHEET, "row-gap: 12px;", "row-gap: 44px;");

        cli.run("apply").assertExitCode(0);

        // Flow watches the source tree but never refreshes target/classes,
        // so anything that re-fetches the file - a page reload, a new tab -
        // used to get stale bytes. The daemon copies it first; this is that.
        assertTrue(fetch("/task-list.css").contains("44px"),
                "the served stylesheet should hold the edited value");
    }

    @Test
    void cssAndJavaInOneApply_pushTheStylesheetAndHotSwapTheClass()
            throws Exception {
        // The mixed change-set: the resource leg runs, and then the Java leg
        // decides the outcome. The resource half has to be pushed all the same
        // - a redefine that succeeds says nothing about a stylesheet the page
        // never received, and the reply from that push is what says which
        // happened.
        patch.replace(STYLESHEET, "row-gap: 12px;", "row-gap: 55px;");
        patch.replace(MUTABLE.resolve("TaskListView.java"), "\"Task List\"",
                "\"Tasks, mixed\"");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        outcome.assertOutputContains("hot-reload:");
        outcome.assertOutputDoesNotContain("restarting");
        assertTrue(fetch("/task-list.css").contains("55px"),
                "the served stylesheet should hold the edited value");
    }

    private String fetch(String path) throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient
                .newHttpClient().send(
                        HttpRequest.newBuilder(URI.create(rootUrl() + path))
                                .GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(),
                () -> "GET " + path + " returned " + response.statusCode());
        return response.body();
    }
}
