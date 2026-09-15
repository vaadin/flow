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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reactor discovery decides where the edit loop is even looked for, so getting
 * the root wrong makes a sibling module's edit invisible.
 */
class ReactorTest {

    @TempDir
    private Path repo;

    private final List<String> logged = new java.util.ArrayList<>();

    private final Launch.Log log = logged::add;

    @Test
    void noAggregator_isAReactorOfOne() throws IOException {
        Path app = module("app", "app", "jar");

        Reactor reactor = Reactor.discover(app, log);

        assertFalse(reactor.isMultiModule());
        assertEquals(List.of("app"), names(reactor.candidates()));
        assertEquals(1, reactor.poms().size());
    }

    @Test
    void aggregatorAbove_becomesTheRoot() throws IOException {
        aggregator(repo, "root", List.of("app", "shared"));
        Path app = module("app", "app", "jar");
        module("shared", "shared", "jar");

        Reactor reactor = Reactor.discover(app, log);

        assertTrue(reactor.isMultiModule());
        assertEquals(Reactor.real(repo), reactor.root());
        assertEquals("app", reactor.app().name());
        // The application is always first; a sibling with sources joins it.
        assertEquals(List.of("app", "shared"), names(reactor.candidates()));
    }

    @Test
    void highestVerifyingAncestorWins() throws IOException {
        // root -> group -> app: -pl/-am has to run against the top of the
        // chain.
        aggregator(repo, "root", List.of("group"));
        Path group = repo.resolve("group");
        aggregator(group, "group", List.of("app"));
        Path app = module("group/app", "app", "jar");

        Reactor reactor = Reactor.discover(app, log);

        assertEquals(Reactor.real(repo), reactor.root());
    }

    @Test
    void ancestorThatDoesNotAggregateTheApp_isIgnored() throws IOException {
        // Aggregation and inheritance are independent in Maven: a pom above the
        // application that does not list it is not this application's reactor.
        aggregator(repo, "root", List.of("other"));
        module("other", "other", "jar");
        Path app = module("app", "app", "jar");

        Reactor reactor = Reactor.discover(app, log);

        assertFalse(reactor.isMultiModule());
    }

    @Test
    void moduleWithoutSources_isNotACandidate() throws IOException {
        aggregator(repo, "root", List.of("app", "empty"));
        Path app = module("app", "app", "jar");
        Files.createDirectories(repo.resolve("empty"));
        Files.writeString(repo.resolve("empty").resolve("pom.xml"),
                pom("empty", "jar", List.of()));

        Reactor reactor = Reactor.discover(app, log);

        assertEquals(List.of("app"), names(reactor.candidates()));
    }

    @Test
    void aggregatorPackaging_isNotACompileDomain() throws IOException {
        aggregator(repo, "root", List.of("libs"));
        Path libs = repo.resolve("libs");
        aggregator(libs, "libs", List.of("app"));
        // The intermediate aggregator has sources lying around but packaging
        // pom, so it compiles nothing and must not be in the loop.
        Files.createDirectories(
                libs.resolve("src").resolve("main").resolve("java"));
        Path app = module("libs/app", "app", "jar");

        Reactor reactor = Reactor.discover(app, log);

        assertEquals(List.of("app"), names(reactor.candidates()));
    }

    @Test
    void interpolatedModulePath_isResolvedFromProperties() throws IOException {
        Files.writeString(repo.resolve("pom.xml"), """
                <project>
                  <artifactId>root</artifactId>
                  <packaging>pom</packaging>
                  <properties>
                    <variant>app</variant>
                  </properties>
                  <modules>
                    <module>${variant}</module>
                  </modules>
                </project>
                """);
        Path app = module("app", "app", "jar");

        Reactor reactor = Reactor.discover(app, log);

        assertTrue(reactor.isMultiModule());
        assertEquals("app", reactor.app().name());
    }

    @Test
    void unresolvedModuleProperty_isSkippedAndReported() throws IOException {
        Files.writeString(repo.resolve("pom.xml"), """
                <project>
                  <artifactId>root</artifactId>
                  <packaging>pom</packaging>
                  <modules>
                    <module>app</module>
                    <module>${unknown}</module>
                  </modules>
                </project>
                """);
        Path app = module("app", "app", "jar");

        Reactor reactor = Reactor.discover(app, log);

        assertTrue(reactor.isMultiModule());
        assertTrue(
                logged.stream()
                        .anyMatch(line -> line.contains("unresolved property")),
                "expected the skipped module to be reported, got " + logged);
    }

    @Test
    void module_ownsOnlyItsOwnSourceAndResourceTrees() throws IOException {
        Path app = module("app", "app", "jar");
        Reactor.Module module = Reactor.Module.of(Reactor.real(app), "app");

        assertTrue(module.owns(module.sourceDir().resolve("Foo.java")));
        assertTrue(module.owns(module.resourceDir().resolve("a.css")));
        // A path in a sibling must answer false rather than throw, which is
        // what relativize() would do - across Windows drive letters as well.
        assertFalse(module.owns(repo.resolve("shared").resolve("Bar.java")));
    }

