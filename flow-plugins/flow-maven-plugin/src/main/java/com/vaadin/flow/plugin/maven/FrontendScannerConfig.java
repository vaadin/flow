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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.maven.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Reflector.Cloneable
public class FrontendScannerConfig {

    static final Predicate<Artifact> DEFAULT_FILTER = withDefaults()::shouldScan;

    private final boolean silent;

    private boolean enabled = true;

    private boolean includeOutputDirectory = true;

    private List<ArtifactMatcher> includes = new ArrayList<>();

    private List<ArtifactMatcher> excludes = new ArrayList<>();

    public FrontendScannerConfig() {
        this.silent = false;
    }

    private FrontendScannerConfig(boolean silent) {
        this.silent = silent;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setIncludeOutputDirectory(boolean includeOutputDirectory) {
        this.includeOutputDirectory = includeOutputDirectory;
    }

    public boolean isIncludeOutputDirectory() {
        return includeOutputDirectory;
    }

    public List<ArtifactMatcher> getExcludes() {
        return excludes;
    }

    public void addExclude(ArtifactMatcher artifactMatcher) {
        excludes.add(artifactMatcher);
    }

    public List<ArtifactMatcher> getIncludes() {
        return includes;
    }

    public void addInclude(ArtifactMatcher artifactMatcher) {
        includes.add(artifactMatcher);
    }

    private static final Logger LOGGER = LoggerFactory
            .getLogger(FrontendScannerConfig.class);

    // Verifies if the given artifact should be analyzed by the frontend scanner
    // Exclusions have higher priority and are checked first; if exclusion rules
    // pass, it evaluates inclusion rules.
    // returns true if there are no rules to evaluate or if the artifact matches
    // all rules, otherwise false.
    boolean shouldScan(Artifact artifact) {
        if (!enabled) {
            return true;
        }
        if (!excludes.isEmpty() && excludes.stream()
                .anyMatch(matcher -> matcher.matches(artifact))) {
            log("Artifact {} rejected by exclusion rules", artifact.getId());
            return false;
        }
        if (!includes.isEmpty() && includes.stream()
                .noneMatch(matcher -> matcher.matches(artifact))) {
            log("Artifact {} rejected because not matching inclusion rules",
                    artifact.getId());
            return false;
        }
        log("Artifact {} accepted", artifact.getId());
        return true;
    }

    private void log(String message, Object... args) {
        if (!silent && LOGGER.isDebugEnabled()) {
            LOGGER.debug(message, args);
        }
    }

    // Vaadin artifact should always be scanned to prevent the user ignoring
    // them by mistake; however, well know Vaadin artifact that do not contain
    // any frontend reference can be safely excluded.
    // In addition, logging is turned off to prevent confusion with user
    // configured scanning rules.
    private static FrontendScannerConfig withDefaults() {
        FrontendScannerConfig out = new FrontendScannerConfig(true);
        out.addInclude(
                new FrontendScannerConfig.ArtifactMatcher("com.vaadin", "*"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin.external.gw", "*"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin.servletdetector", "*"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher("com.vaadin",
                "open"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher("com.vaadin",
                "license-checker"));
        return out;
    }

    @Override
    public String toString() {
        return "FrontendScannerConfig { enabled=" + enabled
                + ", includeOutputDirectory=" + includeOutputDirectory
                + ", includes=" + includes + ", excludes=" + excludes + '}';
    }

    /**
     * Matches artifacts: can use * as wildcard but only at the beginning or
     * ending of the rule For example, 'com.vaadin*', '*.vaadin' and
     * '*.vaadin.*' are valid, but 'com.*.vaadin' is not
     */
    public static class ArtifactMatcher {
        // setters do not exactly match field names to prevent Maven field
        // injection, so validation rules can be applied by setters
        private String groupIdPattern;
        private String artifactPattern;

        public ArtifactMatcher() {
        }

        public ArtifactMatcher(String groupId, String artifactId) {
            this.groupIdPattern = groupId;
            this.artifactPattern = artifactId;
        }

        public String getGroupId() {
            return groupIdPattern;
        }

        public void setGroupId(String groupId) {
            validatePattern(groupId);
            this.groupIdPattern = groupId;
        }

        public String getArtifactId() {
            return artifactPattern;
        }

        public void setArtifactId(String artifactId) {
            validatePattern(artifactId);
            this.artifactPattern = artifactId;
        }

        public boolean matches(Artifact artifact) {
            if (artifact == null) {
                return false;
            }
            boolean allGroups = groupIdPattern == null
                    || groupIdPattern.isBlank() || "*".equals(groupIdPattern);
            boolean allArtifacts = artifactPattern == null
                    || artifactPattern.isBlank() || "*".equals(artifactPattern);
            if (allGroups && allArtifacts) {
                return true;
            }
            if (!allGroups
                    && !matchesPattern(groupIdPattern, artifact.getGroupId())) {
                return false;
            }
            return allArtifacts || matchesPattern(artifactPattern,
                    artifact.getArtifactId());
        }

        private static boolean matchesPattern(String pattern, String value) {
            boolean startWildcard = pattern.charAt(0) == '*';
            int patternLength = pattern.length();
            boolean endWildcard = pattern.charAt(patternLength - 1) == '*';
            if (startWildcard && endWildcard) {
                return value.contains(pattern.substring(1, patternLength - 1));
            } else if (startWildcard) {
                return value.endsWith(pattern.substring(1));
            } else if (endWildcard) {
                return value
                        .startsWith(pattern.substring(0, patternLength - 1));
            }
            return value.equals(pattern);
        }

        private static void validatePattern(String pattern) {
            if (pattern != null) {
                int idx = pattern.indexOf('*');
                if (idx > 0 && idx < pattern.length() - 2) {
                    throw new IllegalArgumentException("Invalid pattern: '"
                            + pattern
                            + "'. * can be only at the begin and the end of the pattern");
                }
            }
        }

        @Override
        public String toString() {
            return (this.groupIdPattern != null
                    && !this.groupIdPattern.isBlank() ? this.groupIdPattern
                            : "*")
                    + ':'
                    + (this.artifactPattern != null
                            && !this.artifactPattern.isBlank()
                                    ? this.artifactPattern
                                    : "*");
        }
    }
}
