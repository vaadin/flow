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
package com.vaadin.quarkus.deployment;

import java.util.List;
import java.util.Map;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeprecatedJandexArtifactsWarningTest {

    private final Logger logger = Mockito.mock(Logger.class);
    private MockedStatic<LoggerFactory> loggerFactory;

    @BeforeEach
    void mockLogger() {
        loggerFactory = Mockito.mockStatic(LoggerFactory.class);
        loggerFactory.when(
                () -> LoggerFactory.getLogger(VaadinQuarkusProcessor.class))
                .thenReturn(logger);
    }

    @AfterEach
    void closeLoggerMock() {
        loggerFactory.close();
    }

    @Test
    void noReferences_noWarning() {
        VaadinQuarkusProcessor.warnAboutDeprecatedJandexArtifacts(List.of(),
                Map.of("quarkus.index-dependency.lumo.artifact-id",
                        "vaadin-lumo-theme"));

        Mockito.verifyNoInteractions(logger);
    }

    @Test
    void transitiveDependency_noWarning() {
        VaadinQuarkusProcessor.warnAboutDeprecatedJandexArtifacts(
                List.of(dependency("vaadin-core-jandex", false)), Map.of());

        Mockito.verifyNoInteractions(logger);
    }

    @Test
    void directDependencies_warningNamesBoth() {
        VaadinQuarkusProcessor
                .warnAboutDeprecatedJandexArtifacts(
                        List.of(dependency("vaadin-jandex", true),
                                dependency("vaadin-core-jandex", true)),
                        Map.of());

        assertEquals(
                "the com.vaadin:vaadin-core-jandex and "
                        + "com.vaadin:vaadin-jandex dependencies",
                loggedReferences());
    }

    @Test
    void allReferences_warningGroupsDependenciesAndProperties() {
        VaadinQuarkusProcessor.warnAboutDeprecatedJandexArtifacts(
                List.of(dependency("vaadin-jandex", true),
                        dependency("vaadin-core-jandex", true)),
                Map.of("quarkus.index-dependency.vaadin-jandex.artifact-id",
                        "vaadin-jandex",
                        "quarkus.index-dependency.vaadin-core-jandex.artifact-id",
                        "vaadin-core-jandex"));

        assertEquals("the com.vaadin:vaadin-core-jandex and "
                + "com.vaadin:vaadin-jandex dependencies and the "
                + "quarkus.index-dependency.vaadin-core-jandex and "
                + "quarkus.index-dependency.vaadin-jandex configuration "
                + "properties", loggedReferences());
    }

    @Test
    void dependencyAndIndexDependencyConfig_warningNamesBoth() {
        VaadinQuarkusProcessor.warnAboutDeprecatedJandexArtifacts(
                List.of(dependency("vaadin-jandex", true)),
                Map.of("quarkus.index-dependency.vaadin.group-id", "com.vaadin",
                        "quarkus.index-dependency.vaadin.artifact-id",
                        "vaadin-jandex"));

        assertEquals("the com.vaadin:vaadin-jandex dependency and "
                + "the quarkus.index-dependency.vaadin configuration property",
                loggedReferences());
    }

    @Test
    void indexDependencyConfig_warningNamesProperty() {
        VaadinQuarkusProcessor.warnAboutDeprecatedJandexArtifacts(List.of(),
                Map.of("quarkus.index-dependency.vaadin.group-id", "com.vaadin",
                        "quarkus.index-dependency.vaadin.artifact-id",
                        "vaadin-jandex"));

        assertEquals(
                "the quarkus.index-dependency.vaadin configuration property",
                loggedReferences());
    }

    private String loggedReferences() {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> references = ArgumentCaptor
                .forClass(Object.class);
        Mockito.verify(logger).warn(message.capture(), references.capture());
        assertTrue(message.getValue().contains("You can safely remove {}."),
                message.getValue());
        return (String) references.getValue();
    }

    private static Dependency dependency(String artifactId, boolean direct) {
        return ResolvedDependencyBuilder.newInstance().setGroupId("com.vaadin")
                .setArtifactId(artifactId).setVersion("25.4.0")
                .setDirect(direct).build();
    }
}
