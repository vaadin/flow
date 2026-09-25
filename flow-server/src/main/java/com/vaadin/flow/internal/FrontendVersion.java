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
package com.vaadin.flow.internal;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version object for frontend versions comparison and handling.
 * <p>
 * Equality, hash codes and ordering use version parts only, ignoring npm alias
 * targets. Use {@link #isSameDependency(FrontendVersion)} when both the version
 * and package target must match, or
 * {@link #hasSamePackageTarget(FrontendVersion)} to compare targets alone.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.1
 */
public class FrontendVersion
        implements Serializable, Comparable<FrontendVersion> {

    private static final Pattern NPM_ALIAS = Pattern.compile(
            "^npm:((?:@[^/@\\s]+/)?[^/@\\s]+)@([~^]?\\d+(?:\\.\\d+){0,2}(?:-[0-9A-Za-z.-]+)?)$");

    private static final Pattern VERSION_SEPARATOR = Pattern.compile("[.-]");

    /**
     * Parses the buildIdentifier to String + Integer. For instance beta1
     * returns 'beta' and '1'
     */
    private final Pattern buildIdentifierParser = Pattern
            .compile("(\\D*)(\\d*)");

    /**
     * The version number of this release. For example "6.2.0". Always in the
     * format "major.minor.revision[.build]". The build part is optional. All of
     * major, minor, revision must be integers.
     */
    private final String version;

    private final String aliasTarget;

    /**
     * Major version number. For example 6 in 6.2.0.
     */
    private final int majorVersion;

    /**
     * Minor version number. For example 2 in 6.2.0.
     */
    private final int minorVersion;

    /**
     * Version revision number. For example 0 in 6.2.0.
     */
    private final int revision;

    /**
     * Build identifier. For example "nightly-20091123-c9963" in
     * 6.2.0.nightly-20091123-c9963.
     */
    private final String buildIdentifier;

    /**
     * Create a version of format "major.minor.0".
     *
     * @param major
     *            major version
     * @param minor
     *            minor version
     */
    public FrontendVersion(int major, int minor) {
        this(major, minor, 0);
    }

    /**
     * Create a version of format "major.minor.revision".
     *
     * @param major
     *            major version
     * @param minor
     *            minor version
     * @param revision
     *            revision number
     */
    public FrontendVersion(int major, int minor, int revision) {
        this(major, minor, revision, "");
    }

    /**
     * Create a version of format "major.minor.revision.build"
     *
     * @param major
     *            major version
     * @param minor
     *            minor version
     * @param revision
     *            revision number
     * @param build
     *            build identifier
     */
    public FrontendVersion(int major, int minor, int revision, String build) {
        aliasTarget = null;
        if (build.isEmpty()) {
            this.version = major + "." + minor + "." + revision;
        } else {
            this.version = major + "." + minor + "." + revision + "." + build;
        }
        majorVersion = major;
        minorVersion = minor;
        this.revision = revision;
        buildIdentifier = build;
    }

    /**
     * Parse version numbers from version string with the format
     * "major.minor.revision[.build]". The build part is optional.
     * <p>
     * Versions are normalized and any caret or tildes will not be considered
     * when comparing versions. Versioned npm aliases are compared using their
     * embedded version, while retaining the full alias specification.
     *
     * @param version
     *            version string as "major.minor.revision[.build]"
     */
    public FrontendVersion(String version) {
        this(null, version);
    }

    /**
     * Parse version numbers from version string with the format
     * "major.minor.revision[.build]". The build part is optional.
     * <p>
     * Versions are normalized and any caret or tildes will not be considered
     * when comparing versions. Versioned npm aliases are compared using their
     * embedded version, while retaining the full alias specification.
     *
     * @param name
     *            the name of the artifact which version is to be parsed, used
     *            in error message to help discover the issue
     * @param version
     *            version string as "major.minor.revision[.build]"
     */
    public FrontendVersion(String name, String version) {
        Objects.requireNonNull(version);
        String originalVersion = version;
        if (version.startsWith("npm:")) {
            Matcher alias = NPM_ALIAS.matcher(version);
            if (!alias.matches()) {
                throw new NumberFormatException(
                        getInvalidVersionMessage(name, version));
            }
            aliasTarget = alias.group(1);
            version = alias.group(2);
        } else {
            aliasTarget = null;
        }
        if (version.isEmpty()) {
            throw new NumberFormatException(
                    getInvalidVersionMessage(name, version));
        }
        String numericVersion = !Character.isDigit(version.charAt(0))
                ? version.substring(1).trim()
                : version.trim();
        this.version = aliasTarget == null ? numericVersion : originalVersion;

        final String[] digits = VERSION_SEPARATOR.split(numericVersion, 4);
        try {
            majorVersion = Integer.parseInt(digits[0]);
        } catch (NumberFormatException nfe) {
            throw new NumberFormatException(
                    getInvalidVersionMessage(name, version));
        }
        if (digits.length >= 2) {
            try {
                minorVersion = Integer.parseInt(digits[1]);
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException(
                        getInvalidVersionMessage(name, version));
            }
        } else {
            minorVersion = 0;
        }
        int revisionNumber;
        String build = "";
        try {
            revisionNumber = digits.length >= 3 ? Integer.parseInt(digits[2])
                    : 0;
            if (digits.length == 4) {
                build = digits[3];
            }
        } catch (NumberFormatException e) {
            // 1.0-SNAPSHOT -> 1.0.0-SNAPSHOT
            revisionNumber = 0;
            if (digits.length >= 3) {
                build = digits[2];
            }
        }
        this.revision = revisionNumber;
        buildIdentifier = build;
    }

    /**
     * Gets the full version, in format {@literal x.y.z} or
     * {@literal x.y.z.qualifier}. For npm aliases, returns the complete alias
     * specification so that writing it back preserves the target package.
     *
     * @return the full version number
     */
    public String getFullVersion() {
        return version;
    }

    /**
     * Checks whether two versions refer to the same npm alias target, or both
     * are ordinary versions. Version ordering itself ignores alias targets.
     *
     * @param other
     *            the version to compare
     * @return whether the package targets match
     */
    public boolean hasSamePackageTarget(FrontendVersion other) {
        return Objects.equals(aliasTarget, other.aliasTarget);
    }

    /**
     * Checks both package target and version when detecting dependency edits.
     *
     * @param other
     *            the version to compare
     * @return whether the target and parsed version are equal
     */
    public boolean isSameDependency(FrontendVersion other) {
        return hasSamePackageTarget(other) && isEqualTo(other);
    }

    /**
     * Gets the major version, {@literal x} in {@literal x.y.z.qualifier}.
     *
     * @return the major version number
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Gets the minor version, {@literal y} in {@literal x.y.z.qualifier}.
     *
     * @return the minor version number
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Gets the revision, {@literal z} in {@literal x.y.z.qualifier}.
     *
     * @return the revision number
     */
    public int getRevision() {
        return revision;
    }

    /**
     * Gets the version qualifier, {@literal qualifier} in
     * {@literal x.y.z.qualifier}.
     *
     * @return the version qualifier
     */
    public String getBuildIdentifier() {
        return buildIdentifier;
    }

    /**
     * Check if this version is older than given version. Will return false if
     * equals or is newer.
     *
     * @param otherVersion
     *            version to check against
     * @return true if this is older than otherVersion
     */
    public boolean isOlderThan(FrontendVersion otherVersion) {
        return compareTo(otherVersion) < 0;
    }

    /**
     * Check if this version is newer than given version. Will return false if
     * equals or is older.
     *
     * @param otherVersion
     *            version to check against
     * @return true if this is newer than otherVersion
     */
    public boolean isNewerThan(FrontendVersion otherVersion) {
        return compareTo(otherVersion) > 0;
    }

    /**
     * Check if this version is equal or newer than given version. Will return
     * false if is older.
     *
     * @param otherVersion
     *            version to check against
     * @return true if this is newer than or equal to otherVersion
     */
    public boolean isEqualOrNewer(FrontendVersion otherVersion) {
        return compareTo(otherVersion) >= 0;
    }

    /**
     * Check if this and the given version are equal to each other.
     *
     * @param otherVersion
     *            version to test equals with
     * @return true if parsed version parts are exactly the same
     */
    public boolean isEqualTo(FrontendVersion otherVersion) {
        return compareTo(otherVersion) == 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FrontendVersion other) {
            return majorVersion == other.getMajorVersion()
                    && minorVersion == other.getMinorVersion()
                    && revision == other.getRevision()
                    && buildIdentifier.equals(other.getBuildIdentifier());

        }
        return false;
    }

    @Override
    public String toString() {
        return "FrontendVersion [" + "majorVersion=" + majorVersion
                + ", minorVersion=" + minorVersion + ", revision=" + revision
                + ", buildIdentifier=" + buildIdentifier + "]";
    }

    @Override
    public int hashCode() {
        return (majorVersion + "." + minorVersion + "." + revision + "."
                + buildIdentifier).hashCode();
    }

    /**
     * Compare version numbers and return order as -1, 0 and 1. Where this
     * version is older, equals, newer than given version.
     *
     * @param other
     *            version to compare against this version
     * @return -1 this is older, 0 versions equal, 1 this is newer
     */
    @Override
    public int compareTo(FrontendVersion other) {
        if (majorVersion != other.majorVersion) {
            return Integer.compare(majorVersion, other.majorVersion);
        }
        if (minorVersion != other.minorVersion) {
            return Integer.compare(minorVersion, other.minorVersion);
        }
        if (revision != other.revision) {
            return Integer.compare(revision, other.revision);
        }
        if (this.buildIdentifier != other.buildIdentifier) {
            if (buildIdentifier.isEmpty() && !other.buildIdentifier.isEmpty()) {
                return 1;
            } else if (!buildIdentifier.isEmpty()
                    && other.buildIdentifier.isEmpty()) {
                return -1;
            }
            return compareBuildIdentifier(other);
        }
        return 0;
    }

    private int compareBuildIdentifier(FrontendVersion other) {
        final Matcher thisMatcher = buildIdentifierParser
                .matcher(buildIdentifier);
        final Matcher otherMatcher = buildIdentifierParser
                .matcher(other.buildIdentifier);
        if (thisMatcher.find() && otherMatcher.find()) {
            if (thisMatcher.group(1)
                    .compareToIgnoreCase(otherMatcher.group(1)) != 0) {
                // If we do not have a text identifier assume newer
                // If other doesn't have text identifier assume older
                if (thisMatcher.group(1).isEmpty()) {
                    return 1;
                } else if (otherMatcher.group(1).isEmpty()) {
                    return -1;
                }
                return thisMatcher.group(1)
                        .compareToIgnoreCase(otherMatcher.group(1));
            }
            // if one or both are missing numeric value do not parse int
            if (thisMatcher.group(2).isEmpty()
                    || otherMatcher.group(2).isEmpty()) {
                return buildIdentifier
                        .compareToIgnoreCase(other.buildIdentifier);
            }
            return Integer.parseInt(thisMatcher.group(2))
                    - Integer.parseInt(otherMatcher.group(2));
        }
        return buildIdentifier.compareToIgnoreCase(other.buildIdentifier);
    }

    private String getInvalidVersionMessage(String name, String version) {
        if (name != null) {
            return String.format("'%s' is not a valid version for '%s'!",
                    version, name);
        } else {
            return String.format("'%s' is not a valid version!", version);
        }
    }

}
