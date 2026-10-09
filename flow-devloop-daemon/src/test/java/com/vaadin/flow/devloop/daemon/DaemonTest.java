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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The CLI sends {@code --jvm-args} as a bare marker followed by one
 * {@code --jvm-arg=} word per flag, because the request line is split on
 * whitespace.
 */
class DaemonTest {

    @Test
    void parseJvmArgsOption_isNullWhenNotGiven() {
        // Null rather than empty: a restart without --jvm-args keeps the flags
        // the last launch had, where an empty value clears them.
        assertNull(Daemon.parseJvmArgsOption(List.of()));
        assertNull(Daemon.parseJvmArgsOption(List.of("--json")));
    }

    @Test
    void parseJvmArgsOption_keepsEachFlagAsGiven() {
        // A | or a comma is part of the flag, not a separator.
        assertEquals(
                List.of("-Xmx2g", "--add-exports",
                        "java.base/jdk.internal.misc=ALL-UNNAMED",
                        "-Dpattern=a|b", "-Dlist=a,b"),
                Daemon.parseJvmArgsOption(List.of("--jvm-args",
                        "--jvm-arg=-Xmx2g", "--jvm-arg=--add-exports",
                        "--jvm-arg=java.base/jdk.internal.misc=ALL-UNNAMED",
                        "--jvm-arg=-Dpattern=a|b", "--jvm-arg=-Dlist=a,b")));
    }

    @Test
    void parseJvmArgsOption_markerAloneClears() {
        assertEquals(List.of(),
                Daemon.parseJvmArgsOption(List.of("--jvm-args")));
    }
}
