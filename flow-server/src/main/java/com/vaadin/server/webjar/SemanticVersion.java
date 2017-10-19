/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server.webjar;

import java.util.Objects;

/**
 * Holds semantic version information.
 *
 * @see <a href="http://semver.org/">Semantic versioning page</a>
 * @see <a href="https://bower.io/docs/creating-packages/">Bower and semantic
 *      versioning</a>
 */
public class SemanticVersion {
    private static final char WEBJAR_VERSION_INCORRECT_PREFIX = 'v';

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;

    /**
     * Parses provided string into a semantic version.
     *
     * @param versionString
     *            semantic version string, not {@code null}
     *
     * @throws IllegalArgumentException
     *             if {@code versionString} cannot be parsed as semantic version
     *             string
     */
    public SemanticVersion(String versionString) {
        String[] versionParts = removeIncorrectVersionPrefix(
                Objects.requireNonNull(versionString)).split("\\.", 3);
        if (versionParts.length != 3) {
            throw new IllegalArgumentException(
                    createSemanticVersioningErrorString(versionString));
        }
        major = parseVersionPart(versionString, versionParts[0]);
        minor = parseVersionPart(versionString, versionParts[1]);

        String[] patchAndPreReleaseParts = versionParts[2].split("-", 2);
        patch = parseVersionPart(versionString, patchAndPreReleaseParts[0]);
        preRelease = patchAndPreReleaseParts.length > 1 ? patchAndPreReleaseParts[1] : "";
    }

    private String createSemanticVersioningErrorString(String versionString) {
        return String.format(
                "Could not parse string '%s' as a semantic version string, see http://semver.org/ for details.",
                versionString);
    }

    private String removeIncorrectVersionPrefix(String version) {
        if (version.charAt(0) == WEBJAR_VERSION_INCORRECT_PREFIX) {
            return version.substring(1);
        } else {
            return version;
        }
    }

    private int parseVersionPart(String fullVersionString,
            String versionPartString) {
        try {
            return Integer.parseInt(versionPartString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    createSemanticVersioningErrorString(fullVersionString), e);
        }
    }

    /**
     * Compares patch parts of the versions, also checking that major and minor
     * parts of the dependency are the same.
     *
     * @param version
     *            the version to compare with
     * @return zero integer, if versions have the same patch part, positive
     *         integer, if current version was released later, negative integer
     *         otherwise
     *
     * @throws IllegalArgumentException
     *             if other version has different major or minor version
     */
    public int comparePatchParts(SemanticVersion version) {
        Objects.requireNonNull(version);
        if (major != version.major || minor != version.minor) {
            throw new IllegalArgumentException(String.format(
                    "Received incomparable bower webJars with different bower names: '%s' and '%s'",
                    this, version));
        }
        int patchComparison = Integer.compare(patch, version.patch);
        return patchComparison == 0 ? compareStringsByCharacter(preRelease, version.preRelease) : patchComparison;
    }

    private int compareStringsByCharacter(String first, String second) {
        if (first.length() == 0 && second.length() != 0) {
            return 1;
        }
        if (first.length() != 0 && second.length() == 0) {
            return -1;
        }

        int minLength = Math.min(first.length(), second.length());
        for (int i = 0; i < minLength; i++) {
            int comparison = Character.compare(first.charAt(i),
                    second.charAt(i));
            if (comparison != 0) {
                return comparison;
            }
        }
        if (second.length() > minLength) {
            return -1;
        } else if (first.length() > minLength) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "SemanticVersion{" + "major=" + major + ", minor=" + minor
                + ", patch='" + patch + '\'' + '}';
    }
}
