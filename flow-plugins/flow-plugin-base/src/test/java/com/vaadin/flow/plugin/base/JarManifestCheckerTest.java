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
package com.vaadin.flow.plugin.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link JarManifestChecker}.
 */
public class JarManifestCheckerTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Before
    public void setup() {
        // Clear cache before each test
        JarManifestChecker.clearCache();
    }

    @After
    public void tearDown() {
        // Clear cache after each test
        JarManifestChecker.clearCache();
    }

    @Test
    public void hasVaadinManifest_jarWithVaadinPackageVersion_returnsTrue()
            throws IOException {
        File jarFile = createJarWithManifest("1");

        boolean result = JarManifestChecker.hasVaadinManifest(jarFile);

        Assert.assertTrue(
                "JAR with Vaadin-Package-Version should return true", result);
    }

    @Test
    public void hasVaadinManifest_jarWithDifferentVersion_returnsTrue()
            throws IOException {
        File jarFile = createJarWithManifest("2");

        boolean result = JarManifestChecker.hasVaadinManifest(jarFile);

        Assert.assertTrue(
                "JAR with Vaadin-Package-Version=2 should return true",
                result);
    }

    @Test
    public void hasVaadinManifest_jarWithoutManifest_returnsFalse()
            throws IOException {
        File jarFile = createJarWithoutManifest();

        boolean result = JarManifestChecker.hasVaadinManifest(jarFile);

        Assert.assertFalse("JAR without manifest should return false", result);
    }

    @Test
    public void hasVaadinManifest_jarWithManifestButNoVaadinAttribute_returnsFalse()
            throws IOException {
        File jarFile = createJarWithManifestWithoutVaadinAttribute();

        boolean result = JarManifestChecker.hasVaadinManifest(jarFile);

        Assert.assertFalse(
                "JAR without Vaadin-Package-Version attribute should return false",
                result);
    }

    @Test
    public void hasVaadinManifest_nullFile_returnsFalse() {
        boolean result = JarManifestChecker.hasVaadinManifest(null);

        Assert.assertFalse("Null file should return false", result);
    }

    @Test
    public void hasVaadinManifest_nonExistentFile_returnsFalse() {
        File nonExistent = new File(tmpDir.getRoot(), "nonexistent.jar");

        boolean result = JarManifestChecker.hasVaadinManifest(nonExistent);

        Assert.assertFalse("Non-existent file should return false", result);
    }

    @Test
    public void hasVaadinManifest_directory_returnsFalse() throws IOException {
        File directory = tmpDir.newFolder("test-directory");

        boolean result = JarManifestChecker.hasVaadinManifest(directory);

        Assert.assertFalse("Directory should return false", result);
    }

    @Test
    public void hasVaadinManifest_cachedResult_doesNotReadTwice()
            throws IOException {
        File jarFile = createJarWithManifest("1");

        // First call - reads manifest
        boolean result1 = JarManifestChecker.hasVaadinManifest(jarFile);
        // Second call - should use cache
        boolean result2 = JarManifestChecker.hasVaadinManifest(jarFile);

        Assert.assertTrue("First call should return true", result1);
        Assert.assertTrue("Second call should return true from cache",
                result2);

        // Verify cache size
        Assert.assertEquals("Cache should contain one entry", 1,
                JarManifestChecker.getCacheSize());
    }

    @Test
    public void clearCache_removesAllCachedEntries() throws IOException {
        File jarFile1 = createJarWithManifest("1");
        File jarFile2 = createJarWithManifest("2");

        // Populate cache
        JarManifestChecker.hasVaadinManifest(jarFile1);
        JarManifestChecker.hasVaadinManifest(jarFile2);

        Assert.assertEquals("Cache should contain two entries", 2,
                JarManifestChecker.getCacheSize());

        JarManifestChecker.clearCache();

        Assert.assertEquals("Cache should be empty after clear", 0,
                JarManifestChecker.getCacheSize());
    }

    @Test
    public void hasVaadinManifest_multipleDifferentJars_cachesEachSeparately()
            throws IOException {
        File jarWithVaadin = createJarWithManifest("1");
        File jarWithoutVaadin = createJarWithoutManifest();

        boolean resultWith = JarManifestChecker
                .hasVaadinManifest(jarWithVaadin);
        boolean resultWithout = JarManifestChecker
                .hasVaadinManifest(jarWithoutVaadin);

        Assert.assertTrue("JAR with Vaadin manifest should return true",
                resultWith);
        Assert.assertFalse("JAR without Vaadin manifest should return false",
                resultWithout);

        Assert.assertEquals("Cache should contain two entries", 2,
                JarManifestChecker.getCacheSize());
    }

    /**
     * Creates a test JAR file with a manifest containing the
     * Vaadin-Package-Version attribute.
     */
    private File createJarWithManifest(String version) throws IOException {
        File jarFile = tmpDir.newFile("test-with-vaadin-" + version + ".jar");

        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue(JarManifestChecker.VAADIN_PACKAGE_VERSION, version);

        try (FileOutputStream fos = new FileOutputStream(jarFile);
                JarOutputStream jos = new JarOutputStream(fos, manifest)) {
            // Add a dummy class file to make it a valid JAR
            ZipEntry entry = new ZipEntry("com/example/Test.class");
            jos.putNextEntry(entry);
            jos.write(new byte[] { 0, 0, 0, 0 });
            jos.closeEntry();
        }

        return jarFile;
    }

    /**
     * Creates a test JAR file without a manifest.
     */
    private File createJarWithoutManifest() throws IOException {
        File jarFile = tmpDir.newFile("test-without-manifest.jar");

        try (FileOutputStream fos = new FileOutputStream(jarFile);
                JarOutputStream jos = new JarOutputStream(fos)) {
            // Add a dummy class file
            ZipEntry entry = new ZipEntry("com/example/Test.class");
            jos.putNextEntry(entry);
            jos.write(new byte[] { 0, 0, 0, 0 });
            jos.closeEntry();
        }

        return jarFile;
    }

    /**
     * Creates a test JAR file with a manifest but without the
     * Vaadin-Package-Version attribute.
     */
    private File createJarWithManifestWithoutVaadinAttribute()
            throws IOException {
        File jarFile = tmpDir
                .newFile("test-manifest-no-vaadin-attribute.jar");

        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("Implementation-Version", "1.0.0");
        attrs.putValue("Created-By", "Test");

        try (FileOutputStream fos = new FileOutputStream(jarFile);
                JarOutputStream jos = new JarOutputStream(fos, manifest)) {
            // Add a dummy class file
            ZipEntry entry = new ZipEntry("com/example/Test.class");
            jos.putNextEntry(entry);
            jos.write(new byte[] { 0, 0, 0, 0 });
            jos.closeEntry();
        }

        return jarFile;
    }
}
