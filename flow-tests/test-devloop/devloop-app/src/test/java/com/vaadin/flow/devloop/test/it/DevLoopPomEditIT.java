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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * A pom edit touches no source file, so the source scan cannot see it. What can
 * be seen is whether the classpath moved - and only comparing against the one
 * the running JVM was actually launched with gives an honest answer.
 */
class DevLoopPomEditIT extends AbstractDevLoopIT {

    private static final Path POM = APP.resolve("pom.xml");

    /**
     * The comment the application's pom carries for exactly this purpose.
     * {@code </dependencies>} would be the obvious anchor and is the wrong one:
     * it also closes the block inside {@code <dependencyManagement>}, where a
     * dependency is a managed version rather than a classpath entry.
     */
    private static final String ANCHOR = "<!-- DevLoopPomEditIT adds a dependency below this line";

    @Test
    void pomEditThatMovesNothing_reportsNoChangesAndSaysWhy() {
        // A property nothing reads: Maven re-resolves, and neither any module's
        // compile classpath nor the app's runtime classpath moves.
        patch.replace(POM, "<maven.deploy.skip>true</maven.deploy.skip>",
                "<maven.deploy.skip>true</maven.deploy.skip>\n"
                        + "    <devloop.it.marker>1</devloop.it.marker>");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        // A *positive* answer, not a shrug: the edit was seen and deliberately
        // needed no action. A bare "no changes" here would read as "I never
        // noticed your pom edit".
        outcome.assertOutputContains("no changes");
        outcome.assertOutputContains("pom.xml changed");
        outcome.assertOutputDoesNotContain("restarting");
    }

    @Test
    void pomEditThatMovesTheAppClasspath_restartsAndNamesWhatMoved() {
        // A Flow module of this very version: it is in the local repository
        // whenever this test module is buildable at all, and it is certainly
        // not
        // already on the application's classpath. That second half matters -
        // adding something the classpath already carries in another scope moves
        // nothing, and the apply would rightly say "no changes".
        patch.replace(POM, ANCHOR, """
                <dependency>
                  <groupId>com.vaadin</groupId>
                  <artifactId>flow-webpush</artifactId>
                  <version>${project.version}</version>
                </dependency>
                """ + ANCHOR);

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        // A JVM's class path is fixed for its lifetime, so a redefine can
        // never apply this: the runtime leg is skipped rather than attempted
        // and undone.
        outcome.assertOutputContains("restarting");
        outcome.assertOutputContains("classpath changed");
        outcome.assertOutputContains("flow-webpush");
    }

    @Test
    void status_namesTheModulesInTheLoop() {
        // The one thing a developer cannot infer from behaviour until an edit
        // fails to be noticed, and by then they are debugging the wrong thing.
        cli.run("status").assertExitCode(0).assertOutputContains("modules ")
                .assertOutputContains("devloop-shared");
    }
}
