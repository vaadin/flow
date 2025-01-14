package com.vaadin.flow.plugin.maven;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration for the fast reflector.
 *
 * @apiNote These classes are designed to only hold data and should contain no logic.
 */
public class FastReflectorConfig {
    private boolean enabled = true;
    @Nonnull
    private Isolation isolation = new Isolation();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Nonnull
    public Isolation getIsolation() {
        return isolation;
    }

    public void setIsolation(@Nonnull Isolation isolation) {
        this.isolation = isolation;
    }

    public static class Isolation {
        private boolean includeWhenInOutputDirectory = true;
        @Nonnull
        private Set<String> outputDirectoryNames = new HashSet<>(Set.of("target"));
        @Nonnull
        private ArtifactSelectors excludes = new ArtifactSelectors();
        @Nonnull
        private ArtifactSelectors includes = new ArtifactSelectors();

        public boolean isIncludeWhenInOutputDirectory() {
            return this.includeWhenInOutputDirectory;
        }

        public void setIncludeWhenInOutputDirectory(final boolean includeWhenInOutputDirectory) {
            this.includeWhenInOutputDirectory = includeWhenInOutputDirectory;
        }

        @Nonnull
        public Set<String> getOutputDirectoryNames() {
            return this.outputDirectoryNames;
        }

        public void setOutputDirectoryNames(@Nonnull final Set<String> outputDirectoryNames) {
            this.outputDirectoryNames = outputDirectoryNames;
        }

        @Nonnull
        public ArtifactSelectors getExcludes() {
            return this.excludes;
        }

        public void setExcludes(@Nonnull final ArtifactSelectors excludes) {
            this.excludes = excludes;
        }

        @Nonnull
        public ArtifactSelectors getIncludes() {
            return this.includes;
        }

        public void setIncludes(@Nonnull final ArtifactSelectors includes) {
            this.includes = includes;
        }
    }

    public static class ArtifactSelectors {
        private boolean defaults = true;
        @Nonnull
        private List<ArtifactSelector> additional = new ArrayList<>();

        public boolean isDefaults() {
            return this.defaults;
        }

        public void setDefaults(final boolean defaults) {
            this.defaults = defaults;
        }

        @Nonnull
        public List<ArtifactSelector> getAdditional() {
            return this.additional;
        }

        public void setAdditional(@Nonnull final List<ArtifactSelector> additional) {
            this.additional = additional;
        }
    }


    public static class ArtifactSelector {
        private String groupId;
        private String artifactId;

        /**
         * Determines if the selector should also be applied for scanning (using the reflections library) and not just
         * for class loading.
         * <p>
         * This should be set to <code>false</code> when no Vaadin specific code like Vaadin annotations are present
         * to improve the scanning speed.
         * </p>
         * <p>
         * Please note that this only works for inclusions, not exclusions.
         * </p>
         */
        private boolean scan = true;

        public ArtifactSelector() {
        }

        public ArtifactSelector(final String groupId) {
            this.groupId = groupId;
        }

        public ArtifactSelector(final String groupId, final String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        public String getGroupId() {
            return this.groupId;
        }

        public void setGroupId(final String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return this.artifactId;
        }

        public void setArtifactId(final String artifactId) {
            this.artifactId = artifactId;
        }

        public boolean isScan() {
            return this.scan;
        }

        public void setScan(final boolean scan) {
            this.scan = scan;
        }

        @Override
        public String toString() {
            return (this.groupId != null ? this.groupId : "*")
                    + ":"
                    + (this.artifactId != null ? this.artifactId : "*")
                    + (!this.scan ? " NO_SCAN" : "");
        }
    }
}
