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

package com.vaadin.flow.plugin.maven;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.junit.Assert;
import org.junit.Test;

public class FrontendScannerConfigTest {

    @Test
    public void shouldScan_noRules_allArtifactsAccepted() {
        FrontendScannerConfig config = new FrontendScannerConfig();
        Assert.assertTrue(artifacts().allMatch(config::shouldScan));
    }

    @Test
    public void shouldScan_includes_ruleApplied() {
        FrontendScannerConfig config = new FrontendScannerConfig();
        config.addInclude(
                new FrontendScannerConfig.ArtifactMatcher("com.vaadin", null));
        Assert.assertTrue(artifacts().filter(config::shouldScan)
                .allMatch(a -> a.getGroupId().equals("com.vaadin")));

        config.addInclude(new FrontendScannerConfig.ArtifactMatcher(
                "com.fasterxml.jackson*", null));
        Assert.assertTrue(artifacts().filter(config::shouldScan)
                .allMatch(a -> a.getGroupId().equals("com.vaadin")
                        || a.getGroupId().startsWith("com.fasterxml.jackson")));

        config.addInclude(
                new FrontendScannerConfig.ArtifactMatcher(null, "flow-server"));
        Assert.assertTrue(artifacts().filter(config::shouldScan)
                .allMatch(a -> a.getGroupId().equals("com.vaadin")
                        || a.getGroupId().startsWith("com.fasterxml.jackson")
                        || a.getArtifactId().equals("flow-server")));

        config.addInclude(
                new FrontendScannerConfig.ArtifactMatcher("vaadin-*", null));
        Assert.assertTrue(artifacts().filter(config::shouldScan)
                .allMatch(a -> a.getGroupId().equals("com.vaadin")
                        || a.getGroupId().startsWith("com.fasterxml.jackson")
                        || a.getArtifactId().equals("flow-server")
                        || a.getArtifactId().startsWith("vaadin-")));
    }

    @Test
    public void shouldScan_excludes_ruleApplied() {
        FrontendScannerConfig config = new FrontendScannerConfig();
        config.addExclude(
                new FrontendScannerConfig.ArtifactMatcher("com.vaadin", null));
        Assert.assertTrue(artifacts().filter(config::shouldScan)
                .noneMatch(a -> a.getGroupId().equals("com.vaadin")));

        config.addExclude(new FrontendScannerConfig.ArtifactMatcher(
                "com.fasterxml.jackson*", null));
        Assert.assertTrue(artifacts().filter(config::shouldScan)
                .noneMatch(a -> a.getGroupId().equals("com.vaadin")
                        || a.getGroupId().startsWith("com.fasterxml.jackson")));

        config.addExclude(
                new FrontendScannerConfig.ArtifactMatcher(null, "flow-server"));
        Assert.assertTrue(artifacts().filter(config::shouldScan)
                .noneMatch(a -> a.getGroupId().equals("com.vaadin")
                        || a.getGroupId().startsWith("com.fasterxml.jackson")
                        || a.getArtifactId().equals("flow-server")));

        config.addExclude(
                new FrontendScannerConfig.ArtifactMatcher("vaadin-*", null));
        Assert.assertTrue(artifacts().filter(config::shouldScan)
                .noneMatch(a -> a.getGroupId().equals("com.vaadin")
                        || a.getGroupId().startsWith("com.fasterxml.jackson")
                        || a.getArtifactId().equals("flow-server")
                        || a.getArtifactId().startsWith("vaadin-")));
    }

