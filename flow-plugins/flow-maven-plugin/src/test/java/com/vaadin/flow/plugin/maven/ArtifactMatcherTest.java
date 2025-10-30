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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactMatcherTest {

    @Test
    public void setGroupId_wildcard_throwIfInInvalidPosition() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher();
        matcher.setGroupId(null);
        matcher.setGroupId("");
        matcher.setGroupId("*");
        matcher.setGroupId("com.vaadin");
        matcher.setGroupId("*.vaadin");
        matcher.setGroupId("com.vaadin.*");
        matcher.setGroupId("*.vaadin.*");
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("com.*.vaadin"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("c*.vaadi*n"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("*.vaa*din.*"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("*.vaa*din"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("**.vaadin"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("com.vaa*din*"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("com.vaadi**"));
    }

    @Test
    public void setArtifactId_wildcard_throwIfInInvalidPosition() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher();
        matcher.setArtifactId(null);
        matcher.setArtifactId("");
        matcher.setArtifactId("*");
        matcher.setArtifactId("vaadin");
        matcher.setArtifactId("*vaadin");
        matcher.setArtifactId("vaadin*");
        matcher.setArtifactId("*vaadin*");
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("va*din"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("v*di*n"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("v*d*i*n"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("*di*n"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("**din"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("vadi**"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> matcher.setGroupId("vadi*n*"));
    }

    @Test
    public void matches_matchEverything_returnsTrue() {
        Artifact artifact = fromString(
                "com.vaadin:vaadin:jar:25.0-SNAPSHOT:compile");
        Assert.assertTrue("Unspecified groups and artifacts",
                new FrontendScannerConfig.ArtifactMatcher().matches(artifact));
        Assert.assertTrue("Empty groups and artifacts",
                new FrontendScannerConfig.ArtifactMatcher("", "")
                        .matches(artifact));
        Assert.assertTrue("All groups and artifacts",
                new FrontendScannerConfig.ArtifactMatcher("*", "*")
                        .matches(artifact));
        Assert.assertTrue("All groups, unspecified artifacts",
                new FrontendScannerConfig.ArtifactMatcher("*", null)
                        .matches(artifact));
        Assert.assertTrue("Unspecified groups, all artifacts",
                new FrontendScannerConfig.ArtifactMatcher(null, "*")
                        .matches(artifact));
        Assert.assertTrue("Blank groups, all artifacts",
                new FrontendScannerConfig.ArtifactMatcher("", "*")
                        .matches(artifact));
        Assert.assertTrue("All groups, unspecified artifacts",
                new FrontendScannerConfig.ArtifactMatcher("*", null)
                        .matches(artifact));
        Assert.assertTrue("Unspecified groups, all artifacts",
                new FrontendScannerConfig.ArtifactMatcher(null, "*")
                        .matches(artifact));
        Assert.assertTrue("Unspecified groups, blank artifacts",
                new FrontendScannerConfig.ArtifactMatcher(null, "")
                        .matches(artifact));
    }

    @Test
    public void matches_exactGroup() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin", null);
        Assert.assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin:flow-server:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(fromString(
                "com.vaadin.demo:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(fromString(
                "org.com.vaadin.demo:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadindemo:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("org.example:vaadin:jar:25.0-SNAPSHOT:compile")));
    }

    @Test
    public void matches_wildcardGroup() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin.*", null);
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));

        matcher.setGroupId("*.vaadin");
        Assert.assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.example.vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(
                fromString(".vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadindemo:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:25.0-SNAPSHOT:compile")));

        matcher.setGroupId("*vaadin*");
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.example.vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(
                fromString(".vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(
                fromString("vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(
                fromString("com.vaadindemo:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.example:vaadin:jar:25.0-SNAPSHOT:compile")));
    }

    @Test
    public void matches_wildcardArtifact() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher(
                null, "vaadin*");
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:vaadin-demo:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:vaadindemo:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:demovaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:example:jar:25.0-SNAPSHOT:compile")));

        matcher.setArtifactId("*vaadin");
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:demo-vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:demovaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:vaadindemo:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(fromString(
                "com.vaadin:vaadin-demo:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:example:jar:25.0-SNAPSHOT:compile")));

        matcher.setArtifactId("*vaadin*");
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.example:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:vaadin-demo:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:vaadindemo:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo:demo-vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(fromString(
                "com.vaadin.demo.a:demovaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:example:jar:25.0-SNAPSHOT:compile")));
    }

    @Test
    public void matches_exactArtifact() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher(
                null, "vaadin");
        Assert.assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertTrue(matcher.matches(
                fromString("org.example:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:vaadindemo:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:demovaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(fromString(
                "com.vaadin:demovaadindemo:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:example:jar:25.0-SNAPSHOT:compile")));

    }

    @Test
    public void matches_exactGroupAndArtifact() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin", "vaadin");
        Assert.assertTrue(matcher.matches(
                fromString("com.vaadin:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(fromString(
                "com.vaadin:flow-server:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(fromString(
                "com.vaadin.demo:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(fromString(
                "org.com.vaadin.demo:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadindemo:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("org.example:vaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:vaadindemo:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:demovaadin:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(fromString(
                "com.vaadin:demovaadindemo:jar:25.0-SNAPSHOT:compile")));
        Assert.assertFalse(matcher.matches(
                fromString("com.vaadin:example:jar:25.0-SNAPSHOT:compile")));
    }

    @Test
    public void matches_nullArtifact_returnsFalse() {
        FrontendScannerConfig.ArtifactMatcher matcher = new FrontendScannerConfig.ArtifactMatcher();
        Assert.assertFalse(matcher.matches(null));
        matcher.setGroupId("com.vaadin");
        Assert.assertFalse(matcher.matches(null));
        matcher.setArtifactId("vaadin");
        Assert.assertFalse(matcher.matches(null));
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
