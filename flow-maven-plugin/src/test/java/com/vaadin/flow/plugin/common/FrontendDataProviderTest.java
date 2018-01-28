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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.theme.AbstractTheme;

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

    private ThemedURLTranslator translator = Mockito
            .mock(ThemedURLTranslator.class);

    public class TestFrontendDataProvider extends FrontendDataProvider {

        public TestFrontendDataProvider(boolean shouldBundle,
                File es6SourceDirectory,
                AnnotationValuesExtractor annotationValuesExtractor,
                File fragmentConfigurationFile,
                Map<String, Set<String>> userDefinedFragments) {
            super(shouldBundle, es6SourceDirectory, annotationValuesExtractor,
                    fragmentConfigurationFile, userDefinedFragments);
        }

        @Override
        protected ThemedURLTranslator getTranslator(File es6SourceDirectory,
                ClassPathIntrospector introspector) {
            return translator;
        }

    }

    public static class TestTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return "src/";
        }

        @Override
        public String getThemeUrl() {
            return "theme/myTheme/";
        }

    }

    @Before
    public void createFrontendSources() throws IOException {
        sourceDirectory = temporaryFolder.newFolder("es6Source");
        targetDirectory = temporaryFolder.newFolder("target");
        jsFile = createFile(sourceDirectory, "test.js");
        cssFile = createFile(sourceDirectory, "test.css");
        htmlFile = createFile(sourceDirectory, "test.html");

        Mockito.doAnswer(invocation -> {
            return invocation.getArgumentAt(0, Set.class);
        }).when(translator).applyTheme(Matchers.any());
    }

    private File createFile(File directory, String fileName)
            throws IOException {
        File result = new File(directory, fileName);
        if (!result.createNewFile()) {
            throw new IllegalStateException(
                    String.format("Failed to create file '%s'", result));
        }
        return result;
    }

    @Test
    public void fragmentsFilesAreCheckedForExistence() {
        String nonExistentFragmentFile = "nonExistentFragmentFile";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(
                new File(sourceDirectory, nonExistentFragmentFile)
                        .getAbsolutePath());

        new TestFrontendDataProvider(true, sourceDirectory,
                mock(AnnotationValuesExtractor.class), null,
                Collections.singletonMap("fragmentName",
                        Collections.singleton(nonExistentFragmentFile)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void shellImportsAreCheckedForExistence() {
        String nonExistentImport = "nonExistentImport";
        AnnotationValuesExtractor annotationValuesExtractorMock = mock(
                AnnotationValuesExtractor.class);
        Map<Class<HtmlImport>, Set<String>> map = Collections.singletonMap(
                HtmlImport.class, Collections.singleton(nonExistentImport));
        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap()))
                .thenReturn(new HashMap<>(map));

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(
                new File(sourceDirectory, nonExistentImport).getAbsolutePath());

        new TestFrontendDataProvider(true, sourceDirectory,
                annotationValuesExtractorMock, null, Collections.emptyMap());

        verify(annotationValuesExtractorMock, Mockito.times(2))
                .extractAnnotationValues(anyMap());
    }

    @Test
    public void shouldBundleIsPersisted() {
        boolean shouldBundle = ThreadLocalRandom.current().nextBoolean();
        AnnotationValuesExtractor annotationValuesExtractorMock = mock(
                AnnotationValuesExtractor.class);
        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap()))
                .thenReturn(new HashMap<>());

        FrontendDataProvider frontendDataProvider = new TestFrontendDataProvider(
                shouldBundle, sourceDirectory, annotationValuesExtractorMock,
                null, Collections.emptyMap());

        boolean actualShouldBundle = frontendDataProvider.shouldBundle();
        assertEquals(
                "Expect to have the same value for 'shouldBundle' variable as passed into a constructor",
                shouldBundle, actualShouldBundle);

        verify(annotationValuesExtractorMock, Mockito.times(2))
                .extractAnnotationValues(anyMap());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void themedHtmlImports_existingThemedImportIsConverted_nonExistentIsPreserved()
            throws IOException {
        File src = new File(sourceDirectory, "src");
        src.mkdir();
        createFile(src, "component1.html");
        File a = createFile(src, "component2.html");

        File theme = new File(sourceDirectory, "theme");
        theme.mkdir();
        theme = new File(theme, "myTheme");
        theme.mkdir();
        createFile(theme, "component1.html");

        AnnotationValuesExtractor annotationValuesExtractorMock = mock(
                AnnotationValuesExtractor.class);

        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap()))
                .thenReturn(
                        new HashMap<>(Collections.singletonMap(HtmlImport.class,
                                new HashSet<>(
                                        Arrays.asList("src/component1.html",
                                                "src/component2.html")))));

        Mockito.when(translator.applyTheme(Mockito.anySet())).thenReturn(
                new HashSet<>(Arrays.asList("theme/myTheme/component1.html",
                        "src/component2.html")));

        FrontendDataProvider provider = new TestFrontendDataProvider(true,
                sourceDirectory, annotationValuesExtractorMock, null,
                Collections.emptyMap());

        provider.createShellFile(targetDirectory);
        File bundle = new File(targetDirectory, "vaadin-flow-bundle.html");
        List<String> lines = Files.readAllLines(bundle.toPath(),
                StandardCharsets.UTF_8);
        String content = lines.stream().collect(Collectors.joining(""));
        Assert.assertThat(content, CoreMatchers
                .containsString("es6Source/theme/myTheme/component1.html"));
        Assert.assertThat(content,
                CoreMatchers.containsString("es6Source/src/component2.html"));

        verify(annotationValuesExtractorMock, Mockito.times(2))
                .extractAnnotationValues(anyMap());
    }

    @Test
    public void fragmentsAreNotCreatedOrCheckedIfBundlingIsDisabled() {
        AnnotationValuesExtractor annotationValuesExtractorMock = mock(
                AnnotationValuesExtractor.class);
        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap()))
                .thenReturn(new HashMap<>());

        FrontendDataProvider frontendDataProvider = new TestFrontendDataProvider(
                false, sourceDirectory, annotationValuesExtractorMock, null,
                Collections.singletonMap("whatever",
                        Collections.singleton("doesNotMatter")));
        Set<String> fragmentFiles = frontendDataProvider
                .createFragmentFiles(targetDirectory);

        assertTrue(
                "Fragment files should not be created or checked if bundling is disabled",
                fragmentFiles.isEmpty());
        assertTrue("There should be no files created in the target directory",
                targetDirectory.listFiles().length == 0);
        verify(annotationValuesExtractorMock, Mockito.times(2))
                .extractAnnotationValues(anyMap());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void fragmentsContentsIsNotIncludedIntoShellFile()
            throws IOException {
        String fragmentName = "userFragment";
        Set<String> fragmentImports = ImmutableSet.of(jsFile.getName(),
                cssFile.getName());
        Set<String> shellImports = Collections.singleton(htmlFile.getName());

        Set<String> allImports = new HashSet<>(fragmentImports);
        allImports.addAll(shellImports);

        AnnotationValuesExtractor annotationValuesExtractorMock = mock(
                AnnotationValuesExtractor.class);
        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap()))
                .thenReturn(new HashMap<>(Collections
                        .singletonMap(HtmlImport.class, allImports)));

        FrontendDataProvider frontendDataProvider = new TestFrontendDataProvider(
                true, sourceDirectory, annotationValuesExtractorMock, null,
                Collections.singletonMap(fragmentName, fragmentImports));

        Set<String> fragmentFilePaths = frontendDataProvider
                .createFragmentFiles(targetDirectory);
        assertEquals(
                "Single fragment name passed should result in single fragment created",
                1, fragmentFilePaths.size());
        findAndVerifyFragment(fragmentFilePaths, fragmentName, fragmentImports);

        String shellFile = frontendDataProvider
                .createShellFile(targetDirectory);
        verifyFileWithImports(shellFile, shellImports);

        verify(annotationValuesExtractorMock, Mockito.times(2))
                .extractAnnotationValues(anyMap());
    }

    @Test
    public void userDefinedAndConfigurationFileFragmentsAreMerged()
            throws IOException {
        AnnotationValuesExtractor annotationValuesExtractorMock = mock(
                AnnotationValuesExtractor.class);
        when(annotationValuesExtractorMock.extractAnnotationValues(anyMap()))
                .thenReturn(new HashMap<>());

        String firstFragment = "firstFragment";
        String firstFragmentImport = jsFile.getName();

        String secondFragment = "secondFragment";
        String secondFragmentImport = htmlFile.getName();
        File configurationFile = temporaryFolder
                .newFile("fragment-configuration.json");
        String additionalFirstFragmentImport = cssFile.getName();
        Files.write(configurationFile.toPath(),
                Collections.singletonList(String.format(
                        "{'fragments':[ {'name': '%s', 'files': ['%s']}, {'name': '%s', 'files': ['%s']} ]}",
                        firstFragment, additionalFirstFragmentImport,
                        secondFragment, secondFragmentImport)),
                StandardCharsets.UTF_8);

        FrontendDataProvider frontendDataProvider = new TestFrontendDataProvider(
                true, sourceDirectory, annotationValuesExtractorMock,
                configurationFile, Collections.singletonMap(firstFragment,
                        Collections.singleton(firstFragmentImport)));

        Set<String> fragmentFilePaths = frontendDataProvider
                .createFragmentFiles(targetDirectory);
        assertEquals(
                "Both fragments from file and method argument should be created",
                2, fragmentFilePaths.size());
        findAndVerifyFragment(fragmentFilePaths, firstFragment, Arrays
                .asList(firstFragmentImport, additionalFirstFragmentImport));
        findAndVerifyFragment(fragmentFilePaths, secondFragment,
                Collections.singletonList(secondFragmentImport));

        String shellFile = frontendDataProvider
                .createShellFile(targetDirectory);
        verifyFileWithImports(shellFile, Collections.emptySet());

        verify(annotationValuesExtractorMock, Mockito.times(2))
                .extractAnnotationValues(anyMap());
    }

    private void findAndVerifyFragment(Collection<String> outputFragmentNames,
            String fragmentNameToFind, Collection<String> expectedImports)
            throws IOException {
        String expectedFragmentFileName = fragmentNameToFind + ".html";
        verifyFileWithImports(outputFragmentNames.stream()
                .filter(fragmentFilePath -> fragmentFilePath
                        .endsWith(expectedFragmentFileName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(String.format(
                        "Failed to find fragment file '%s' in output fragment names",
                        expectedFragmentFileName))),
                expectedImports);
    }

    private void verifyFileWithImports(String fragmentFilePath,
            Collection<String> expectedImports) throws IOException {
        List<String> fileImports = Files.lines(Paths.get(fragmentFilePath))
                .collect(Collectors.toList());
        assertEquals(
                String.format("Incorrect number of imports in file '%s'",
                        fragmentFilePath),
                expectedImports.size(), fileImports.size());
        expectedImports.forEach(expectedImport -> assertTrue(
                String.format(
                        "Expected file '%s' to contain import of file '%s'",
                        fragmentFilePath, expectedImport),
                fileImports.stream().anyMatch(
                        fileImport -> fileImport.contains(expectedImport))));
    }
}
