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

package com.vaadin.flow.plugin.production;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.plugin.common.ArtifactData;
import com.vaadin.flow.server.frontend.JarContentsManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ProductionModeCopyStepTest {
    private ArtifactData getWebJarData(String version, String artifactId) {
        return new ArtifactData(TestUtils.getTestJar(String.format("%s-%s.jar", artifactId, version)), artifactId, version);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder testDirectory = new TemporaryFolder();

    @Test
    public void webJarsWithDifferentVersions_fail() {
        File testJar = TestUtils.getTestJar();
        String version1 = "2.0.0";
        String version2 = "2.0.1";
        String artifactId = "paper-button";

        JarContentsManager noBowerJsonManager = mock(JarContentsManager.class);
        when(noBowerJsonManager.findFiles(any(File.class), anyString(), anyString())).thenReturn(Collections.singletonList("test"));
        when(noBowerJsonManager.getFileContents(any(File.class), any())).thenReturn(String.format("{'name' : '%s'}", artifactId).getBytes(StandardCharsets.UTF_8));

        new ProductionModeCopyStep(noBowerJsonManager, Arrays.asList(new ArtifactData(testJar, artifactId, version1), new ArtifactData(testJar, artifactId, version2)));
    }

    /**
     * WebJars' issues with incorrect prefixes should be treated normally.
     *
     * @see <a href="https://github.com/webjars/webjars/issues/1656">https://github.com/webjars/webjars/issues/1656</a>
     */
    @Test
    public void webJarsWithSameBowerNamesAndDifferentArtifactIds_work() {
        String version = "2.0.0";
        String artifactId = "paper-button";

        String prefixedArtifactId = "github-com-polymerelements-" + artifactId;

        new ProductionModeCopyStep(Arrays.asList(getWebJarData(version, artifactId), getWebJarData(version, prefixedArtifactId)));
    }

    @Test
    public void jarWithJSNoInclusions_nothingGetsCopied() {
        File outputDirectory = testDirectory.getRoot();
        assertTrue(
                "No files should be in output directory before the beginning",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());

        new ProductionModeCopyStep(Collections
                .singleton(getWebJarData("10.0.0-alpha1", "TimeSelector")))
                .copyFrontendJavaScriptFiles(outputDirectory, null,
                        "META-INF/frontend");

        List<String> resultingFiles = TestUtils
                .listFilesRecursively(outputDirectory);
        assertTrue("No files should have been copied from the test Jar",
                resultingFiles.isEmpty());
    }

    @Test
    public void jarWithJS_copiedCorrectly() {
        File outputDirectory = testDirectory.getRoot();
        assertTrue(
                "No files should be in output directory before the beginning",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());

        new ProductionModeCopyStep(Collections
                .singleton(getWebJarData("10.0.0-alpha1", "TimeSelector")))
                .copyFrontendJavaScriptFiles(outputDirectory, "**/*.js",
                        "META-INF/frontend");

        List<String> resultingFiles = TestUtils
                .listFilesRecursively(outputDirectory);
        assertFalse("JavaScript files should be copied from the test Jar",
                resultingFiles.isEmpty());
        assertEquals("Jar with multiple js files gets all files copied", 4,
                resultingFiles.stream().filter(path -> path.endsWith(".js"))
                        .count());

        assertTrue("Missing 'test/test.js'", resultingFiles.stream()
                .filter(path -> path
                        .endsWith("test" + File.separator + "test.js"))
                .findFirst().isPresent());
        assertTrue("Missing 'CircleSelector.js'", resultingFiles.stream()
                .filter(path -> path.endsWith("CircleSelector.js")).findFirst()
                .isPresent());
        assertTrue("Missing 'PopupSelector.js'", resultingFiles.stream()
                .filter(path -> path.endsWith("PopupSelector.js")).findFirst()
                .isPresent());
        assertTrue("Missing 'TimeSelector.js'", resultingFiles.stream()
                .filter(path -> path.endsWith("TimeSelector.js")).findFirst()
                .isPresent());
    }

    @Test
    public void jarWithJsAndHtml_copiedCorrectly() {
        File outputDirectory = testDirectory.getRoot();
        assertTrue(
                "No files should be in output directory before the beginning",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());

        new ProductionModeCopyStep(Collections
                .singleton(getWebJarData("10.0.0-alpha1", "TimeSelector")))
                .copyFrontendJavaScriptFiles(outputDirectory,
                        "**/*.js,**/*.html", "META-INF");

        List<String> resultingFiles = TestUtils
                .listFilesRecursively(outputDirectory);
        assertFalse("JavaScript files should be copied from the test Jar",
                resultingFiles.isEmpty());

        assertEquals("All files matching inclusions should be copied", 7,
                resultingFiles.size());

        assertEquals("Jar with multiple js files gets all files copied", 4,
                resultingFiles.stream().filter(path -> path.endsWith(".js"))
                        .count());
        assertEquals("Jar with multiple html files gets all files copied", 3,
                resultingFiles.stream().filter(path -> path.endsWith(".html"))
                        .count());

        assertTrue("Missing 'frontend/test/test.js'", resultingFiles.stream()
                .filter(path -> path.endsWith(
                        "frontend" + File.separator + "test" + File.separator
                                + "test.js")).findFirst().isPresent());
        assertTrue("Missing 'frontend/CircleSelector.js'",
                resultingFiles.stream().filter(path -> path.endsWith(
                        "frontend" + File.separator + "CircleSelector.js"))
                        .findFirst().isPresent());
        assertTrue("Missing 'frontend/PopupSelector.js'",
                resultingFiles.stream().filter(path -> path.endsWith(
                        "frontend" + File.separator + "PopupSelector.js"))
                        .findFirst().isPresent());
        assertTrue("Missing 'frontend/TimeSelector.js'", resultingFiles.stream()
                .filter(path -> path.endsWith(
                        "frontend" + File.separator + "TimeSelector.js"))
                .findFirst().isPresent());

        assertTrue("Missing 'resources/frontend/CircleSelector.html'",
                resultingFiles.stream().filter(path -> path.endsWith(
                        "resources" + File.separator + "frontend"
                                + File.separator + "CircleSelector.html"))
                        .findFirst().isPresent());
        assertTrue("Missing 'resources/frontend/PopupSelector.html'",
                resultingFiles.stream().filter(path -> path.endsWith(
                        "resources" + File.separator + "frontend"
                                + File.separator + "PopupSelector.html"))
                        .findFirst().isPresent());
        assertTrue("Missing 'resources/frontend/TimeSelector.html'",
                resultingFiles.stream().filter(path -> path.endsWith(
                        "resources" + File.separator + "frontend"
                                + File.separator + "TimeSelector.html"))
                        .findFirst().isPresent());
    }

    @Test
    public void webJarsWithMultiplePackages_work() {
        File outputDirectory = testDirectory.getRoot();
        assertTrue("No files should be in output directory before the beginning", TestUtils.listFilesRecursively(outputDirectory).isEmpty());

        new ProductionModeCopyStep(Collections.singleton(
                getWebJarData("6.0.0-alpha3", "vaadin-charts-webjar")))
                        .copyWebApplicationFiles(outputDirectory, null, null);

        List<String> resultingFiles = TestUtils.listFilesRecursively(outputDirectory);
        assertFalse("Files should be copied from the test WebJar", resultingFiles.isEmpty());
        assertEquals("WebJar with multiple bower.json are handled correctly and copied",
                2, resultingFiles.stream().filter(path -> path.endsWith(File.separator + "bower.json")).count());
    }

    /*
    Test for issue:
        flow fails to serve static resources from latest webjars #6241
        https://github.com/vaadin/flow/issues/6241
     */
    @Test
    public void webJarsWithMissingDirectoryListings_work() {
        File outputDirectory = testDirectory.getRoot();
        assertTrue("No files should be in output directory before the beginning", TestUtils.listFilesRecursively(outputDirectory).isEmpty());

        new ProductionModeCopyStep(Collections.singleton(
                getWebJarData("6241", "test-jar-issue")))
                .copyWebApplicationFiles(outputDirectory, null, null);

        List<String> resultingFiles =
                TestUtils.listFilesRecursively(outputDirectory);
        assertFalse("Files should be copied from the test WebJar", resultingFiles.isEmpty());
    }

    @Test
    public void copyWebApplicationFiles_fileInsteadOfOutputDirectory() throws IOException {
        File fileNotDirectory = testDirectory.newFile("test");

        exception.expect(UncheckedIOException.class);
        exception.expectMessage(fileNotDirectory.getAbsolutePath());

        new ProductionModeCopyStep(Collections.emptySet())
                .copyWebApplicationFiles(fileNotDirectory, testDirectory.getRoot(), "sss");
    }

    @Test
    public void copyWebApplicationFiles_nothingSpecified() {
        File outputDirectory = testDirectory.getRoot();
        new ProductionModeCopyStep(Collections.emptySet())
                .copyWebApplicationFiles(outputDirectory, null, null);
        List<String> resultingFiles = TestUtils.listFilesRecursively(outputDirectory);

        assertTrue("Output directory should not contain any files since no frontend directory or jars are specified",
                resultingFiles.isEmpty());
    }

    @Test
    public void copyWebApplicationFiles_copyFrontendDirectory_noExclusions() {
        File outputDirectory = testDirectory.getRoot();
        File frontendOutputDirectory = new File(".").getAbsoluteFile();
        SortedSet<String> originalFiles = new TreeSet<>(TestUtils.listFilesRecursively(frontendOutputDirectory));

        new ProductionModeCopyStep(Collections.emptySet())
                .copyWebApplicationFiles(outputDirectory, frontendOutputDirectory, null);
        assertEquals("Output directory should contain all files from frontend directory '%s' and only them",
                originalFiles, new TreeSet<>(TestUtils.listFilesRecursively(outputDirectory)));
    }

    @Test
    public void copyWebApplicationFiles_copyFrontendDirectory_withExclusions() {
        File outputDirectory = testDirectory.getRoot();
        File frontendOutputDirectory = new File(".").getAbsoluteFile();
        List<String> originalFiles = TestUtils.listFilesRecursively(frontendOutputDirectory);

        new ProductionModeCopyStep(Collections.emptySet())
                .copyWebApplicationFiles(outputDirectory, frontendOutputDirectory, "*.jar, *.class");

        SortedSet<String> filteredPaths = originalFiles.stream().filter(path -> !path.endsWith(".jar") && !path.endsWith(".class"))
                .collect(Collectors.toCollection(TreeSet::new));
        assertFalse("Original directory should contain files that are not filtered", filteredPaths.isEmpty());
        assertEquals("Output directory should contain filtered files from frontend directory '%s' and only them",
                filteredPaths, new TreeSet<>(TestUtils.listFilesRecursively(outputDirectory)));
    }

    @Test
    public void copyWebApplicationFiles_copyWebJar_noExclusions() {
        String version = "2.0.0";
        String artifactId = "paper-button";
        File outputDirectory = testDirectory.getRoot();

        new ProductionModeCopyStep(Collections.singleton(getWebJarData(version, artifactId)))
                .copyWebApplicationFiles(outputDirectory, null, null);

        String expectedPathPrefix = "bower_components" + File.separator + artifactId;
        List<String> resultingFiles = TestUtils.listFilesRecursively(outputDirectory);

        assertFalse("WebJar files should be present in output directory",
                resultingFiles.isEmpty());
        assertTrue("All WebJar files should be put into (bower_components + File.separator + bower name for WebJar) directory",
                resultingFiles.stream().allMatch(path -> path.startsWith(expectedPathPrefix)));
    }

    @Test
    public void copyWebApplicationFiles_copyWebJar_excludeAll() {
        String version = "2.0.0";
        String artifactId = "paper-button";
        File outputDirectory = testDirectory.getRoot();

        new ProductionModeCopyStep(Collections.singleton(getWebJarData(version, artifactId)))
                .copyWebApplicationFiles(outputDirectory, null, "*");

        assertTrue("WebJar files should not be copied due to exclusions",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());
    }

    @Test
    public void copyWebApplicationFiles_copyWebJar_bowerJsonShouldBePresent() throws IOException {
        String version = "2.0.0";
        String artifactId = "github-com-polymerelements-paper-button";
        ArtifactData webJarToCopy = getWebJarData(version, artifactId);

        JarContentsManager noBowerJsonManager = mock(JarContentsManager.class);
        String expectedFilePath = "bower.json";
        when(noBowerJsonManager.containsPath(webJarToCopy.getFileOrDirectory(), ProductionModeCopyStep.WEB_JAR_FILES_BASE)).thenReturn(true);
        when(noBowerJsonManager.findFiles(webJarToCopy.getFileOrDirectory(), ProductionModeCopyStep.WEB_JAR_FILES_BASE, expectedFilePath))
                .thenReturn(Collections.emptyList());

        File outputDirectory = testDirectory.getRoot();
        assertTrue("No files should be in output directory before the beginning",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());

        new ProductionModeCopyStep(noBowerJsonManager, Collections.singleton(webJarToCopy))
                .copyWebApplicationFiles(outputDirectory, null, null);

        assertTrue("WebJar with no bower.json is not unpacked into output directory.",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());

        verify(noBowerJsonManager, times(1)).containsPath(
                webJarToCopy.getFileOrDirectory(),
                ProductionModeCopyStep.WEB_JAR_FILES_BASE);
        verify(noBowerJsonManager, times(1)).findFiles(
                webJarToCopy.getFileOrDirectory(),
                ProductionModeCopyStep.WEB_JAR_FILES_BASE, expectedFilePath);
    }

    @Test
    public void copyWebApplicationFiles_copyNonWebJar_noFrontendFiles() {
        File outputDirectory = testDirectory.getRoot();
        ArtifactData noFrontendFilesJar = getTestArtifact("jar-without-frontend-resources.jar");
        new ProductionModeCopyStep(Collections.singleton(noFrontendFilesJar))
                .copyWebApplicationFiles(outputDirectory, null, null);
        assertEquals("Non WebJar with no web resources should not be copied to output directory",
                TestUtils.listFilesRecursively(outputDirectory).size(), 0);
    }

    @Test
    public void copyWebApplicationFiles_copyNonWebJar_withFrontendFiles_noExclusions() {
        File outputDirectory = testDirectory.getRoot();
        ArtifactData jarWithFrontendFiles = getTestArtifact("jar-with-frontend-resources.jar");
        new ProductionModeCopyStep(Collections.singleton(jarWithFrontendFiles))
                .copyWebApplicationFiles(outputDirectory, null, null);
        assertTrue("Non WebJar with web resources should be copied to output directory",
                TestUtils.listFilesRecursively(outputDirectory).size() > 0);
    }

    @Test
    public void copyWebApplicationFiles_directoryPathsAndNonExistingFilesIgnored() {
        File outputDirectory = testDirectory.getRoot();
        ArtifactData directoryInsteadOfFile = new ArtifactData(outputDirectory, "whatever", "whatever");
        ArtifactData nonExistingFile = new ArtifactData(new File("nope"), "whatever", "whatever");

        new ProductionModeCopyStep(
                Arrays.asList(directoryInsteadOfFile, nonExistingFile))
                        .copyWebApplicationFiles(outputDirectory, null, null);

        assertTrue(
                "Only artifacts with a file should be extracted to the directory",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());
    }

    @Test
    public void copyWebApplicationFiles_copyNonWebJar_withFrontendFiles_withExclusions() throws IOException {
        ArtifactData jarWithFrontendFiles = getTestArtifact("jar-with-frontend-resources.jar");

        File noExclusionsDirectory = testDirectory.newFolder("noExclusions");
        new ProductionModeCopyStep(Collections.singleton(jarWithFrontendFiles))
                .copyWebApplicationFiles(noExclusionsDirectory, null, null);
        List<String> allFiles = TestUtils.listFilesRecursively(noExclusionsDirectory);
        assertTrue("Files copied without filters should contain *.html and *.json files",
                allFiles.stream().anyMatch(path -> path.endsWith(".json") || path.endsWith(".html")));

        File exclusionsDirectory = testDirectory.newFolder("exclusions");
        new ProductionModeCopyStep(Collections.singleton(jarWithFrontendFiles))
                .copyWebApplicationFiles(exclusionsDirectory, null, "*.json, *.html");
        List<String> filteredFiles = TestUtils.listFilesRecursively(exclusionsDirectory);

        assertTrue("Files copied without filter should contain more files than the filtered ones",
                allFiles.size() > filteredFiles.size());
        assertTrue("Files copied without filters should not contain *.html and *.json files", filteredFiles.stream().noneMatch(path -> path.endsWith(".json") || path.endsWith(".html")));
        assertTrue("Files copied without filter should contain all filtered files", allFiles.containsAll(filteredFiles));
    }

    /**
     * WebJar tested has a name github-com-PolymerElements-iron-behaviors-2.0.0.jar but all paths inside are lower cased.
     * Looks like an exception rather than a regular WebJar, but we should be able to bypass it anyway.
     *
     * @see <a href="https://github.com/webjars/webjars/issues/1668">https://github.com/webjars/webjars/issues/1668</a>
     */
    @Test
    public void copyWebApplicationFiles_webJarWithWrongCasedInside() {
        File outputDirectory = testDirectory.getRoot();
        assertTrue("No files should be in output directory before the beginning",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());
        String version = "2.0.0";
        String artifactId = "github-com-PolymerElements-iron-behaviors";

        new ProductionModeCopyStep(
                Arrays.asList(getWebJarData(version, artifactId),
                        getWebJarData(version, artifactId)))
                                .copyWebApplicationFiles(outputDirectory, null,
                                        null);

        List<String> resultingFiles = TestUtils.listFilesRecursively(outputDirectory);

        assertFalse("WebJar files should be present in output directory",
                resultingFiles.isEmpty());
        assertTrue("All WebJar files should be put into (bower_components + File.separator + bower name for WebJar) directory",
                resultingFiles.stream().allMatch(path -> path.startsWith("bower_components" + File.separator + "iron-behaviors")));
    }

    private ArtifactData getTestArtifact(String jarName) {
        return new ArtifactData(TestUtils.getTestJar(jarName),
                "test-jar-artifact-id", "0.0.1");
    }
}
