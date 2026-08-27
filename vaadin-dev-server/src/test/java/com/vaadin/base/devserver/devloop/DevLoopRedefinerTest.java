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

import org.junit.jupiter.api.Test;

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
}
