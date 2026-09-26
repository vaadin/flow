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
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The claim this whole module exists to make: a WAR with no entry point, whose
 * servlet container is a build plugin rather than a dependency, is started and
 * owned by the daemon like any other application.
 * <p>
 * {@link DevLoopLifecycleIT} asks whether the application is up and serving.
 * What is left here is everything that is true only because the runtime is a
 * build plugin.
 */
class DevLoopJettyStartIT extends AbstractDevLoopIT {

    @Test
    void statusNamesTheRuntimeItChose() {
        VaadinDevCli.Outcome outcome = cli.run("status").assertExitCode(0);

        // Not "main": nothing in this project has a main method, and saying so
        // is what tells a developer the daemon understood their project.
        outcome.assertOutputContains("jetty-ee10");
    }

    @Test
    void hotswapAgentIsToldWhichPluginsToLeaveAlone() throws IOException {
        // -DdisabledPlugins never reaches a webapp class loader: HotswapAgent
        // asks the PluginConfiguration of the class's own loader, and builds
        // one per loader from a hotswap-agent.properties found on it. Left at
        // the system property, its Vaadin plugin went on transforming Flow's
        // own classes - a second driver of page reloads, competing with every
        // apply - and the transforms failed because that loader cannot see
        // HotswapAgent either, which apply then read as the app throwing.
        Path properties = APP.resolve("target").resolve("classes")
                .resolve("hotswap-agent.properties");

        assertTrue(Files.isRegularFile(properties),
                () -> "expected " + properties);
        String content = Files.readString(properties);
        assertTrue(content.contains("disabledPlugins="), content);
        assertTrue(content.contains("Vaadin"), content);
        // And the jar itself, so that the plugins left enabled can find the
        // rest of HotswapAgent from the webapp class loader.
        assertTrue(content.contains("extraClasspath="), content);
    }

    @Test
    void theRescannerIsOverriddenWithoutTouchingThePom() throws IOException {
        // This module's pom keeps <scan>2</scan>, exactly as a generated WAR
        // starter ships it, and Maven gives that precedence over
        // the -Djetty.scan=0 on the command line. Left alone, the plugin
        // redeploys the webapp on its own schedule and tears down the context
        // an apply has just hot swapped into. The daemon rewrites the value
        // through a build extension instead, so that no project has to change
        // its pom - and that is what this asserts.
        String log = Files.readString(
                APP.resolve("target").resolve("devloop").resolve("app.log"));

        assertTrue(log.contains("using <scan>0</scan> for this run"), log);
        // The proof it worked: the plugin says so when scanning is off, and a
        // redeploy would have logged "Restart completed".
        assertFalse(log.contains("Restart completed"), log);
    }
}
