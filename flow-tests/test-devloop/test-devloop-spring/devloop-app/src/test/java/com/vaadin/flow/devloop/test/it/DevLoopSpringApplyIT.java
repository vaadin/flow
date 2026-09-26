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

import org.junit.jupiter.api.Test;

/**
 * The gate {@code --no-restart} stops at, which needs a change that escalates -
 * and that is where this fixture differs.
 * <p>
 * {@link DevLoopApplyIT} has what an apply does anywhere.
 */
class DevLoopSpringApplyIT extends AbstractDevLoopIT {

    @Test
    void noRestart_stopsAfterTheCompileGate() {
        // A new member on a bean, which escalates because the live proxy was
        // generated against the old shape - not because the edit is structural.
        // Measured: the same edit to the plain class in test-devloop-jetty is a
        // clean hot swap on a JVM with enhanced class redefinition, so this
        // belongs to the fixture with a Spring context rather than to the
        // shared ITs. The gate itself is exercised in both fixtures by
        // DevLoopFrontendIT, where a bundled frontend edit escalates whoever
        // launched the application.
        patch.addMember(SERVICE,
                "\n    public int added() {\n        return 1;\n    }\n");

        // Structural for the proxy, so the only way to make it live is a
        // restart - which --no-restart forbids. The honest answer is
        // "compiled", not "Stable".
        cli.run("apply", "--no-restart").assertExitCode(0)
                .assertOutputContains("compiled")
                .assertOutputDoesNotContain("restarting");
    }
}
