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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    void restart_bringsTheAppBackOnTheSamePort() {
        cli.run("restart").assertExitCode(0);

        cli.run("status").assertExitCode(0).assertOutputContains("running")
                .assertOutputContains("registered=true");
    }
}