    @Test
    public void shouldScan_excludeAndIncludeRules_exclusionsHaveHigherPriority() {
        FrontendScannerConfig config = new FrontendScannerConfig();
        config.addExclude(new FrontendScannerConfig.ArtifactMatcher("*", "*"));
        config.addInclude(new FrontendScannerConfig.ArtifactMatcher("*", "*"));
        Assert.assertTrue(artifacts().noneMatch(config::shouldScan));

        config = new FrontendScannerConfig();
        config.addExclude(new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin", "vaadin-*"));
        config.addInclude(
                new FrontendScannerConfig.ArtifactMatcher("com.vaadin", "*"));

        List<Artifact> artifacts = artifacts().filter(config::shouldScan)
                .toList();
        Assert.assertFalse(artifacts.isEmpty());
        Assert.assertTrue(artifacts.stream()
                .allMatch(a -> a.getGroupId().equals("com.vaadin")
                        && !a.getArtifactId().startsWith("vaadin-")));

    }

    @Test
    public void shouldScan_disabled_alwaysTrue() {
        FrontendScannerConfig config = new FrontendScannerConfig();
        config.addExclude(new FrontendScannerConfig.ArtifactMatcher("*", "*"));
        config.addInclude(new FrontendScannerConfig.ArtifactMatcher("*", "*"));
        config.setEnabled(false);
        Assert.assertTrue(artifacts().allMatch(config::shouldScan));
    }

    private Stream<Artifact> artifacts() {
        return Arrays.stream(TEST_DEPENDENCIES_LIST.split("\n"))
                .map(ArtifactMatcherTest::fromString);
    }

    //
    // List of dependency dumped from a Maven project with
    // mvn -B -q dependency:list -DoutputFile=deps.txt
    // and then processed with
    // grep -E "^\\s+\\w+" deps.txt | sed -E 's/\s+(\w.*) -- .*/\1/g'
    // Possible formats are:
    // <groupId>:<artifactId>:<type>:<version>:<scope> (optional)
    // <groupId>:<artifactId>:<type>:<classifier>:<version>:<scope> (optional)
    //
    private static final String TEST_DEPENDENCIES_LIST = """
            com.vaadin:vaadin:jar:24.8-SNAPSHOT:compile
            com.vaadin:vaadin-internal:jar:24.8-SNAPSHOT:compile
            com.vaadin:vaadin-core-internal:jar:24.8-SNAPSHOT:compile
            com.vaadin:vaadin-accordion-flow:jar:24.8-SNAPSHOT:compile
            com.vaadin:vaadin-avatar-flow:jar:24.8-SNAPSHOT:compile
            com.vaadin:vaadin-checkbox-flow:jar:24.8-SNAPSHOT:compile
            com.vaadin:flow-server:jar:24.8-SNAPSHOT:compile
            com.vaadin:flow-push:jar:24.8-SNAPSHOT:compile
            com.vaadin.external.atmosphere:atmosphere-runtime:jar:3.0.5.slf4jvaadin1:compile
            com.vaadin.servletdetector:throw-if-servlet3:jar:1.0.2:compile
            org.jspecify:jspecify:jar:1.0.0:compile
            org.slf4j:slf4j-api:jar:2.0.17:compile
            com.vaadin.external.gwt:gwt-elemental:jar:2.8.2.vaadin2:compile
            org.apache.commons:commons-fileupload2-jakarta:jar:2.0.0-M1:compile
            org.apache.commons:commons-fileupload2-core:jar:2.0.0-M1:compile
            commons-io:commons-io:jar:2.18.0:compile
            com.fasterxml.jackson.core:jackson-core:jar:2.18.3:compile
            com.fasterxml.jackson.core:jackson-databind:jar:2.18.3:compile
            com.fasterxml.jackson.core:jackson-annotations:jar:2.18.3:compile
            com.fasterxml.jackson.datatype:jackson-datatype-jsr310:jar:2.18.3:compile
            org.jsoup:jsoup:jar:1.19.1:compile
            com.helger:ph-css:jar:7.0.4:compile
            com.helger.commons:ph-commons:jar:11.2.0:compile
            com.google.code.findbugs:jsr305:jar:3.0.2:compile
            org.ow2.asm:asm:jar:9.7.1:compile
            com.vaadin.external:gentyref:jar:1.2.0.vaadin1:compile
            org.apache.commons:commons-compress:jar:1.27.1:compile
            commons-codec:commons-codec:jar:1.17.1:compile
            org.apache.commons:commons-lang3:jar:3.16.0:compile
            com.vaadin:flow-client:jar:24.8-SNAPSHOT:compile
            org.yaml:snakeyaml:jar:2.2:compile
            org.yaml:snakeyaml:jar:android:1.23:compile
            org.springframework.boot:spring-boot-autoconfigure:jar:3.4.3:compile
            org.springframework.boot:spring-boot-devtools:jar:3.4.3:compile (optional)
            org.springframework.boot:spring-boot:jar:3.4.3:compile
            org.springframework.boot:spring-boot-starter:jar:3.4.3:compile
            org.springframework.boot:spring-boot-starter-json:jar:3.4.3:compile
            org.springframework.boot:spring-boot-starter-logging:jar:3.4.3:compile
            org.springframework.boot:spring-boot-starter-tomcat:jar:3.4.3:compile
            org.springframework.boot:spring-boot-starter-validation:jar:3.4.3:compile
            org.springframework.boot:spring-boot-starter-web:jar:3.4.3:compile
            org.springframework.boot:spring-boot-test-autoconfigure:jar:3.4.3:test
            org.springframework.boot:spring-boot-test:jar:3.4.3:test
            org.springframework.data:spring-data-commons:jar:3.4.3:compile
            org.springframework.security:spring-security-core:jar:6.4.3:compile
            org.springframework.security:spring-security-crypto:jar:6.4.3:compile
            org.springframework:spring-aop:jar:6.2.3:compile
            org.springframework:spring-beans:jar:6.2.3:compile
            org.springframework:spring-context:jar:6.2.3:compile
            org.springframework:spring-core:jar:6.2.3:compile
            org.springframework:spring-expression:jar:6.2.3:compile
            org.springframework:spring-jcl:jar:6.2.3:compile
            org.springframework:spring-test:jar:6.2.3:test
            org.springframework:spring-web:jar:6.2.3:compile
            org.springframework:spring-webmvc:jar:6.2.3:compile
            org.springframework:spring-websocket:jar:6.2.3:compile
            com.jayway.jsonpath:json-path:jar:2.9.0:test
            org.slf4j:slf4j-api:jar:2.0.16:compile
            jakarta.xml.bind:jakarta.xml.bind-api:jar:4.0.2:compile
            jakarta.activation:jakarta.activation-api:jar:2.1.3:compile
            net.minidev:json-smart:jar:2.5.2:test
            """;
}