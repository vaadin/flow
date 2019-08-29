/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class JarContentsManagerTest {
    @Rule
    public TemporaryFolder testDirectory = new TemporaryFolder();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private final JarContentsManager jarContentsManager = new JarContentsManager();
    private final File testJar = TestUtils.getTestJar();

    @Test
    public void getFileContents_directoryInsteadOfJar() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(String.format(
                "Expect '%s' to be an existing file", testDirectory.getRoot()));

        jarContentsManager.getFileContents(testDirectory.getRoot(), "test");
    }

    @Test
    public void getFileContents_notAJarFile() throws IOException {
        File testFile = testDirectory.newFile("test");

        expectedException.expect(UncheckedIOException.class);
        expectedException.expectMessage(
                String.format("Failed to retrieve file '%s' from jar '%s'",
                        "test", testFile));

        jarContentsManager.getFileContents(testFile, "test");
    }

    @Test
    public void getFileContents_nonExistingJarFile() {
        File test = new File("test");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
                String.format("Expect '%s' to be an existing file", test));

        jarContentsManager.getFileContents(test, "test");
    }

    @Test
    public void getFileContents_nonExistingFile() {
        byte[] fileContents = jarContentsManager.getFileContents(testJar,
                "blah");

        assertNull("Expect to have non-empty file from jar", fileContents);
    }

    @Test
    public void getFileContents_existingFile() {
        byte[] fileContents = jarContentsManager.getFileContents(testJar,
                "META-INF/resources/webjars/paper-button/2.0.0/bower.json");

        assertNotNull("Expect to have non-empty file from jar", fileContents);
        assertTrue("Expect to have non-empty file from jar",
                fileContents.length > 0);
    }

    @Test
    public void containsPath_directoryInsteadOfJar() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(String.format(
                "Expect '%s' to be an existing file", testDirectory.getRoot()));

        jarContentsManager.containsPath(testDirectory.getRoot(), "test");
    }

    @Test
    public void containsPath_notAJarFile() throws IOException {
        File testFile = testDirectory.newFile("test");

        expectedException.expect(UncheckedIOException.class);
        expectedException.expectMessage(
                String.format("Failed to retrieve file '%s' from jar '%s'",
                        "test", testFile));

        jarContentsManager.containsPath(testFile, "test");
    }

    @Test
    public void containsPath_nonExistingJarFile() {
        File test = new File("test");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
                String.format("Expect '%s' to be an existing file", test));

        jarContentsManager.containsPath(test, "test");
    }

    @Test
    public void containsPath_nonExistingPath() {
        String nonExistingPath = "should not exist";

        assertFalse(
                String.format("Test jar '%s' should not contain path '%s'",
                        testJar, nonExistingPath),
                jarContentsManager.containsPath(testJar, nonExistingPath));
    }

    @Test
    public void containsPath_existingFile() {
        String existingPath = "META-INF/resources/webjars/";

        assertTrue(
                String.format("Test jar '%s' should contain path '%s'", testJar,
                        existingPath),
                jarContentsManager.containsPath(testJar, existingPath));
    }

    /*
     * Test for issue: flow fails to serve static resources from latest webjars
     * #6241 https://github.com/vaadin/flow/issues/6241
     */
    @Test
    public void containsPath_missingDirectoryStructure_scansForMatch() {
        String existingPathLocal = "META-INF/resources/webjars/";
        File testJarLocal = TestUtils.getTestJar("test-jar-issue-6241.jar");

        assertTrue(
                String.format("Test jar '%s' should contain path '%s'", testJar,
                        existingPathLocal),
                jarContentsManager.containsPath(testJarLocal,
                        existingPathLocal));
    }

    @Test
    public void findFiles_directoryInsteadOfJar() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(String.format(
                "Expect '%s' to be an existing file", testDirectory.getRoot()));

        jarContentsManager.findFiles(testDirectory.getRoot(), "test", "test");
    }

    @Test
    public void findFiles_notAJarFile() throws IOException {
        File testFile = testDirectory.newFile("test");

        expectedException.expect(UncheckedIOException.class);
        expectedException
                .expectMessage("java.util.zip.ZipException: zip file is empty");

        jarContentsManager.findFiles(testFile, "test", "test");
    }

    @Test
    public void findFiles_nonExistingJarFile() {
        File test = new File("test");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
                String.format("Expect '%s' to be an existing file", test));

        jarContentsManager.findFiles(test, "test", "test");
    }

    @Test
    public void findFiles_nonExistingFile() {
        List<String> result = jarContentsManager.findFiles(testJar, "blah",
                "nope");

        assertTrue("Expect to have empty results for non-existing file",
                result.isEmpty());
    }

    @Test
    public void findFiles_existingFiles() {
        String resourceName = "vaadin-charts-webjar-6.0.0-alpha3.jar";
        String searchName = "bower.json";

        List<String> bowerJsons = jarContentsManager
                .findFiles(TestUtils.getTestJar(resourceName), "", searchName);

        assertEquals(
                String.format("Expect '%s' WebJar to contain two '%s' files",
                        resourceName, searchName),
                2, bowerJsons.size());
        assertTrue(String.format(
                "Expect all found paths to end with the file name searched for: '%s'",
                searchName),
                bowerJsons.stream()
                        .allMatch(path -> path.endsWith('/' + searchName)));
    }

    @Test
    public void findFiles_existingFiles_baseDirectoryMatters() {
        String resourceName = "vaadin-charts-webjar-6.0.0-alpha3.jar";
        String testPath = "META-INF/resources/webjars/highcharts/5.0.14/";
        String searchName = "bower.json";

        List<String> bowerJson = jarContentsManager.findFiles(
                TestUtils.getTestJar(resourceName), testPath, searchName);

        assertEquals(String.format(
                "Expect '%s' WebJar to contain one '%s' file in directory '%s'",
                resourceName, searchName, testPath), 1, bowerJson.size());
    }

    @Test
    public void copyFilesFromJar_nullJarFile() {
        expectedException.expect(NullPointerException.class);

        jarContentsManager.copyFilesFromJarTrimmingBasePath(null, null,
                testDirectory.getRoot());
    }

    @Test
    public void copyFilesFromJar_notAJarFile() throws IOException {
        File testFile = testDirectory.newFile("test");

        expectedException.expect(UncheckedIOException.class);
        expectedException.expectMessage(String.format(
                "Failed to extract files from jarFile '%s' to directory '%s'",
                testFile, testDirectory.getRoot()));

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testFile, null,
                testDirectory.getRoot());
    }

    @Test
    public void copyFilesFromJar_nonExistingJarFile() {
        File test = new File("test");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
                String.format("Expect '%s' to be an existing file", test));

        jarContentsManager.copyFilesFromJarTrimmingBasePath(test, null,
                testDirectory.getRoot());
    }

    @Test
    public void copyFilesFromJar_directoryInsteadOfJar() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(String.format(
                "Expect '%s' to be an existing file", testDirectory.getRoot()));

        jarContentsManager.copyFilesFromJarTrimmingBasePath(
                testDirectory.getRoot(), null, testDirectory.getRoot());
    }

    @Test
    public void copyFilesFromJar_nullOutputDirectory() {
        expectedException.expect(NullPointerException.class);

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, null,
                null);
    }

    @Test
    public void copyFilesFromJar_fileInsteadOfDirectory() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(String
                .format("Expect '%s' to be an existing directory", testJar));

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, null,
                testJar);
    }

    @Test
    public void copyFilesFromJar_noBasePath_noExclusions() {
        assertThat(
                "Do not expect any files in temporary directory before the test",
                TestUtils.listFilesRecursively(testDirectory.getRoot()).size(),
                is(0));

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, null,
                testDirectory.getRoot());

        assertThat("Temporary directory should have files after jar copied",
                TestUtils.listFilesRecursively(testDirectory.getRoot()).size(),
                is(not(0)));
    }

    @Test
    public void copyFilesFromJar_noBasePath_excludeEverything() {
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, null,
                testDirectory.getRoot(), "*");
        assertThat(
                "Do not expect any files with filter that excludes everything",
                TestUtils.listFilesRecursively(testDirectory.getRoot()).size(),
                is(0));
    }

    @Test
    public void copyFilesFromJar_withBasePath_noExclusions()
            throws IOException {
        String basePath = "META-INF/maven/";
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath,
                testDirectory.getRoot());

        List<String> resultingPaths = TestUtils
                .listFilesRecursively(testDirectory.getRoot());
        assertThat(String.format(
                "Expect jar '%s' to contain files with base path '%s'", testJar,
                basePath), resultingPaths.size(), is(not(0)));
        assertTrue("Resulting paths should not contain base path = " + basePath,
                resultingPaths.stream()
                        .noneMatch(path -> path.contains(basePath)));
    }

    @Test
    public void copyFilesFromJar_exclusionsWork() throws IOException {
        String basePath = "META-INF/maven";

        File notFilteredCopyingDirectory = testDirectory
                .newFolder("notFiltered");
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath,
                notFilteredCopyingDirectory);
        List<String> notFilteredPaths = TestUtils
                .listFilesRecursively(notFilteredCopyingDirectory);

        File filteredCopyingDirectory = testDirectory.newFolder("filtered");
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath,
                filteredCopyingDirectory, "*.xml");
        List<String> filteredPaths = TestUtils
                .listFilesRecursively(filteredCopyingDirectory);

        assertTrue(
                "Filtered paths' count should be less than non filtered paths' count",
                filteredPaths.size() < notFilteredPaths.size());
        assertTrue("Not filtered paths should contain xml files",
                notFilteredPaths.stream()
                        .anyMatch(path -> path.endsWith(".xml")));
        assertTrue("Paths with '*.xml' exclusion should not contain xml files",
                filteredPaths.stream()
                        .noneMatch(path -> path.endsWith(".xml")));
    }

    @Test
    public void copyFilesFromJar_basePathAppendedWithTrailingSlash()
            throws IOException {
        String basePath1 = "META-INF/maven";
        File basePath1Directory = testDirectory.newFolder("basePath1");
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath1,
                basePath1Directory);
        List<String> basePath1Paths = TestUtils
                .listFilesRecursively(basePath1Directory);

        String basePath2 = basePath1 + '/';
        File basePath2Directory = testDirectory.newFolder("basePath2");
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath2,
                basePath2Directory);
        List<String> basePath2Paths = TestUtils
                .listFilesRecursively(basePath2Directory);

        assertEquals(
                "Base path without trailing slash should be treated the same as base path with one",
                basePath1Paths, basePath2Paths);
    }

    @Test
    public void copyFilesFromJar_copiedFromBasePathResultsAreContainedInAllPaths()
            throws IOException {
        File allFilesDirectory = testDirectory.newFolder("all");
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, null,
                allFilesDirectory);
        List<String> allPaths = TestUtils
                .listFilesRecursively(allFilesDirectory);

        String basePath = "/META-INF/maven";
        File filteredFilesDirectory = testDirectory.newFolder("filtered");
        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar, basePath,
                filteredFilesDirectory);
        List<String> filteredPaths = TestUtils
                .listFilesRecursively(filteredFilesDirectory);
        List<String> filteredPathsPrefixedByBasePath = filteredPaths.stream()
                .map(path -> basePath + path).collect(Collectors.toList());

        assertTrue("Filtered paths' count should be less than all paths' count",
                filteredPaths.size() < allPaths.size());
        assertTrue("base path + filtered path should be contained in all paths",
                allPaths.containsAll(filteredPathsPrefixedByBasePath));
    }

    @Test
    public void copyFilesFromJar_casePreserved() {
        File outputDirectory = testDirectory.getRoot();
        String jarDirectory = "META-INF/resources/webjars/paper-button/2.0.0/.github/";
        File testJar = TestUtils.getTestJar("paper-button-2.0.0.jar");
        List<String> originalFiles = listFilesInJar(testJar, jarDirectory);

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar,
                jarDirectory, outputDirectory);

        Set<String> copiedFiles = new HashSet<>(
                TestUtils.listFilesRecursively(outputDirectory));

        assertEquals(String.format(
                "Number of files in jar '%s' in jar directory '%s' and number of copied files should match.",
                testJar, jarDirectory), originalFiles.size(),
                copiedFiles.size());

        copiedFiles.forEach(copiedFile -> assertTrue(String.format(
                "Failed to find copied file '%s' in files '%s' from jar '%s'",
                copiedFile, originalFiles, testJar),
                originalFiles.stream()
                        .anyMatch(file -> file.endsWith(copiedFile))));
    }

    @Test
    public void copyFilesFromJar_doNotUpdateFileIfContentIsTheSame() {
        File outputDirectory = testDirectory.getRoot();
        String jarDirectory = "META-INF/resources/webjars/paper-button";
        File testJar = TestUtils.getTestJar("paper-button-2.0.0.jar");
        File jsonFile = copyFilesFromJar(outputDirectory, jarDirectory,
                testJar);

        long timestamp = System.currentTimeMillis();
        Assert.assertTrue(FileUtils.isFileOlder(jsonFile, timestamp));

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar,
                jarDirectory, outputDirectory);

        // The file is still older
        Assert.assertTrue(FileUtils.isFileOlder(jsonFile, timestamp));
    }

    @Test
    public void copyFilesFromJar_updateFileIfContentIsNotTheSame()
            throws IOException {
        File outputDirectory = testDirectory.getRoot();
        String jarDirectory = "META-INF/resources/webjars/paper-button";
        File testJar = TestUtils.getTestJar("paper-button-2.0.0.jar");
        File jsonFile = copyFilesFromJar(outputDirectory, jarDirectory,
                testJar);

        String originalContent = FileUtils
                .readLines(jsonFile, StandardCharsets.UTF_8).stream()
                .collect(Collectors.joining(""));

        String content = "{}";
        FileUtils.write(jsonFile, content, StandardCharsets.UTF_8);

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar,
                jarDirectory, outputDirectory);

        Assert.assertNotEquals(content,
                FileUtils.readLines(jsonFile, StandardCharsets.UTF_8).stream()
                        .collect(Collectors.joining("")));
        Assert.assertEquals(originalContent,
                FileUtils.readLines(jsonFile, StandardCharsets.UTF_8).stream()
                        .collect(Collectors.joining("")));
    }

    private File copyFilesFromJar(File outputDirectory, String jarDirectory,
            File testJar) {
        List<String> originalFiles = listFilesInJar(testJar, jarDirectory);

        Optional<String> json = originalFiles.stream()
                .filter(fileName -> fileName.endsWith(".json")).findFirst();

        // self check
        assert json.isPresent();

        String jsonPath = json.get();
        jsonPath = jsonPath.substring(jarDirectory.length() + 1);

        jarContentsManager.copyFilesFromJarTrimmingBasePath(testJar,
                jarDirectory, outputDirectory);

        File jsonFile = new File(outputDirectory, jsonPath);

        // self check
        assert jsonFile.exists();
        return jsonFile;
    }

    private List<String> listFilesInJar(File jar, String jarDirectory) {
        try (JarFile jarFile = new JarFile(jar, false)) {
            return jarFile.stream().filter(file -> !file.isDirectory())
                    .filter(file -> file.getName().startsWith(jarDirectory))
                    .map(ZipEntry::getName).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Failed to list files in jarFile '%s'", jar),
                    e);
        }
    }
}
