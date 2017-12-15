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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.stream.Stream;

import org.junit.Test;

import com.vaadin.flow.server.webjar.SemanticVersion;

/**
 * @author Vaadin Ltd.
 */
public class SemanticVersionTest {

    @Test(expected = NullPointerException.class)
    public void nullStringsAreNotAllowed() {
        new SemanticVersion(null);
    }

    @Test
    public void nonSemverStringShouldThrowAnException() {
        Stream.of("a.0.1", "1.a.1", "1.1", "2", "1.0.aaa")
                .forEach(this::parseIncorrectVersionString);
    }

    @Test
    public void semverStringShouldBeParsedCorrectly() {
        Stream.of("1.0.1", "1.2.1-alpha3", "1.0.0-x.7.z.92", "1.0.0-0.3.7")
                .forEach(this::parseCorrectVersionString);
    }

    @Test
    public void webjarVersioningIssueShouldBeParsed() {
        parseCorrectVersionString("v1.0.1");
    }

    private void parseCorrectVersionString(String versionString) {
        parseVersionString(versionString, true);
    }

    private void parseIncorrectVersionString(String versionString) {
        parseVersionString(versionString, false);
    }

    private void parseVersionString(String versionString,
            boolean shouldBeParsedCorrectly) {
        try {
            new SemanticVersion(versionString);
            if (!shouldBeParsedCorrectly) {
                fail(String.format(
                        "Expected 'SemanticVersion' class to throw an exception when parsing incorrect version string '%s'",
                        versionString));
            }
        } catch (IllegalArgumentException e) {
            if (shouldBeParsedCorrectly) {
                fail(String.format(
                        "Expected 'SemanticVersion' class to correctly parse version string '%s'",
                        versionString));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void versionsWithDifferentMajorVersionPartsAreIncompatible() {
        new SemanticVersion("1.2.3")
                .comparePatchParts(new SemanticVersion("2.2.3"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void versionsWithDifferentMinorVersionPartsAreIncompatible() {
        new SemanticVersion("1.2.3")
                .comparePatchParts(new SemanticVersion("1.1.3"));
    }

    @Test
    public void comparePatchVersions_equal() {
        String versionString = "1.2.3";
        assertThat(
                "Semantic versions parsed from the same string should be equal",
                new SemanticVersion(versionString).comparePatchParts(
                        new SemanticVersion(versionString)),
                is(0));
    }

    @Test
    public void comparePatchVersions_equalWithPrefixes() {
        String versionString1 = "1.2.3";
        String versionString2 = 'v' + versionString1;
        assertThat(
                "Semantic versions parsed from the same string should be equal",
                new SemanticVersion(versionString1).comparePatchParts(
                        new SemanticVersion(versionString2)),
                is(0));
    }

    @Test
    public void comparePatchVersions_biggerNumberWins() {
        compareNotEqualVersions("1.2.4", "1.2.3");
    }

    @Test
    public void comparePatchVersions_biggerNumberWins2() {
        compareNotEqualVersions("1.2.3-alpha8", "1.2.3-alpha1");
    }

    @Test
    public void comparePatchVersions_letterWins() {
        compareNotEqualVersions("1.0.0-alpha.beta", "1.0.0-alpha.1");
    }

    @Test
    public void comparePatchVersions_alphabeticOrderWins() {
        compareNotEqualVersions("1.0.0-beta", "1.0.0-alpha.beta");
    }

    @Test
    public void comparePatchVersions_stableVersionWins() {
        compareNotEqualVersions("1.2.3", "1.2.3-alpha8");
    }

    @Test
    public void comparePatchVersions_biggerNumberWins_evenIfOccurredNotAtTheEnd() {
        compareNotEqualVersions("1.2.3-g2", "1.2.3-g1aaaaaaaaaaaaaaaaa");
    }

    @Test
    public void comparePatchVersions_longerPreReleaseWins_1() {
        compareNotEqualVersions("1.2.3-gggg", "1.2.3-gg");
    }

    @Test
    public void comparePatchVersions_longerPreReleaseWins_2() {
        compareNotEqualVersions("1.0.0-alpha1", "1.0.0-alpha");
    }

    private void compareNotEqualVersions(String biggerVersion, String smallerVersion) {
        String errorMessage = String.format(
                "Semantic version '%s' is expected to be bigger than '%s'",
                biggerVersion, smallerVersion);
        SemanticVersion bigger = new SemanticVersion(biggerVersion);
        SemanticVersion smaller = new SemanticVersion(smallerVersion);

        assertThat(errorMessage, bigger.comparePatchParts(smaller),
                greaterThan(0));
        assertThat(errorMessage, smaller.comparePatchParts(bigger),
                lessThan(0));
    }
}
