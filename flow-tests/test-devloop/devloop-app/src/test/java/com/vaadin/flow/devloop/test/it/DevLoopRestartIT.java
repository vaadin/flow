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

import javax.tools.ToolProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The changes a redefine cannot make live, and the reason each one gives.
 * <p>
 * Every case here is one that used to be reported as live: what the application
 * built at startup - a bean's proxy, an ORM's metamodel, the set of bean
 * definitions - silently no longer matches the sources, and the redefine that
 * says so is either accepted or not needed at all.
 */
class DevLoopRestartIT extends AbstractDevLoopIT {

    private static final Path VIEW = MUTABLE.resolve("TaskListView.java");

    @Test
    void newMemberOnAProxiedBean_restartsAndStaysUp() {
        patch.addMember(MUTABLE.resolve("TaskService.java"),
                "\n    public String added() {\n        return \"added\";\n    }\n");

        VaadinDevCli.Outcome outcome = cli.run("apply").assertExitCode(0);

        outcome.assertOutputContains("restarting");
        outcome.assertOutputContains("Stable");
        // Not just that it restarted, but why: "restarting" with no reason is
        // indistinguishable from the daemon deciding to restart on a whim.
        outcome.assertOutputContains("restart:");

        cli.run("status").assertExitCode(0).assertOutputContains("running");
    }

    @Test
    void addingAnEntityAnnotation_escalatesEvenThoughTheClassRedefines() {
        // A class-level annotation is an attribute rather than a member, so the
        // redefine is accepted and the class really does come back carrying
        // @Entity. Hibernate mapped neither version, though - the metamodel and
        // schema were fixed at startup - so this has to escalate. Asked only of
        // the class the application is running, the annotation is not there
        // yet, and the apply reports as live the very change that is not.
        patch.replace(VIEW, "public class TaskListView extends Div {",
                "@jakarta.persistence.Entity\npublic class TaskListView extends Div {");

        // --no-restart stops at the verdict, which is what is under test; the
        // restart itself is the case above.
        VaadinDevCli.Outcome outcome = cli
                .run("apply", "--no-restart", "--json").assertExitCode(0);

        outcome.assertOutputContains(
                "entity mapping cannot hot reload (TaskListView)");
    }

    @ParameterizedTest(name = "@{0}")
    @ValueSource(strings = { "Component", "Service" })
    void aNewSpringBean_escalatesEvenThoughNothingWasRedefined(
            String stereotype) {
        // The case with no redefine behind it: a class the running JVM has
        // never loaded has nothing to swap, so every signal the two cases above
        // turn on is empty and the apply reported Stable. Component scanning is
        // a startup act, though, so the context has no definition for the new
        // bean and the first view to inject it fails with Spring's own
        // NoSuchBeanDefinitionException - which names Spring rather than the
        // restart nobody was told to do.
        //
        // The change-set is deliberately one new file and no edit to a loaded
        // class. Taking the new bean as a constructor parameter of a view that
        // is already running would restart on a stock JVM whatever this
        // reports, because adding a parameter is a structural change and
        // redefineClasses rejects it - so the fixture would pass without the
        // rule it is here to pin, and only a JVM with enhanced class
        // redefinition would show the difference.
        //
        // Two stereotypes rather than one, and only two: @Component is the
        // one every other is composed from and @Service the one an
        // application reaches for, so between them they show the rule is
        // about the change-set rather than about a particular annotation.
        // That each remaining entry of the list matches its own descriptor is
        // pinned in DevLoopRedefinerTest, where it costs no application.
        //
        // What this one cannot see, and did not: setUp applies before the
        // fixture exists, so the daemon's baseline is already built and
        // already seeded by the time the file appears. A miss that needs the
        // file to appear *before* the first apply - which is what a developer
        // does, and what was reported - is invisible from here however many
        // stereotypes it runs. That ordering has its own case below.
        String type = "Extra" + stereotype;
        patch.create(MUTABLE.resolve(type + ".java"), """
                package com.vaadin.flow.devloop.test.app.mutable;

                import org.springframework.stereotype.%1$s;

                /** Created by DevLoopRestartIT and deleted again by it. */
                @%1$s
                public class %2$s {
                }
                """.formatted(stereotype, type));

        // --no-restart stops at the verdict, as in the case above.
        VaadinDevCli.Outcome outcome = cli
                .run("apply", "--no-restart", "--json").assertExitCode(0);

        outcome.assertOutputContains("new Spring bean (" + type + ")");
    }

