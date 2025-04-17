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
package com.vaadin.flow.server;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.StringUtil;

/**
 * Provides information about the current version of Vaadin Flow.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Version implements Serializable {
    /**
     * The version number of this release. For example "6.2.0". Always in the
     * format "major.minor.revision[.build]". The build part is optional. All of
     * major, minor, revision must be integers.
     */
    private static final String VERSION;
    /**
     * Major version number. For example 6 in 6.2.0.
     */
    private static final int VERSION_MAJOR;

    /**
     * Minor version number. For example 2 in 6.2.0.
     */
    private static final int VERSION_MINOR;

    /**
     * Version revision number. For example 0 in 6.2.0.
     */
    private static final int VERSION_REVISION;

    /**
     * Build identifier. For example "nightly-20091123-c9963" in
     * 6.2.0.nightly-20091123-c9963.
     */
    private static final String VERSION_BUILD;

    /**
     * Build hash based on the timestamp of the build.
     */
    private static final String VERSION_BUILD_HASH;

    /* Initialize version numbers from string replaced by build-script. */
    static {
        String flowVersion = "9.9.9.INTERNAL-DEBUG-BUILD";
        String buildTimestamp = "";
        Properties properties = new Properties();
        try {
            properties.load(
                    Version.class.getResourceAsStream("version.properties"));
            flowVersion = properties.getProperty("flow.version");
            buildTimestamp = properties.getProperty("flow.build.timestamp");
        } catch (IOException e) {
            LoggerFactory.getLogger(Version.class.getName())
                    .warn("Unable to determine Flow version number", e);
        }

        VERSION = flowVersion;
        final String[] digits = VERSION.split("[-.]", 4);
        VERSION_MAJOR = Integer.parseInt(digits[0]);
        VERSION_MINOR = Integer.parseInt(digits[1]);
        int revision;
        String build = "";
        try {
            revision = Integer.parseInt(digits[2]);
            if (digits.length == 4) {
                build = digits[3];
            }
        } catch (NumberFormatException e) {
            // 1.0-SNAPSHOT -> 1.0.0-SNAPSHOT
            revision = 0;
            if (digits.length >= 3) {
                build = digits[2];
            }
        }
        VERSION_REVISION = revision;
        VERSION_BUILD = build;
        VERSION_BUILD_HASH = StringUtil.getHash(buildTimestamp,
                StandardCharsets.UTF_8);
    }

    /**
     * Gets the full version, in format {@literal x.y.z} or
     * {@literal x.y.z.qualifier}.
     *
     * @return the full version number
     */
    public static String getFullVersion() {
        return VERSION;
    }

    /**
     * Gets the major version, {@literal x} in {@literal x.y.z.qualifier}.
     *
     * @return the major version number
     */
    public static int getMajorVersion() {
        return VERSION_MAJOR;
    }

    /**
     * Gets the minor version, {@literal y} in {@literal x.y.z.qualifier}.
     *
     * @return the minor version number
     */
    public static int getMinorVersion() {
        return VERSION_MINOR;
    }

    /**
     * Gets the revision, {@literal z} in {@literal x.y.z.qualifier}.
     *
     * @return the revision number
     */
    public static int getRevision() {
        return VERSION_REVISION;
    }

    /**
     * Gets the version qualifier, {@literal qualifier} in
     * {@literal x.y.z.qualifier}.
     *
     * @return the version qualifier
     */
    public static String getBuildIdentifier() {
        return VERSION_BUILD;
    }

    /**
     * Gets the version's build hash. This hash is based on build's timestamp
     * and varies from build to build.
     *
     * @return version's build hash
     */
    public static String getBuildHash() {
        return VERSION_BUILD_HASH;
    }

}
