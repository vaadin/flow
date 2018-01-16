package com.vaadin.flow.plugin.production;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.plugin.common.WebJarData;

import static org.junit.Assert.assertEquals;

/**
 * @author Vaadin Ltd.
 */
public class WebJarPackageTest {
    @Rule
    public TemporaryFolder testDirectory = new TemporaryFolder();

    private File webJarFile;

    @Test(expected = IllegalArgumentException.class)
    public void selectCorrectPackage_differentNames() {
        String version = "1.0.2";
        WebJarPackage.selectCorrectPackage(createPackage("one", version), createPackage("two", version));
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectCorrectPackage_differentVersions() {
        String packageName = "vaaadin-test";
        WebJarPackage.selectCorrectPackage(createPackage(packageName, "22222"), createPackage(packageName, "111"));
    }

    @Test
    public void selectCorrectPackage_sameVersionsOnePrefixed() {
        String packageName = "vaaadin-test";
        String version = "1.0.3";
        String prefixedVersion = 'v' + version;
        WebJarPackage packageWithoutPrefixedVersion = createPackage(packageName, version);

        WebJarPackage merged = WebJarPackage.selectCorrectPackage(packageWithoutPrefixedVersion, createPackage(packageName, prefixedVersion));

        assertEquals("Expected to have version without prefix after merge",
                merged.getPackageName(), packageWithoutPrefixedVersion.getPackageName());
        assertEquals("Got different package name after merge",
                merged.getPathToPackage(), packageWithoutPrefixedVersion.getPathToPackage());
        assertEquals("Got different WebJar after merge",
                merged.getWebJar(), packageWithoutPrefixedVersion.getWebJar());
    }

    private WebJarPackage createPackage(String name, String version) {
        if (webJarFile == null) {
            try {
                webJarFile = testDirectory.newFile("testWebJarFile");
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create test web jar file", e);
            }
        }
        return new WebJarPackage(new WebJarData(webJarFile, "artifactId", version), name, "path");
    }
}
