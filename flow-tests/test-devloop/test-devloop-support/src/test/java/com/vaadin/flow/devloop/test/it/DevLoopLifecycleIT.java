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

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The daemon owns one application process, and it is up and serving.
 * <p>
 * The claim each fixture exists to make, asked the same way of both: whatever
 * launched the application - a JVM the daemon composed a command line for, or
 * the project's own build plugin - what a browser gets from it is a Flow page
 * on the port the daemon reported, a second {@code start} is an answer rather
 * than a rival for that port, and a restart brings the same application back.
 * Which runtime was chosen, and what that runtime needs to be told, is the part
 * each fixture asserts for itself.
 */
class DevLoopLifecycleIT extends AbstractDevLoopIT {

    @Test
    void theApplicationIsRunningAndServing() throws Exception {
        HttpResponse<String> response = get("/");

        assertEquals(200, response.statusCode());
        // Flow's bootstrap page, so the servlet really is registered. Asserted
        // on Flow's own marker rather than on a doctype: Flow writes
        // "<!doctype html>" in lower case, and any HTML at all would satisfy a
        // doctype check anyway.
        assertTrue(response.body().contains("window.Vaadin"), response.body());
    }

    @Test
    void aSecondStartIsAnAnswerRatherThanASecondApplication() {
        // The daemon owns one process; asking again must not start a rival for
        // the port.
        cli.run("start").assertExitCode(0)
                .assertOutputContains("already running");
    }

    @Test
    void theResourceLegServesFromTheClasspath() throws Exception {
        // Shipped by the sibling module under META-INF/resources, which is how
        // a jar on the class path carries a static file - and the reason
        // neither fixture needs a src/main/webapp or a resource handler of its
        // own.
        String body = fetch("/task-list.css");

        // The bytes, not just the status: a path the server has nothing for
        // falls through to Flow's route, which answers 200 with the bootstrap
        // page, so a status on its own says nothing about who served it.
        assertTrue(body.contains(".task-list-view"), body);
    }

    @Test
    void startingLeavesNoErrorsInTheApplicationLog() {
        // A green status over a log full of startup failures is not a green
        // result: the daemon surfaces those under "app log:", and this is what
        // says it has nothing to surface.
        cli.run("status").assertExitCode(0)
                .assertOutputDoesNotContain("app log:");
    }

    @Test
    void statusJson_isASingleParseableObject() {
        VaadinDevCli.Outcome outcome = cli.run("status", "--json")
                .assertExitCode(0);

        String json = outcome.output().strip();
        assertTrue(json.startsWith("{") && json.endsWith("}"),
                () -> "expected one JSON object and no progress lines, got:\n"
                        + json);
        outcome.assertOutputContains("\"state\":\"running\"");
    }

    @Test
    void restart_bringsTheApplicationBackUpOnTheSamePort() {
        cli.run("restart").assertExitCode(0);

        cli.run("status").assertExitCode(0).assertOutputContains("running")
                .assertOutputContains("registered=true");
    }
}
