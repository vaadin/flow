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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Choosing the app JVM by name, as the daemon once did, gets both halves wrong:
 * a JBR too old for the project cannot run the application at all, and
 * {@code jbr-9} sorts above {@code jbr-21}. Every case here is one of those two
 * mistakes.
 */
class JvmTest {

    @TempDir
    private Path jdks;

    private final List<String> logged = new ArrayList<>();

    @Test
    void featureOf_readsEveryShapeAVersionComesIn() {
        assertEquals(8, Jvm.featureOf("1.8.0_442").getAsInt());
        assertEquals(21, Jvm.featureOf("21.0.5").getAsInt());
        assertEquals(25, Jvm.featureOf("25").getAsInt());
        assertEquals(17, Jvm.featureOf("17-ea").getAsInt());
        // A directory name is the fallback for a home with no release file.
        assertEquals(21, Jvm.featureOf("jbr-21.0.5").getAsInt());
    }

    @Test
    void aJbrIsIdentifiedByItsImplementorRatherThanItsPath()
            throws IOException {
        Path jbr = jdk("some-runtime", "21.0.5", "JetBrains s.r.o.");
        Path plain = jdk("jbr-lookalike", "21.0.5", "Eclipse Adoptium");

        assertTrue(Jvm.describe(jbr).jbr());
        assertFalse(Jvm.describe(plain).jbr());
    }

    @Test
    void belowTheFloorIsNeverACandidate() throws IOException {
        jdk("jbr-17", "17.0.13", "JetBrains s.r.o.");
        jdk("corretto-8", "1.8.0_442", "Amazon.com Inc.");
        Path usable = jdk("temurin-21", "21.0.10", "Eclipse Adoptium");

        List<Jvm.Jdk> candidates = Jvm.candidates(homes());

        // A JBR that cannot load a class Flow compiled is worse than no JBR.
        assertEquals(List.of(usable),
                candidates.stream().map(Jvm.Jdk::home).toList());
    }

    @Test
    void aJbrAboveTheRequirementBeatsAnExactlyMatchingPlainJdk()
            throws IOException {
        Path jbr = jdk("jbr-25", "25.0.2", "JetBrains s.r.o.");
        jdk("temurin-21", "21.0.10", "Eclipse Adoptium");

        assertEquals(jbr, chosen(21).home());
    }

    @Test
    void aRequirementBelowTheFloorIsRaisedToIt() throws IOException {
        jdk("corretto-17", "17.0.13", "Amazon.com Inc.");
        Path jbr = jdk("jbr-25", "25.0.2", "JetBrains s.r.o.");

        // Flow needs 21 whatever a 17-target project's poms say.
        assertEquals(jbr, chosen(17).home());
    }

    @Test
    void aJbrBelowTheRequirementLosesToAJdkThatCanRunIt() throws IOException {
        jdk("jbr-21", "21.0.5", "JetBrains s.r.o.");
        Path temurin = jdk("temurin-25", "25.0.2", "Eclipse Adoptium");

        Jvm.Jdk pick = chosen(25);

        assertEquals(temurin, pick.home());
        assertFalse(pick.jbr());
    }

    @Test
    void withNothingDeclared_theNewestJbrWinsByVersionRatherThanByName()
            throws IOException {
        Path newest = jdk("jbr-21.0.10", "21.0.10", "JetBrains s.r.o.");
        jdk("jbr-21.0.6", "21.0.6", "JetBrains s.r.o.");

        // Sorted by name, 21.0.6 would come out above 21.0.10.
        assertEquals(newest, chosen(0).home());
    }

    @Test
    void aJdkInstalledAsAMacOsBundleIsFoundThroughItsHome() throws IOException {
        Path home = jdk("jbrsdk-21.0.11.jdk/Contents/Home", "21.0.11",
                "JetBrains s.r.o.");

        List<Jvm.Jdk> candidates = Jvm.candidates(homes());

        // homes() lists the bundle root, which has neither the launcher nor the
        // release file - so the JBR a JetBrains IDE downloaded is only a
        // candidate if the home inside it is resolved.
        assertEquals(List.of(home),
                candidates.stream().map(Jvm.Jdk::home).toList());
        assertTrue(candidates.get(0).jbr());
    }

    /**
     * The chosen JVM for a requirement, or for none when {@code required} is 0,
     * over the temporary JDKs alone.
     */
    private Jvm.Jdk chosen(int required) throws IOException {
        Optional<Jvm.Requirement> requirement = required == 0 ? Optional.empty()
                : Optional.of(new Jvm.Requirement(required, "a test"));
        return Jvm.select(requirement, Jvm.candidates(homes()), logged::add);
    }

    private List<Path> homes() throws IOException {
        try (var stream = Files.list(jdks)) {
            return stream.toList();
        }
    }

    /**
     * A JDK home as an installer leaves it: a launcher and a release file.
     * <p>
     * Returned canonical, because that is the spelling {@code candidates()}
     * reports: on macOS the temporary directory is reached through a symlink,
     * so comparing against the path as built here would compare
     * {@code /var/folders/...} with {@code /private/var/folders/...}.
     */
    private Path jdk(String name, String version, String implementor)
            throws IOException {
        Path home = jdks.resolve(name);
        Files.createDirectories(home.resolve("bin"));
        Files.writeString(home.resolve("bin").resolve("java"), "");
        Files.writeString(home.resolve("bin").resolve("java.exe"), "");
        Files.writeString(home.resolve("release"), "JAVA_VERSION=\"" + version
                + "\"\nIMPLEMENTOR=\"" + implementor + "\"\n");
        return Reactor.real(home);
    }
}
