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

package com.vaadin.flow.plugin.common;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.component.dependency.HtmlImport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vaadin Ltd.
 */
public class FrontendDataProviderTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private File sourceDirectory;
    private File targetDirectory;
    private File jsFile;
    private File cssFile;
    private File htmlFile;

    @Before
    public void createFrontendSources() throws IOException {
        sourceDirectory = temporaryFolder.newFolder("es6Source");
        targetDirectory = temporaryFolder.newFolder("target");
        jsFile = createFile(sourceDirectory, "test.js");
        cssFile = createFile(sourceDirectory, "test.css");
        htmlFile = createFile(sourceDirectory, "test.html");
    }

    private File createFile(File directory, String fileName) throws IOException {
        File result = new File(directory, fileName);
        if (!result.createNewFile()) {
            throw new IllegalStateException(String.format("Failed to create file '%s'", result));
        }
        return result;
    }

    @Test
    public void fragmentsFilesAreCheckedForExistence() {
        String nonExistentFragmentFile = "nonExistentFragmentFile";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(new File(sourceDirectory, nonExistentFragmentFile).getAbsolutePath());

        new FrontendDataProvider(true, sourceDirectory, mock(AnnotationValuesExtractor.class), null,
                Collections.singletonMap("fragmentName", Collections.singleton(nonExistentFragmentFile)));
    }

    @Test
    public void shellImportsAreCheckedForExistence() {
        String nonExistentImport = "nonExistentImport";
        AnnotationValuesExtractor annotationValuesExtractorMock = mock(AnnotationValuesExtractor.class);
        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap())).thenReturn(Collections.singletonMap(HtmlImport.class, Collections.singleton(nonExistentImport)));

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(new File(sourceDirectory, nonExistentImport).getAbsolutePath());

        new FrontendDataProvider(true, sourceDirectory, annotationValuesExtractorMock, null, Collections.emptyMap());

        verify(annotationValuesExtractorMock, only()).extractAnnotationValues(anyMap());
        verify(annotationValuesExtractorMock, times(1)).extractAnnotationValues(anyMap());
    }

    @Test
    public void shouldBundleIsPersisted() {
        boolean shouldBundle = ThreadLocalRandom.current().nextBoolean();
        AnnotationValuesExtractor annotationValuesExtractorMock = mock(AnnotationValuesExtractor.class);
        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap())).thenReturn(Collections.emptyMap());

        FrontendDataProvider frontendDataProvider = new FrontendDataProvider(shouldBundle, sourceDirectory, annotationValuesExtractorMock, null, Collections.emptyMap());

        boolean actualShouldBundle = frontendDataProvider.shouldBundle();
        assertEquals("Expect to have the same value for 'shouldBundle' variable as passed into a constructor", shouldBundle, actualShouldBundle);

        verify(annotationValuesExtractorMock, only()).extractAnnotationValues(anyMap());
        verify(annotationValuesExtractorMock, times(1)).extractAnnotationValues(anyMap());
    }

    @Test
    public void fragmentsAreNotCreatedOrCheckedIfBundlingIsDisabled() {
        AnnotationValuesExtractor annotationValuesExtractorMock = mock(AnnotationValuesExtractor.class);
        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap())).thenReturn(Collections.emptyMap());

        FrontendDataProvider frontendDataProvider = new FrontendDataProvider(false, sourceDirectory, annotationValuesExtractorMock, null,
                Collections.singletonMap("whatever", Collections.singleton("doesNotMatter")));
        Set<String> fragmentFiles = frontendDataProvider.createFragmentFiles(targetDirectory);

        assertTrue("Fragment files should not be created or checked if bundling is disabled", fragmentFiles.isEmpty());
        assertTrue("There should be no files created in the target directory", targetDirectory.listFiles().length == 0);
        verify(annotationValuesExtractorMock, only()).extractAnnotationValues(anyMap());
        verify(annotationValuesExtractorMock, times(1)).extractAnnotationValues(anyMap());
    }

    @Test
    public void fragmentsContentsIsNotIncludedIntoShellFile() throws IOException {
        String fragmentName = "userFragment";
        Set<String> fragmentImports = ImmutableSet.of(jsFile.getName(), cssFile.getName());
        Set<String> shellImports = Collections.singleton(htmlFile.getName());

        Set<String> allImports = new HashSet<>(fragmentImports);
        allImports.addAll(shellImports);

        AnnotationValuesExtractor annotationValuesExtractorMock = mock(AnnotationValuesExtractor.class);
        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap())).thenReturn(Collections.singletonMap(HtmlImport.class, allImports));

        FrontendDataProvider frontendDataProvider = new FrontendDataProvider(true, sourceDirectory, annotationValuesExtractorMock, null,
                Collections.singletonMap(fragmentName, fragmentImports));

        Set<String> fragmentFilePaths = frontendDataProvider.createFragmentFiles(targetDirectory);
        assertEquals("Single fragment name passed should result in single fragment created", 1, fragmentFilePaths.size());
        findAndVerifyFragment(fragmentFilePaths, fragmentName, fragmentImports);

        String shellFile = frontendDataProvider.createShellFile(targetDirectory);
        verifyFileWithImports(shellFile, shellImports);

        verify(annotationValuesExtractorMock, only()).extractAnnotationValues(anyMap());
        verify(annotationValuesExtractorMock, times(1)).extractAnnotationValues(anyMap());
    }

    @Test
    public void userDefinedAndConfigurationFileFragmentsAreMerged() throws IOException {
        AnnotationValuesExtractor annotationValuesExtractorMock = mock(AnnotationValuesExtractor.class);
        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap())).thenReturn(Collections.emptyMap());

        String firstFragment = "firstFragment";
        String firstFragmentImport = jsFile.getName();

        String secondFragment = "secondFragment";
        String secondFragmentImport = htmlFile.getName();
        File configurationFile = temporaryFolder.newFile("fragment-configuration.json");
        String additionalFirstFragmentImport = cssFile.getName();
        Files.write(configurationFile.toPath(), Collections.singletonList(String.format(
                "{'fragments':[ {'name': '%s', 'files': ['%s']}, {'name': '%s', 'files': ['%s']} ]}",
                firstFragment, additionalFirstFragmentImport, secondFragment, secondFragmentImport)), StandardCharsets.UTF_8);

        FrontendDataProvider frontendDataProvider = new FrontendDataProvider(true, sourceDirectory, annotationValuesExtractorMock, configurationFile,
                Collections.singletonMap(firstFragment, Collections.singleton(firstFragmentImport)));

        Set<String> fragmentFilePaths = frontendDataProvider.createFragmentFiles(targetDirectory);
        assertEquals("Both fragments from file and method argument should be created", 2, fragmentFilePaths.size());
        findAndVerifyFragment(fragmentFilePaths, firstFragment, Arrays.asList(firstFragmentImport, additionalFirstFragmentImport));
        findAndVerifyFragment(fragmentFilePaths, secondFragment, Collections.singletonList(secondFragmentImport));

        String shellFile = frontendDataProvider.createShellFile(targetDirectory);
        verifyFileWithImports(shellFile, Collections.emptySet());

        verify(annotationValuesExtractorMock, only()).extractAnnotationValues(anyMap());
        verify(annotationValuesExtractorMock, times(1)).extractAnnotationValues(anyMap());
    }

    private void findAndVerifyFragment(Collection<String> outputFragmentNames, String fragmentNameToFind, Collection<String> expectedImports) throws IOException {
        String expectedFragmentFileName = fragmentNameToFind + ".html";
        verifyFileWithImports(
                outputFragmentNames.stream()
                        .filter(fragmentFilePath -> fragmentFilePath.endsWith(expectedFragmentFileName))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(String.format("Failed to find fragment file '%s' in output fragment names", expectedFragmentFileName))),
                expectedImports);
    }

    private void verifyFileWithImports(String fragmentFilePath, Collection<String> expectedImports) throws IOException {
        List<String> fileImports = Files.lines(Paths.get(fragmentFilePath)).collect(Collectors.toList());
        assertEquals(String.format("Incorrect number of imports in file '%s'", fragmentFilePath), expectedImports.size(), fileImports.size());
        expectedImports.forEach(expectedImport -> assertTrue(String.format("Expected file '%s' to contain import of file '%s'", fragmentFilePath, expectedImport),
                fileImports.stream().anyMatch(fileImport -> fileImport.contains(expectedImport))));
    }
}