    @Test
    void module_mapsSourcesToArtifactsAndBackToBinaryNames()
            throws IOException {
        Path app = module("app", "app", "jar");
        Reactor.Module module = Reactor.Module.of(Reactor.real(app), "app");
        Path source = module.sourceDir().resolve("com").resolve("example")
                .resolve("Foo.java");

        Path artifact = module.artifactFor(source);

        assertEquals(module.classesDir().resolve("com").resolve("example")
                .resolve("Foo.class"), artifact);
        assertEquals("com.example.Foo", module.binaryNameOf(artifact));
    }

    @Test
    void describe_countsTheModulesItDoesNotName() throws IOException {
        // One log line, whatever the reactor's size: Flow's own repository
        // aggregates 149 modules, and printing all of them buries the count.
        List<String> modules = new java.util.ArrayList<>(List.of("app"));
        for (int i = 0; i < 12; i++) {
            modules.add("lib" + i);
        }
        aggregator(repo, "root", modules);
        Path app = module("app", "app", "jar");
        for (String name : modules.subList(1, modules.size())) {
            module(name, name, "jar");
        }

        String describe = Reactor.discover(app, log).describe();

        assertTrue(describe.startsWith("13 module(s): app,"), describe);
        assertTrue(describe.endsWith(" and 5 more"), describe);
    }

    @Test
    void requiredRelease_readsTheCompilerProperty() throws IOException {
        Path app = module("app", "app", "jar");
        write(app, """
                <project>
                  <artifactId>app</artifactId>
                  <properties>
                    <maven.compiler.release>21</maven.compiler.release>
                  </properties>
                </project>
                """);

        assertEquals(21,
                Reactor.discover(app, log).requiredRelease().getAsInt());
    }

    @Test
    void requiredRelease_interpolatesTheSpringBootSpelling()
            throws IOException {
        Path app = module("app", "app", "jar");
        write(app,
                """
                        <project>
                          <artifactId>app</artifactId>
                          <properties>
                            <java.version>21</java.version>
                            <maven.compiler.release>${java.version}</maven.compiler.release>
                          </properties>
                        </project>
                        """);

        assertEquals(21,
                Reactor.discover(app, log).requiredRelease().getAsInt());
    }

    @Test
    void requiredRelease_fallsBackToTheReactorRoot() throws IOException {
        aggregator(repo, "root", List.of("app"));
        write(repo, """
                <project>
                  <artifactId>root</artifactId>
                  <packaging>pom</packaging>
                  <modules><module>app</module></modules>
                  <properties><java.version>25</java.version></properties>
                </project>
                """);
        Path app = module("app", "app", "jar");

        // The level is declared once at the top, which is where a multi-module
        // project puts it.
        assertEquals(25,
                Reactor.discover(app, log).requiredRelease().getAsInt());
    }

    @Test
    void requiredRelease_prefersTheCompilerPluginOverTheProperties()
            throws IOException {
        Path app = module("app", "app", "jar");
        write(app, """
                <project>
                  <artifactId>app</artifactId>
                  <properties><java.version>17</java.version></properties>
                  <build><plugins><plugin>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration><release>21</release></configuration>
                  </plugin></plugins></build>
                </project>
                """);

        // Maven acts on the plugin's own configuration, so the daemon must too.
        assertEquals(21,
                Reactor.discover(app, log).requiredRelease().getAsInt());
    }

    @Test
    void requiredRelease_isEmptyWhenNoPomDeclaresOne() throws IOException {
        Path app = module("app", "app", "jar");

        // Inherited from a parent outside the checkout; answered from the
        // bytecode instead.
        assertTrue(Reactor.discover(app, log).requiredRelease().isEmpty());
    }

    private void write(Path moduleDir, String pom) throws IOException {
        Files.writeString(moduleDir.resolve("pom.xml"), pom);
    }

    private List<String> names(List<Reactor.Module> modules) {
        return modules.stream().map(Reactor.Module::name).toList();
    }

    private void aggregator(Path dir, String artifactId, List<String> modules)
            throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("pom.xml"),
                pom(artifactId, "pom", modules));
    }

    private Path module(String relative, String artifactId, String packaging)
            throws IOException {
        Path dir = repo.resolve(relative);
        Files.createDirectories(
                dir.resolve("src").resolve("main").resolve("java"));
        Files.writeString(dir.resolve("pom.xml"),
                pom(artifactId, packaging, List.of()));
        return dir;
    }

    private String pom(String artifactId, String packaging,
            List<String> modules) {
        StringBuilder sb = new StringBuilder("<project>\n  <artifactId>")
                .append(artifactId).append("</artifactId>\n  <packaging>")
                .append(packaging).append("</packaging>\n");
        if (!modules.isEmpty()) {
            sb.append("  <modules>\n");
            modules.forEach(module -> sb.append("    <module>").append(module)
                    .append("</module>\n"));
            sb.append("  </modules>\n");
        }
        return sb.append("</project>\n").toString();
    }
}
