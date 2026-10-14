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

import com.vaadin.base.devserver.stats.DevModeUsageStatistics;
import com.vaadin.flow.internal.UsageStatistics;

/**
 * What the dev loop reports about itself, on the two channels the rest of dev
 * mode uses.
 * <p>
 * Everything is reported from the application JVM, because the daemon runs in
 * its own JVM with no Vaadin classes on its classpath - an enforcer rule bans
 * them - so neither channel is reachable from there.
 * <p>
 * The presence entries say whether a project runs under the loop at all; the
 * counters say how much, which is the part that distinguishes a project that
 * tried the loop once from one using it all day. Both call sites are one line,
 * so neither grows statistics plumbing of its own.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class DevLoopStatistics {

    static final String STATISTIC_DEVLOOP = "flow/devloop";

    static final String STATISTIC_DEVLOOP_APPLY = "flow/devloop/apply";

    /** What the daemon says this launch was for. See {@code Launch.command}. */
    private static final String LAUNCH_PROPERTY = "vaadin.devloop.launch";

    private static final String EVENT_DEVLOOP_START = "devloop/start";

    private static final String EVENT_DEVLOOP_RESTART = "devloop/restart";

    private static final String EVENT_DEVLOOP_APPLY_RESTART = "devloop/apply-restart";

    private static final String EVENT_DEVLOOP_APPLY = "devloop/apply";

    private DevLoopStatistics() {
    }

    /**
     * The daemon launched this application, for the reason it gave.
     */
    static void appLaunched() {
        UsageStatistics.markAsUsed(STATISTIC_DEVLOOP, null);
        DevModeUsageStatistics.collectEvent(launchEvent());
    }

    /**
     * The loop hot-swapped a change into the running application.
     */
    static void changeApplied() {
        UsageStatistics.markAsUsed(STATISTIC_DEVLOOP_APPLY, null);
        DevModeUsageStatistics.collectEvent(EVENT_DEVLOOP_APPLY);
    }

    /**
     * Which launch the daemon said this was.
     * <p>
     * Anything the daemon does not name, or names in a way this version does
     * not know, counts as a plain start: a launch that went uncounted would be
     * worse than one counted a little coarsely.
     *
     * @return the counter name for this launch
     */
    static String launchEvent() {
        return switch (System.getProperty(LAUNCH_PROPERTY, "start")) {
        case "restart" -> EVENT_DEVLOOP_RESTART;
        case "apply" -> EVENT_DEVLOOP_APPLY_RESTART;
        default -> EVENT_DEVLOOP_START;
        };
    }
}
