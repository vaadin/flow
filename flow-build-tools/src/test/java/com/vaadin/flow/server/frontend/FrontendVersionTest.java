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
package com.vaadin.flow.server.frontend;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.internal.FrontendVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendVersionTest {

    @Test
    void stringParser_returnsExpectedVersions() {
        FrontendVersion frontendVersion = new FrontendVersion("1.0.1");
        assertVersion(frontendVersion, 1, 0, 1, "");

        frontendVersion = new FrontendVersion("6.10");
        assertVersion(frontendVersion, 6, 10, 0, "");

        frontendVersion = new FrontendVersion("6.10-SNAPSHOT");
        assertVersion(frontendVersion, 6, 10, 0, "SNAPSHOT");

        frontendVersion = new FrontendVersion("6.5.3.alpha1");
        assertVersion(frontendVersion, 6, 5, 3, "alpha1");
    }

    @Test
    void versionConstructors_returnExpectedVersions() {
        FrontendVersion frontendVersion = new FrontendVersion(1, 2);
        assertVersion(frontendVersion, 1, 2, 0, "");

        frontendVersion = new FrontendVersion(1, 2, 5);
        assertVersion(frontendVersion, 1, 2, 5, "");

        frontendVersion = new FrontendVersion(1, 2, 5, "beta3");
        assertVersion(frontendVersion, 1, 2, 5, "beta3");
    }

    @Test
    void testFrontedEquality() {
        FrontendVersion fromString = new FrontendVersion("1.1.0");
        FrontendVersion fromConstructor = new FrontendVersion(1, 1);

        assertTrue(fromString.equals(fromConstructor),
                "Parsed string didn't equal constructor");
        assertTrue(fromConstructor.equals(fromString),
                "Constructor didn't equal parsed string");

        fromString = new FrontendVersion("1.1.alpha12");
        fromConstructor = new FrontendVersion(1, 1, 0, "alpha12");
        assertTrue(fromConstructor.equals(fromString),
                "Major-Minor version with build identifier didn't match");

        fromString = new FrontendVersion("12.3.5.alpha12");
        fromConstructor = new FrontendVersion(12, 3, 5, "alpha12");
        assertTrue(fromString.equals(fromConstructor),
                "Full version with build identifier didn't match");
    }

    @Test
    void testIsEqualTo() {
        FrontendVersion fromString = new FrontendVersion("1.1.0");
        FrontendVersion fromConstructor = new FrontendVersion(1, 1);

        assertTrue(fromString.isEqualTo(fromConstructor),
                "Parsed string didn't equal constructor");
        assertTrue(fromConstructor.isEqualTo(fromString),
                "Constructor didn't equal parsed string");

        fromString = new FrontendVersion("1.1.alpha12");
        fromConstructor = new FrontendVersion(1, 1, 0, "alpha12");
        assertTrue(fromConstructor.isEqualTo(fromString),
                "Major-Minor version with build identifier didn't match");

        fromString = new FrontendVersion("12.3.5.alpha12");
        fromConstructor = new FrontendVersion(12, 3, 5, "alpha12");
        assertTrue(fromString.isEqualTo(fromConstructor),
                "Full version with build identifier didn't match");
    }

    @Test // #12041
    void testSimilarBuildIdentifiers() {
        FrontendVersion version = new FrontendVersion("1.1.1-SNAPSHOT");
        FrontendVersion equals = new FrontendVersion("1.1.1-SNAPSHOT");

        assertTrue(version.isEqualTo(equals), "Versions be the same");
        assertFalse(version.isOlderThan(equals), "Version should not be older");
        assertEquals(0, version.compareTo(equals),
                "Versions should not have a difference");
        assertFalse(version.isNewerThan(equals), "Version should not be newer");
    }

    @Test
    void faultyStringVersion_throwsException() {
        assertThrows(NumberFormatException.class,
                () -> new FrontendVersion("12.0b.1"));
    }

    @Test
    void notANumber_throwsException() {
        assertThrows(NumberFormatException.class,
                () -> new FrontendVersion("a"));
    }

    @Test
    void emptyString_throwsException() {
        assertThrows(NumberFormatException.class,
                () -> new FrontendVersion(""));
    }

    @Test
    void onlyMajorVersion_allVersoinNumbersAreCalculated() {
        assertVersion(new FrontendVersion("3"), 3, 0, 0, "");
    }

    @Test
    void versionHandlesTildeAndCaretVersions() {
        assertVersion(new FrontendVersion("~1.3.0-beta1"), 1, 3, 0, "beta1");
        assertVersion(new FrontendVersion("~2.2"), 2, 2, 0, "");
        assertVersion(new FrontendVersion("^3.3.1"), 3, 3, 1, "");
    }

    @Test
    void olderVersionIsCalculatedCorrectly() {
        FrontendVersion test = new FrontendVersion("2.2.0");

        assertTrue(test.isOlderThan(new FrontendVersion("2.2.1")),
                "Should be older due to revision");
        assertTrue(test.isOlderThan(new FrontendVersion("2.3.0")),
                "Should be older due to minor");
        assertTrue(test.isOlderThan(new FrontendVersion("3.2.0")),
                "Should be older due to major");

        assertFalse(test.isOlderThan(new FrontendVersion("2.2.0-alpha1")),
                "Should be newer as target has buildIdentifier");

        assertFalse(test.isOlderThan(new FrontendVersion("1.2.0")),
                "Should be newer due to major");
        assertFalse(test.isOlderThan(new FrontendVersion("2.1.0")),
                "Should be newer due to minor");
        assertFalse(test.isOlderThan(new FrontendVersion("2.1.2")),
                "Should be newer due to minor");
        assertFalse(test.isOlderThan(new FrontendVersion("1.5.2")),
                "Should be newer due to minor");

        assertFalse(test.isOlderThan(new FrontendVersion("1.2.0-alpha1")),
                "Should be newer by major even with buildIdentifier");
    }

    @Test
    void newerVersionIsCalculatedCorrectly() {
        FrontendVersion test = new FrontendVersion("2.2.2");

        assertTrue(test.isNewerThan(new FrontendVersion("2.2.1")),
                "Should be newer due to revision");
        assertTrue(test.isNewerThan(new FrontendVersion("2.1.2")),
                "Should be newer due to minor");
        assertTrue(test.isNewerThan(new FrontendVersion("1.2.2")),
                "Should be newer due to major");

        assertTrue(test.isNewerThan(new FrontendVersion("2.2.2-alpha1")),
                "Should be newer as target has buildIdentifier");

        assertFalse(test.isNewerThan(new FrontendVersion("3.2.2")),
                "Should be older due to major");
        assertFalse(test.isNewerThan(new FrontendVersion("2.3.2")),
                "Should be older due to minor");
    }

    @Test
    void olderVersionByBuildIdentifier() {

        FrontendVersion test = new FrontendVersion("2.0.0-RC1");

        assertTrue(test.isOlderThan(new FrontendVersion("2.0.0")),
                "2.0.0 should be newer that RC1");
        assertTrue(test.isOlderThan(new FrontendVersion("2.0.0-RC2")),
                "RC2 should be newer that RC1");
        assertFalse(test.isOlderThan(new FrontendVersion("2.0.0-beta5")),
                "beta5 should be older than RC1");
        assertFalse(test.isOlderThan(new FrontendVersion("2.0.0-alpha4")),
                "alpha4 should be older than RC1");

        test = new FrontendVersion("2.0.0");

        assertFalse(test.isOlderThan(new FrontendVersion("2.0.0-RC2")),
                "RC2 should be older that 2.0.0");
        assertFalse(test.isOlderThan(new FrontendVersion("2.0.0-beta5")),
                "beta5 should be older than 2.0.0");
        assertFalse(test.isOlderThan(new FrontendVersion("2.0.0-alpha4")),
                "alpha4 should be older than 2.0.0");
    }

    @Test
    void newerVersionByBuildIdentifier() {

        FrontendVersion test = new FrontendVersion("2.0.0-alpha2");

        assertTrue(test.isNewerThan(new FrontendVersion("2.0.0-alpha1")),
                "alpha2 should be newer than alpha1");
        assertFalse(test.isNewerThan(new FrontendVersion("2.0.0")),
                "alpha2 should be older than 2.0.0");
        assertFalse(test.isNewerThan(new FrontendVersion("2.0.0-beta1")),
                "alpha2 should be older than beta1");
        assertFalse(test.isNewerThan(new FrontendVersion("2.0.0-RC1")),
                "alpha2 should be older than RC1");

        test = new FrontendVersion("2.0.0");

        assertTrue(test.isNewerThan(new FrontendVersion("2.0.0-alpha1")),
                "2.0.0 should be newer than alpha1");
        assertTrue(test.isNewerThan(new FrontendVersion("2.0.0-beta1")),
                "2.0.0 should be older than beta1");
        assertTrue(test.isNewerThan(new FrontendVersion("2.0.0-RC1")),
                "2.0.0 should be older than RC1");
    }

    @Test
    void buildIdentifierNumbers_returnsAsExpected() {

        FrontendVersion test = new FrontendVersion("2.0.0-alpha6");
        assertTrue(test.isOlderThan(new FrontendVersion("2.0.0-alpha13")),
                "2.0.0-alpha6 should be older than 2.0.0-alpha13");

        test = new FrontendVersion("2.0.0-alpha20");
        assertTrue(test.isNewerThan(new FrontendVersion("2.0.0-alpha13")),
                "2.0.0-alpha20 should be newer than 2.0.0-alpha13");
        assertFalse(test.isOlderThan(new FrontendVersion("2.0.0-alpha13")),
                "2.0.0-alpha20 should be newer than 2.0.0-alpha13");

        assertTrue(new FrontendVersion("2.0.0-alpha13").isOlderThan(test),
                "2.0.0-alpha13 should not be older than 2.0.0-alpha20");
        assertFalse(new FrontendVersion("2.0.0-alpha13").isNewerThan(test),
                "2.0.0-alpha13 should not be older than 2.0.0-alpha20");

        assertTrue(test.isEqualTo(new FrontendVersion("2.0.0.alpha20")),
                "same versions should equal");
    }

    @Test
    void testAgainstVersionWithValueInBuildInfo() {
        FrontendVersion alpha3 = new FrontendVersion("2.0.0-alpha3");
        FrontendVersion five = new FrontendVersion("2.0.0.5");
        FrontendVersion fifteen = new FrontendVersion("2.0.0.15");

        assertTrue(alpha3.isOlderThan(five),
                "2.0.0-alpha3 should be older than 2.0.0.5");
        assertFalse(alpha3.isNewerThan(five),
                "2.0.0-alpha3 should be older than 2.0.0.5");

        assertTrue(five.isNewerThan(alpha3),
                "2.0.0.5 should be newer than 2.0.0-alpha3");
        assertFalse(five.isOlderThan(alpha3),
                "2.0.0.5 should be newer than 2.0.0-alpha3");

        assertTrue(five.isOlderThan(fifteen),
                "2.0.0.5 should be older than 2.0.0.15");
        assertFalse(five.isNewerThan(fifteen),
                "2.0.0.5 should be older than 2.0.0.15");

        assertTrue(fifteen.isNewerThan(five),
                "2.0.0.15 should be newer than 2.0.0.5");
        assertFalse(fifteen.isOlderThan(five),
                "2.0.0.15 should be newer than 2.0.0.5");

    }

    private void assertVersion(FrontendVersion version, int major, int minor,
            int revision, String build) {
        assertEquals(version.getMajorVersion(), major,
                "Major version was wrong for " + version.getFullVersion());
        assertEquals(version.getMinorVersion(), minor,
                "Minor version was wrong for " + version.getFullVersion());
        assertEquals(version.getRevision(), revision,
                "Revision was wrong for " + version.getFullVersion());
        assertEquals(version.getBuildIdentifier(), build,
                "Build identifier was wrong for " + version.getFullVersion());
    }
}
