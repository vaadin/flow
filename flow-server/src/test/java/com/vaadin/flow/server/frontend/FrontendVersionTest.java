package com.vaadin.flow.server.frontend;

import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertTrue("Parsed string didn't equal constructor",
                fromString.equals(fromConstructor));
        Assert.assertTrue("Constructor didn't equal parsed string",
                fromConstructor.equals(fromString));

        fromString = new FrontendVersion("1.1.alpha12");
        fromConstructor = new FrontendVersion(1, 1, 0, "alpha12");
        Assert.assertTrue(
                "Major-Minor version with build identifier didn't match",
                fromConstructor.equals(fromString));

        fromString = new FrontendVersion("12.3.5.alpha12");
        fromConstructor = new FrontendVersion(12, 3, 5, "alpha12");
        Assert.assertTrue("Full version with build identifier didn't match",
                fromString.equals(fromConstructor));
    }

    @Test(expected = NumberFormatException.class)
    public void faultyStringVersion_throwsException() {
        new FrontendVersion("12.0b.1");
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
