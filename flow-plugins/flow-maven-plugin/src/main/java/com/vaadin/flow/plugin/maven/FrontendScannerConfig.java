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
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.maven.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for filtering Maven artifacts to be scanned by the
 * plugin. This class allows enabling or disabling the scanner and defining
 * inclusion and exclusion rules.
 * <p>
 *
 * Exclusions have higher priority and are checked first. If an artifact matches
 * an exclusion rule, it is not scanned. If no exclusion rule applies, inclusion
 * rules are evaluated. If the artifact doesn't even match the inclusion rule,
 * it is not scanned.
 */
@Reflector.Cloneable
public class FrontendScannerConfig {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(FrontendScannerConfig.class);

    static final Predicate<Artifact> DEFAULT_FILTER = withDefaults()::shouldScan;

    static final Predicate<Artifact> DEV_EXCLUSION_FILTER = devExclusions()::shouldScan;

    private final boolean silent;

    private boolean enabled = true;

    private boolean includeOutputDirectory = true;

    private final List<ArtifactMatcher> includes = new ArrayList<>();

    private final List<ArtifactMatcher> excludes = new ArrayList<>();

    /**
     * Creates a new empty configuration, accepting all artifacts.
     */
    public FrontendScannerConfig() {
        this.silent = false;
    }

    FrontendScannerConfig(boolean silent) {
        this.silent = silent;
    }

    /**
     * Sets whether the frontend scanner is enabled or not.
     *
     * @param enabled
     *            {@code true} to enable frontend scan filtering, otherwise
     *            {@code false}.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Indicates whether the frontend scanning filtering is enabled. Default is
     * {@code true}.
     *
     * @return {@code true} if frontend scanning filtering is enabled, otherwise
     *         {@code false}.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets if the output directory should be included in the scan.
     * <p>
     *
     * Can be turned on to make scanning faster if the maven module itself does
     * not have classes referencing frontend resources or Vaadin components or
     * add-ons.
     *
     * @param includeOutputDirectory
     *            {@code true} to scan output directory, otherwise
     *            {@code false}.
     */
    public void setIncludeOutputDirectory(boolean includeOutputDirectory) {
        this.includeOutputDirectory = includeOutputDirectory;
    }

    /**
     * Determines if the output directory should be included in the scan.
     * Default is {@code true}.
     *
     * @return {@code true} if output directory should be included in the scan,
     *         otherwise {@code false}.
     */
    public boolean isIncludeOutputDirectory() {
        return includeOutputDirectory;
    }

    /**
     * Gets the list of artifact matchers specifying which artifacts should be
     * excluded from the scan.
     *
     * @return the list of artifact exclusions.
     */
    public List<ArtifactMatcher> getExcludes() {
        return List.copyOf(excludes);
    }

    /**
     * Adds an artifact matcher to the exclude list.
     *
     * @param artifactMatcher
     *            the artifact matcher to be excluded, not {@literal null}.
     */
    public void addExclude(ArtifactMatcher artifactMatcher) {
        excludes.add(Objects.requireNonNull(artifactMatcher,
                "Artifact matcher must not be null"));
    }

    /**
     * Gets the ist of artifact matchers specifying which artifacts should be
     * included in the scan.
     *
     * @return the list of artifact inclusions.
     */
    public List<ArtifactMatcher> getIncludes() {
        return List.copyOf(includes);
    }

    /**
     * Adds an artifact matcher to the include list.
     *
     * @param artifactMatcher
     *            the artifact matcher to be included, not {@literal null}.
     */
    public void addInclude(ArtifactMatcher artifactMatcher) {
        includes.add(Objects.requireNonNull(artifactMatcher,
                "Artifact matcher must not be null"));
    }

