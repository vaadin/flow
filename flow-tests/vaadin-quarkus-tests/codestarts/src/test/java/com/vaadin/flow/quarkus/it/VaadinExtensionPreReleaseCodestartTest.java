/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it;

import java.util.Map;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.vaadin.flow.quarkus.it.CodestartTestUtils.assertThatHasPreReleaseRepositories;
import static com.vaadin.flow.quarkus.it.CodestartTestUtils.assertThatHasVaadinBom;
import static com.vaadin.flow.quarkus.it.CodestartTestUtils.assertThatHasVaadinQuarkusExtension;
import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class VaadinExtensionPreReleaseCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest
            .builder().languages(Language.JAVA)
            .setupStandaloneExtensionTest("com.vaadin:vaadin-quarkus")
            .putData("vaadin-flow-codestart.vaadinVersion", "24.1-SNAPSHOT")
            .build();

    @Test
    void testApplicationContents() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.example.MainView");
        codestartTest.checkGeneratedSource("org.acme.example.AppConfig");
        codestartTest.checkGeneratedSource("org.acme.example.GreetService");

        codestartTest.assertThatGeneratedTreeMatchSnapshots(Language.JAVA,
                "src/main/resources");

        codestartTest.assertThatGeneratedFile(Language.JAVA, ".gitignore")
                .satisfies(checkContains("node_modules/"),
                        checkContains("src/main/frontend/generated/"),
                        checkContains("vite.generated.ts"));

        // Check POM file: vaadin-bom, deps, production profile
        codestartTest.assertThatGeneratedFile(Language.JAVA, "pom.xml")
                .satisfies(pomFile -> {
                    Model pom = new DefaultModelReader().read(pomFile.toFile(),
                            Map.of());
                    assertSoftly(soft -> {
                        assertThatHasVaadinBom(pom, soft);
                        assertThatHasVaadinQuarkusExtension(pom, soft);
                        assertThatHasPreReleaseRepositories(pom, soft);
                    });
                });
    }

}
