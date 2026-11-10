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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A restart and an escalated apply both produce a new JVM that sees exactly the
 * handshake properties a cold start sees, so the launch kind can only come from
 * the daemon - and telling the three apart is the whole point of the counters.
 */
class DevLoopStatisticsTest {

    private static final String LAUNCH_PROPERTY = "vaadin.devloop.launch";

    @AfterEach
    void clearLaunchProperty() {
        System.clearProperty(LAUNCH_PROPERTY);
    }

    @Test
    void coldStart_isCountedAsAStart() {
        System.setProperty(LAUNCH_PROPERTY, "start");

        assertEquals("devloop/start", DevLoopStatistics.launchEvent());
    }

    @Test
    void explicitRestart_isCountedSeparately() {
        // A developer reaching past apply by hand, which is a different signal
        // from the loop escalating on its own.
        System.setProperty(LAUNCH_PROPERTY, "restart");

        assertEquals("devloop/restart", DevLoopStatistics.launchEvent());
    }

    @Test
    void escalatedApply_isCountedAsAnApplyRestart() {
        // Against devloop/apply this is the ratio that says whether hot swap is
        // actually delivering for a project.
        System.setProperty(LAUNCH_PROPERTY, "apply");

        assertEquals("devloop/apply-restart", DevLoopStatistics.launchEvent());
    }

    @Test
    void missingLaunchKind_stillCountsTheLaunch() {
        // Nothing sets the property but the daemon, so this is the path for a
        // launch it did not name. Counting it coarsely beats dropping it.
        assertEquals("devloop/start", DevLoopStatistics.launchEvent());
    }

    @Test
    void unknownLaunchKind_stillCountsTheLaunch() {
        System.setProperty(LAUNCH_PROPERTY, "something-later");

        assertEquals("devloop/start", DevLoopStatistics.launchEvent());
    }
}