    /**
     * Determines whether the given artifact should be analyzed by the frontend
     * scanner.
     * <p>
     *
     * Exclusions have higher priority and are checked first. If an artifact
     * matches an exclusion rule, it is not scanned. If no exclusion rule
     * applies, inclusion rules are evaluated.
     *
     * @param artifact
     *            the artifact to be evaluated
     * @return {@code true} if the configuration is disabled or there are no
     *         rules to evaluate or if the artifact matches all applicable
     *         rules, otherwise {@code false}
     */
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
        setupDefaultExclusions(out);
        return out;
    }

    private static FrontendScannerConfig devExclusions() {
        FrontendScannerConfig out = new FrontendScannerConfig(true);
        out.addInclude(new FrontendScannerConfig.ArtifactMatcher("*", "*"));
        setupDefaultExclusions(out);
        return out;
    }

    private static void setupDefaultExclusions(FrontendScannerConfig out) {
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin.external.gw", "*"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher(
                "com.vaadin.servletdetector", "*"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher("com.vaadin",
                "open"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher("com.vaadin",
                "license-checker"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher("com.vaadin",
                "vaadin-dev"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher("com.vaadin",
                "vaadin-dev-server"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher("com.vaadin",
                "vaadin-dev-bundle"));
        out.addExclude(new FrontendScannerConfig.ArtifactMatcher("com.vaadin",
                "copilot"));
    }

    @Override
    public String toString() {
        return "FrontendScannerConfig { enabled=" + enabled
                + ", includeOutputDirectory=" + includeOutputDirectory
                + ", includes=" + includes + ", excludes=" + excludes + '}';
    }

    /**
     * Represents a pattern-based matcher for Maven artifacts.
     * <p>
     *
     * Patterns can use the wildcard {@code *}, but only at the beginning or end
     * of the rule. Examples of valid patterns:
     * <ul>
     * <li>{@code com.vaadin*}</li>
     * <li>{@code *.vaadin}</li>
     * <li>{@code *.vaadin.*}</li>
     * <li>{@code *}</li>
     * </ul>
     * Invalid example: {@code com.*.vaadin}
     */
    public static class ArtifactMatcher {
        // setters do not exactly match field names to prevent Maven field
        // injection, so validation rules can be applied by setters
        private String groupIdPattern;
        private String artifactPattern;

        /**
         * Creates an undefined instance that matches everything.
         */
        public ArtifactMatcher() {
        }

        /**
         * Creates a matcher for the given {@code group} and
         * {@code artifact name} patterns.
         *
         * @param groupId
         *            the pattern for matching the artifact's group ID; can be
         *            {@literal null}.
         * @param artifactId
         *            the pattern for matching the artifact's artifact ID; can
         *            be {@literal null}.
         */
        public ArtifactMatcher(String groupId, String artifactId) {
            this.groupIdPattern = groupId;
            this.artifactPattern = artifactId;
        }

        /**
         * Gets the pattern for matching the artifact's group ID.
         *
         * @return the pattern for matching the artifact's group ID; can be
         *         {@literal null}.
         */
        public String getGroupId() {
            return groupIdPattern;
        }

        /**
         * Sets the pattern for matching the artifact's group ID.
         * <p>
         *
         * The argument must be a valid pattern as describe in the class
         * Javadoc. {@literal null} is and allowed and value, and it acts like
         * setting {@code *}, meaning every group ID is allowed.
         *
         * @param groupId
         *            the pattern for matching the artifact's group ID; can be
         *            {@literal null}.
         */
        public void setGroupId(String groupId) {
            validatePattern(groupId);
            this.groupIdPattern = groupId;
        }

        /**
         * Gets the pattern for matching the artifact's artifact ID.
         *
         * @return the pattern for matching the artifact's artifact ID; can be
         *         {@literal null}.
         */
        public String getArtifactId() {
            return artifactPattern;
        }

        /**
         * Sets the pattern for matching the artifact's artifact ID.
         * <p>
         *
         * The argument must be a valid pattern as describe in the class
         * Javadoc. {@literal null} is and allowed and value, and it acts like
         * setting {@code *}, meaning every artifact ID is allowed.
         *
         * @param artifactId
         *            the pattern for matching the artifact's artifact ID; can
         *            be {@literal null}.
         */
        public void setArtifactId(String artifactId) {
            validatePattern(artifactId);
            this.artifactPattern = artifactId;
        }

        /**
         * Evaluates whether a given artifact matches the configured patterns.
         *
         * @param artifact
         *            the artifact to be checked.
         * @return {@code true} if the artifact matches the patterns,
         *         {@code false} otherwise.
         */
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
                long wildcards = pattern.chars().filter(c -> c == '*').count();
                int idx = pattern.indexOf('*');
                int lastIdx = pattern.lastIndexOf('*');
                if (wildcards > 2 || (idx > 0 && idx < pattern.length() - 1)
                        || (lastIdx > 0 && lastIdx < pattern.length() - 1)) {
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
