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
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An edit in the sibling library reaches the running application.
 * <p>
 * This is the case the loop used to miss entirely: run from the application
 * module alone there is no reactor, so a sibling resolved to whatever jar
 * happened to be installed and the edit was invisible. The daemon resolves
 * through {@code -pl :app -am}, drops the sibling's installed jar from the
 * classpath, and compiles into the sibling's own output.
 */
class DevLoopMultiModuleIT extends AbstractDevLoopIT {

    private static final Path FORMATTER = SHARED.resolve(
            "src/main/java/com/vaadin/flow/devloop/test/shared/DueDateFormatter.java");

    @Test
    void siblingModuleEdit_isCompiledAndAppliedLikeTheApplicationsOwn() {
        patch.replace(FORMATTER, "\"Never\"", "\"No due date\"");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        // Named relative to the application, so a path an agent is handed is a
        // path it can open from where the CLI runs.
        outcome.assertOutputContains(
                "../devloop-shared/src/main/java/com/vaadin/flow/devloop/test/shared/DueDateFormatter.java");
    }

    @Test
    void siblingModuleClassesAreCompiledIntoItsOwnOutput() throws IOException {
        // javac takes a single -d, so a shared compile pass would write this
        // class into the application's output, where Maven would never look for
        // it again and where the next mvn run would not delete it either.
        Path expected = SHARED.resolve(
                "target/classes/com/vaadin/flow/devloop/test/shared/DueDateFormatter.class");
        FileTime before = Files.getLastModifiedTime(expected);

        patch.replace(FORMATTER, "FormatStyle.MEDIUM", "FormatStyle.FULL");
        cli.run("apply").assertExitCode(0).assertOutputContains("Stable");

        assertTrue(Files.getLastModifiedTime(expected).compareTo(before) > 0,
                () -> expected + " should have been recompiled");
        assertFalse(Files.exists(APP.resolve(
                "target/classes/com/vaadin/flow/devloop/test/shared/DueDateFormatter.class")),
                "the sibling's class must not land in the application's output");
        // Whether the *page* shows it is a browser question, and one this class
        // is not loaded for: nothing has rendered the view in this test, so the
        // formatter is not in the JVM yet and there is nothing to redefine.
        // DevLoopBrowserIT asserts that half.
    }

    @Test
    void compileErrorInTheSibling_isNamedByItsModule() {
        patch.replace(FORMATTER, "return dueDate == null",
                "return nope() == null");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(1);

        // "DueDateFormatter.java:30" in a reactor is not enough to open a
        // file with, so one outside the application is prefixed by its module.
        outcome.assertOutputContains("devloop-shared/DueDateFormatter.java");
    }
}
