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
package com.vaadin.base.devserver.devloop;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The connector must always answer exactly one line, even on failure, or the
 * daemon blocks waiting for a reply that is not coming.
 * <p>
 * Only the no-agent and no-hotswapper paths are unit-testable: everything past
 * them needs a real {@code Instrumentation} handle and a running application.
 * The rest is covered end to end in {@code flow-tests/test-devloop}.
 */
class DevLoopRedefinerTest {

    @Test
    void redefine_withoutAnAgent_reportsTheReasonInTheProtocolVocabulary() {
        String reply = DevLoopRedefiner.redefine("com.example.Foo");

        // The daemon parses "status" from the first token and escalates to a
        // restart on anything but OK, so the kind has to be machine-readable.
        assertTrue(reply.startsWith("ERR kind="), reply);
        assertTrue(reply.contains("kind=no-agent")
                || reply.contains("kind=no-hotswapper"), reply);
    }

    @Test
    void resources_withoutAHotswapper_reportsIt() {
        String reply = DevLoopRedefiner.resources("/tmp/a.css");

        assertTrue(reply.startsWith("ERR kind=no-hotswapper"), reply);
    }

    @Test
    void info_answersEvenWithNothingRegistered() {
        String reply = DevLoopRedefiner.info();

        // status() is what a daemon reads first; an unregistered app still has
        // to say so in one line rather than throw.
        assertTrue(reply.startsWith("OK "), reply);
        assertTrue(reply.contains("hotswapper=false"), reply);
        assertTrue(reply.contains("frontend=unknown"), reply);
    }

    @Test
    void frontendStatus_withoutAService_isUnknown() {
        assertTrue("unknown".equals(DevLoopRedefiner.frontendStatus()));
    }

    @Test
    void theme_withoutAService_reportsIt() {
        String reply = DevLoopRedefiner.theme("/tmp/themes/t/styles.css");

        assertTrue(reply.startsWith("ERR kind=no-service"), reply);
    }

    @Test
    void reload_withoutABrowser_stillAnswersInOneLine() {
        String reply = DevLoopRedefiner.reload();

        // No live reload to ask means no browser was reloaded, which is a fact
        // the daemon reports rather than an error it fails on.
        assertTrue(reply.equals("OK reloaded=false"), reply);
    }

    @Test
    void publicRootOf_takesTheLastWholeSegment() {
        // The marker directory is part of the root: the URL the browser knows
        // is the path below it, so a root short by one segment pushes CSS under
        // a URL that matches nothing on the page.
        assertEquals(file("/p/src/main/resources/META-INF/resources"),
                DevLoopRedefiner.publicRootOf(
                        "/p/src/main/resources/META-INF/resources/app.css"));

        // "/resources/" also occurs in "src/main/resources", earlier. Taking
        // the first match would make the URL "META-INF/resources/app.css"
        // above, and "resources/a.css" here.
        assertEquals(file("/p/src/main/resources/resources"), DevLoopRedefiner
                .publicRootOf("/p/src/main/resources/resources/a.css"));

        // A directory named like a marker anywhere above the real root must not
        // win, however deep the checkout sits.
        assertEquals(file("/static-site/p/src/main/resources/static"),
                DevLoopRedefiner.publicRootOf(
                        "/static-site/p/src/main/resources/static/a.css"));

        assertEquals(file("C:/p/src/main/webapp"), DevLoopRedefiner
                .publicRootOf("C:\\p\\src\\main\\webapp\\css\\a.css"));

        // Under no public root at all, so there is nothing to resolve against
        // and the caller falls back to a reload.
        assertNull(DevLoopRedefiner
                .publicRootOf("/p/src/main/java/com/example/View.java"));
    }

    private static File file(String path) {
        return new File(path);
    }

    @Test
    void frontend_carriesTheFieldsTheDaemonDecidesModeWith() {
        String reply = DevLoopRedefiner.frontend(null);

        // frontend= keeps its exact previous meaning and position, so a daemon
        // reading only that field is unaffected by the added ones.
        assertTrue(reply.startsWith("OK frontend=unknown"), reply);
        assertTrue(reply.contains(" mode="), reply);
        assertTrue(reply.contains(" themes="), reply);
        assertTrue(reply.contains(" agree=?"), reply);
    }
}
