package com.vaadin.flow.server.frontend;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FrontendVersionTest {

    @Test
    public void stringParser_returnsExpectedVersions() {
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
    public void versionConstructors_returnExpectedVersions() {
        FrontendVersion frontendVersion = new FrontendVersion(1, 2);
        assertVersion(frontendVersion, 1, 2, 0, "");

        frontendVersion = new FrontendVersion(1, 2, 5);
        assertVersion(frontendVersion, 1, 2, 5, "");

        frontendVersion = new FrontendVersion(1, 2, 5, "beta3");
        assertVersion(frontendVersion, 1, 2, 5, "beta3");
    }

    @Test
    public void testFrontedEquality() {
        FrontendVersion fromString = new FrontendVersion("1.1.0");
        FrontendVersion fromConstructor = new FrontendVersion(1, 1);

        assertTrue("Parsed string didn't equal constructor",
                fromString.equals(fromConstructor));
        assertTrue("Constructor didn't equal parsed string",
                fromConstructor.equals(fromString));

        fromString = new FrontendVersion("1.1.alpha12");
        fromConstructor = new FrontendVersion(1, 1, 0, "alpha12");
        assertTrue("Major-Minor version with build identifier didn't match",
                fromConstructor.equals(fromString));

        fromString = new FrontendVersion("12.3.5.alpha12");
        fromConstructor = new FrontendVersion(12, 3, 5, "alpha12");
        assertTrue("Full version with build identifier didn't match",
                fromString.equals(fromConstructor));
    }

    @Test
    public void testIsEqualTo() {
        FrontendVersion fromString = new FrontendVersion("1.1.0");
        FrontendVersion fromConstructor = new FrontendVersion(1, 1);

        assertTrue("Parsed string didn't equal constructor",
                fromString.isEqualTo(fromConstructor));
        assertTrue("Constructor didn't equal parsed string",
                fromConstructor.isEqualTo(fromString));

        fromString = new FrontendVersion("1.1.alpha12");
        fromConstructor = new FrontendVersion(1, 1, 0, "alpha12");
        assertTrue("Major-Minor version with build identifier didn't match",
                fromConstructor.isEqualTo(fromString));

        fromString = new FrontendVersion("12.3.5.alpha12");
        fromConstructor = new FrontendVersion(12, 3, 5, "alpha12");
        assertTrue("Full version with build identifier didn't match",
                fromString.isEqualTo(fromConstructor));
    }

    @Test(expected = NumberFormatException.class)
    public void faultyStringVersion_throwsException() {
        new FrontendVersion("12.0b.1");
    }

    @Test
    public void versionHandlesTildeAndCaretVersions() {
        assertVersion(new FrontendVersion("~1.3.0-beta1"), 1, 3, 0, "beta1");
        assertVersion(new FrontendVersion("~2.2"), 2, 2, 0, "");
        assertVersion(new FrontendVersion("^3.3.1"), 3, 3, 1, "");
    }

    @Test
    public void olderVersionIsCalculatedCorrectly() {
        FrontendVersion test = new FrontendVersion("2.2.0");

        assertTrue("Should be older due to revision",
                test.isOlderThan(new FrontendVersion("2.2.1")));
        assertTrue("Should be older due to minor",
                test.isOlderThan(new FrontendVersion("2.3.0")));
        assertTrue("Should be older due to major",
                test.isOlderThan(new FrontendVersion("3.2.0")));

        assertFalse("Should be newer as target has buildIdentifier",
                test.isOlderThan(new FrontendVersion("2.2.0-alpha1")));

        assertFalse("Should be newer due to major",
                test.isOlderThan(new FrontendVersion("1.2.0")));
        assertFalse("Should be newer due to minor",
                test.isOlderThan(new FrontendVersion("2.1.0")));
        assertFalse("Should be newer due to minor",
                test.isOlderThan(new FrontendVersion("2.1.2")));
        assertFalse("Should be newer due to minor",
                test.isOlderThan(new FrontendVersion("1.5.2")));

        assertFalse("Should be newer by major even with buildIdentifier",
                test.isOlderThan(new FrontendVersion("1.2.0-alpha1")));
    }

    @Test
    public void newerVersionIsCalculatedCorrectly() {
        FrontendVersion test = new FrontendVersion("2.2.2");

        assertTrue("Should be newer due to revision",
                test.isNewerThan(new FrontendVersion("2.2.1")));
        assertTrue("Should be newer due to minor",
                test.isNewerThan(new FrontendVersion("2.1.2")));
        assertTrue("Should be newer due to major",
                test.isNewerThan(new FrontendVersion("1.2.2")));

        assertTrue("Should be newer as target has buildIdentifier",
                test.isNewerThan(new FrontendVersion("2.2.2-alpha1")));

        assertFalse("Should be older due to major",
                test.isNewerThan(new FrontendVersion("3.2.2")));
        assertFalse("Should be older due to minor",
                test.isNewerThan(new FrontendVersion("2.3.2")));
    }

    @Test
    public void olderVersionByBuildIdentifier() {

        FrontendVersion test = new FrontendVersion("2.0.0-RC1");

        assertTrue("2.0.0 should be newer that RC1",
                test.isOlderThan(new FrontendVersion("2.0.0")));
        assertTrue("RC2 should be newer that RC1",
                test.isOlderThan(new FrontendVersion("2.0.0-RC2")));
        assertFalse("beta5 should be older than RC1",
                test.isOlderThan(new FrontendVersion("2.0.0-beta5")));
        assertFalse("alpha4 should be older than RC1",
                test.isOlderThan(new FrontendVersion("2.0.0-alpha4")));

        test = new FrontendVersion("2.0.0");

        assertFalse("RC2 should be older that 2.0.0",
                test.isOlderThan(new FrontendVersion("2.0.0-RC2")));
        assertFalse("beta5 should be older than 2.0.0",
                test.isOlderThan(new FrontendVersion("2.0.0-beta5")));
        assertFalse("alpha4 should be older than 2.0.0",
                test.isOlderThan(new FrontendVersion("2.0.0-alpha4")));
    }

    @Test
    public void newerVersionByBuildIdentifier() {

        FrontendVersion test = new FrontendVersion("2.0.0-alpha2");

        assertTrue("alpha2 should be newer than alpha1",
                test.isNewerThan(new FrontendVersion("2.0.0-alpha1")));
        assertFalse("alpha2 should be older than 2.0.0",
                test.isNewerThan(new FrontendVersion("2.0.0")));
        assertFalse("alpha2 should be older than beta1",
                test.isNewerThan(new FrontendVersion("2.0.0-beta1")));
        assertFalse("alpha2 should be older than RC1",
                test.isNewerThan(new FrontendVersion("2.0.0-RC1")));

        test = new FrontendVersion("2.0.0");

        assertTrue("2.0.0 should be newer than alpha1",
                test.isNewerThan(new FrontendVersion("2.0.0-alpha1")));
        assertTrue("2.0.0 should be older than beta1",
                test.isNewerThan(new FrontendVersion("2.0.0-beta1")));
        assertTrue("2.0.0 should be older than RC1",
                test.isNewerThan(new FrontendVersion("2.0.0-RC1")));
    }

    private void assertVersion(FrontendVersion version, int major, int minor,
            int revision, String build) {
        Assert.assertEquals(
                "Major version was wrong for " + version.getFullVersion(),
                version.getMajorVersion(), major);
        Assert.assertEquals(
                "Minor version was wrong for " + version.getFullVersion(),
                version.getMinorVersion(), minor);
        Assert.assertEquals(
                "Revision was wrong for " + version.getFullVersion(),
                version.getRevision(), revision);
        Assert.assertEquals(
                "Build identifier was wrong for " + version.getFullVersion(),
                version.getBuildIdentifier(), build);
    }
}
