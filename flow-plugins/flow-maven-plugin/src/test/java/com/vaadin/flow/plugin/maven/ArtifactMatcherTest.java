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
package com.vaadin.flow.plugin.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactMatcherTest {

    @Test
    void setGroupId_wildcard_throwIfInInvalidPosition() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher();
        matcher.setGroupId(null);
        matcher.setGroupId("");
        matcher.setGroupId("*");
        matcher.setGroupId("com.vaadin");
        matcher.setGroupId("*.vaadin");
        matcher.setGroupId("com.vaadin.*");
        matcher.setGroupId("*.vaadin.*");
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("com.*.vaadin"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("c*.vaadi*n"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("*.vaa*din.*"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("*.vaa*din"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("**.vaadin"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("com.vaa*din*"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("com.vaadi**"));
    }

    @Test
    void setArtifactId_wildcard_throwIfInInvalidPosition() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher();
        matcher.setArtifactId(null);
        matcher.setArtifactId("");
        matcher.setArtifactId("*");
        matcher.setArtifactId("vaadin");
        matcher.setArtifactId("*vaadin");
        matcher.setArtifactId("vaadin*");
        matcher.setArtifactId("*vaadin*");
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("va*din"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("v*di*n"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("v*d*i*n"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("*di*n"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("**din"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("vadi**"));
        assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("vadi*n*"));
    }

    @Test
    void matches_matchEverything_returnsTrue() {
        Artifact artifact = fromString(
                "com.vaadin:vaadin:jar:999.99-SNAPSHOT:compile");
        assertTrue(
                new FrontendScannerConfig.ArtifactMatcher().matches(artifact),
                "Unspecified groups and artifacts");
        assertTrue(new FrontendScannerConfig.ArtifactMatcher("", "")
                .matches(artifact), "Empty groups and artifacts");
        assertTrue(new FrontendScannerConfig.ArtifactMatcher("*", "*")
                        .matches(artifact), "All groups and artifacts");
        assertTrue(
                        new FrontendScannerConfig.ArtifactMatcher("*", null)
                                .matches(artifact),
                        "All groups, unspecified artifacts");
        assertTrue(
                        new FrontendScannerConfig.ArtifactMatcher(null, "*")
                                .matches(artifact),
                        "Unspecified groups, all artifacts");
        assertTrue(new FrontendScannerConfig.ArtifactMatcher("", "*")
                .matches(artifact), "Blank groups, all artifacts");
        assertTrue(
                        new FrontendScannerConfig.ArtifactMatcher("*", null)
                                .matches(artifact),
                        "All groups, unspecified artifacts");
        assertTrue(
                        new FrontendScannerConfig.ArtifactMatcher(null, "*")
                                .matches(artifact),
                        "Unspecified groups, all artifacts");
        assertTrue(
                        new FrontendScannerConfig.ArtifactMatcher(null, "")
                                .matches(artifact),
                        "Unspecified groups, blank artifacts");
    }

    @Test
    void matches_exactGroup() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin", null);
        assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin:flow-server:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin.demo:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "org.com.vaadin.demo:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadindemo:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(
                fromString("org.example:vaadin:jar:999.99-SNAPSHOT:compile")));
    }

    @Test
    void matches_wildcardGroup() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin.*", null);
        assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(
                fromString("com.vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));

        matcher.setGroupId("*.vaadin");
        assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.example.vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(
                fromString(".vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(
                fromString("vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadindemo:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:999.99-SNAPSHOT:compile")));

        matcher.setGroupId("*vaadin*");
        assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.example.vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(
                fromString(".vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(
                fromString("vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadindemo:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(
                fromString("com.example:vaadin:jar:999.99-SNAPSHOT:compile")));
    }

    @Test
    void matches_wildcardArtifact() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher(
                null, "vaadin*");
        assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:vaadin-demo:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:vaadindemo:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin:demovaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(
                fromString("com.vaadin:example:jar:999.99-SNAPSHOT:compile")));

        matcher.setArtifactId("*vaadin");
        assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:demo-vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:demovaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin:vaadindemo:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin:vaadin-demo:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(
                fromString("com.vaadin:example:jar:999.99-SNAPSHOT:compile")));

        matcher.setArtifactId("*vaadin*");
        assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:vaadin-demo:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:vaadindemo:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:demo-vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:demovaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(
                fromString("com.vaadin:example:jar:999.99-SNAPSHOT:compile")));
    }

    @Test
    void matches_exactArtifact() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher(
                null, "vaadin");
        assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertTrue(matcher.matches(
                fromString("org.example:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin:vaadindemo:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin:demovaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin:demovaadindemo:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(
                fromString("com.vaadin:example:jar:999.99-SNAPSHOT:compile")));

    }

    @Test
    void matches_exactGroupAndArtifact() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin", "vaadin");
        assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin:flow-server:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin.demo:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "org.com.vaadin.demo:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadindemo:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(
                fromString("org.example:vaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin:vaadindemo:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin:demovaadin:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(fromString(
                "com.vaadin:demovaadindemo:jar:999.99-SNAPSHOT:compile")));
        assertFalse(matcher.matches(
                fromString("com.vaadin:example:jar:999.99-SNAPSHOT:compile")));
    }

    @Test
    void matches_nullArtifact_returnsFalse() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher();
        assertFalse(matcher.matches(null));
        matcher.setGroupId("com.vaadin");
        assertFalse(matcher.matches(null));
        matcher.setArtifactId("vaadin");
        assertFalse(matcher.matches(null));
    }

    /**
     * Creates an Artifact instance based on the give coordinates.
     * <p>
     *
     *
     * Allowed syntaxes:
     *
     * <pre>
     * {@code
     * <groupId>:<artifactId>:<type>:<version>:<scope>[(optional)]
     * <groupId>:<artifactId>:<type>:<classifier>:<version>:<scope>[ (optional)]
     * }
     * </pre>
     *
     * @param coordinates
     *            artifact coordinates.
     * @return an Artifact instance based on the give coordinates.
     */
    static Artifact fromString(String coordinates) {
        String[] tokens = coordinates.split(":");
        boolean hasClassifier = tokens.length > 5;
        String groupId = tokens[0];
        String artifactId = tokens[1];
        String type = tokens[2];
        String classifier = hasClassifier ? tokens[5] : null;
        String version = hasClassifier ? tokens[4] : tokens[3];
        String scope = hasClassifier ? tokens[5] : tokens[4];
        boolean optional = scope.contains(" (optional)");
        if (optional) {
            scope = scope.replace(" (optional)", "");
        }
        DefaultArtifactHandler handler = new DefaultArtifactHandler(type);
        handler.setAddedToClasspath(true);
        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId,
                version, scope, type, classifier, handler);
        artifact.setOptional(optional);
        return artifact;
    }

}