    @ParameterizedTest(name = "compiled by the daemon: {0}")
    @ValueSource(booleans = { true, false })
    void aNewSpringBean_escalatesOnTheFirstApplyOfADaemonsLife(
            boolean daemonCompilesIt) throws IOException {
        // Same rule as above, in the ordering that got past it. The baseline
        // the rule is read against describes what the application started
        // with, and it used to be taken when the compile leg was first needed
        // - the first apply - by which time a source created since the start
        // is on disk and lands in it as though the application had always had
        // it. Every other test in this class applies once in setUp, which is
        // exactly what hides this: by then the baseline is already taken.
        //
        // The file has to be created after the start: a start builds the
        // module, so a source already on disk would be compiled and scanned
        // into the application it is meant to be missing from.
        cli.run("shutdown").assertExitCode(0);
        cli.run("start").assertExitCode(0);
        Path source = MUTABLE.resolve("ExtraBean.java");
        patch.create(source, """
                package com.vaadin.flow.devloop.test.app.mutable;

                import org.springframework.stereotype.Component;

                /** Created by DevLoopRestartIT and deleted again by it. */
                @Component
                public class ExtraBean {
                }
                """);
        if (!daemonCompilesIt) {
            // An IDE building on save, or a plain mvn run, in the same window.
            // The artifact is then newer than the source and the baseline has
            // the stamp, so the change-set did not see the file at all and the
            // apply answered "no changes" - a worse answer than the wrong
            // verdict, and the same root cause.
            compileOutsideTheDaemon(source);
        }

        VaadinDevCli.Outcome outcome = cli
                .run("apply", "--no-restart", "--json").assertExitCode(0);

        outcome.assertOutputContains("new Spring bean (ExtraBean)");
        outcome.assertOutputDoesNotContain("no changes");
    }

    /**
     * Compiles a fixture the way anything other than the daemon would, into the
     * classpath the application is running.
     */
    private static void compileOutsideTheDaemon(Path source)
            throws IOException {
        // The application's own classpath, as the daemon resolved it when it
        // launched: this JVM's may be a manifest-only jar, which would not
        // resolve the annotation.
        String classpath = Files.readString(
                APP.resolve("target").resolve("devloop").resolve("cp.txt"),
                StandardCharsets.UTF_8).trim();
        int status = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-nowarn", "-proc:none", "-classpath", classpath, "-d",
                APP.resolve("target").resolve("classes").toString(),
                source.toString());
        assertEquals(0, status,
                "the fixture has to compile for this test to mean anything");
    }

    /**
     * The fixtures here are new classes, and a class file whose source was
     * created and deleted without a restart in between is deliberately left for
     * the next build - so these take their own artifacts out. Otherwise the
     * next application to start would component-scan a bean this class
     * invented, and the test after it would be measuring that.
     */
    @AfterEach
    void removeFixtureArtifacts() throws IOException {
        Path classes = APP.resolve("target").resolve("classes")
                .resolve("com/vaadin/flow/devloop/test/app/mutable".replace('/',
                        java.io.File.separatorChar));
        if (!Files.isDirectory(classes)) {
            return;
        }
        try (Stream<Path> artifacts = Files.list(classes)) {
            for (Path artifact : artifacts.filter(
                    path -> path.getFileName().toString().startsWith("Extra"))
                    .toList()) {
                Files.deleteIfExists(artifact);
            }
        }
    }

    @Test
    void restart_bringsTheAppBackOnTheSamePort() {
        cli.run("restart").assertExitCode(0);

        cli.run("status").assertExitCode(0).assertOutputContains("running")
                .assertOutputContains("registered=true");
    }
}
