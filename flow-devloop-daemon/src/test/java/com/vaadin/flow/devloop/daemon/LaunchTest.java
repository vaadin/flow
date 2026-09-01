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

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Classpath membership is what decides whether a pom edit restarts the app.
 * Maven reorders a classpath for edits that change nothing about what is on it,
 * and restarting an app to hand it the same jars in a different sequence is
 * noise a developer cannot tell from a whim.
 */
class LaunchTest {

    @Test
    void membership_ignoresOrder() {
        assertEquals(Launch.membership(classpath("b.jar", "a.jar")),
                Launch.membership(classpath("a.jar", "b.jar")));
    }

    @Test
    void membership_ignoresDuplicates() {
        assertEquals(Launch.membership(classpath("a.jar")),
                Launch.membership(classpath("a.jar", "a.jar")));
    }

    @Test
    void membership_seesAnAddedEntry() {
        assertNotEquals(Launch.membership(classpath("a.jar")),
                Launch.membership(classpath("a.jar", "b.jar")));
    }

    @Test
    void cacheDir_isMachineLevelRatherThanPerProject() {
        Path cache = Launch.cacheDir();

        // One HotswapAgent download per machine, and nothing written into a
        // project - which also means mvn clean does not throw it away.
        assertEquals(
                Path.of(System.getProperty("user.home"), ".vaadin", "devloop"),
                cache);
    }

    @Test
    void forwardedToApp_carriesTheVaadinAndSpringNamespaces() {
        // VAADIN_DEV_DAEMON_OPTS is the only channel there is - the app is
        // launched with no program arguments - so anything a developer has to
        // set on the application has to come through here.
        assertTrue(Launch.forwardedToApp("spring.profiles.active"));
        assertTrue(Launch.forwardedToApp("spring.datasource.url"));
        assertTrue(Launch.forwardedToApp("spring.main.banner-mode"));
        // The steering case this allowlist already existed for.
        assertTrue(Launch.forwardedToApp("vaadin.frontend.hotdeploy"));
    }

    @Test
    void forwardedToApp_holdsBackWhatTheLoopItselfSets() {
        // These three are put on the app's command line with the value the loop
        // requires. The forwarding runs after them and a later -D wins, so a
        // forwarded copy does not merely duplicate - it overrides. For
        // devtools that would put Spring's own restart back in the ring
        // against the daemon, which is the one thing the loop cannot share.
        assertFalse(Launch.forwardedToApp("spring.devtools.restart.enabled"));
        assertFalse(Launch.forwardedToApp("vaadin.launch-browser"));
        assertFalse(Launch.forwardedToApp("vaadin.devloop.classes"));
    }

    @Test
    void forwardedToApp_leavesTheDaemonsOwnJvmPropertiesBehind() {
        // Not "forward everything": these describe the daemon's process, and
        // another process's answers are worse than none.
        assertFalse(Launch.forwardedToApp("user.dir"));
        assertFalse(Launch.forwardedToApp("java.class.path"));
        assertFalse(Launch.forwardedToApp("os.name"));
    }

    private static String classpath(String... entries) {
        return String.join(File.pathSeparator, entries);
    }
}
