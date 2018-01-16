package com.vaadin.flow.plugin.production;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;
import com.vaadin.flow.plugin.common.FrontendToolsManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vaadin Ltd.
 */
public class TranspilationStepTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final boolean skipEs5 = true;
    private final boolean bundle = true;

    private FrontendToolsManager getFrontendToolsManager(File outputDirectory) {
        return new FrontendToolsManager(new AnnotationValuesExtractor(), outputDirectory, "frontend-es5", "frontend-es6", null);
    }

    @Test(expected = IllegalStateException.class)
    public void transpileFiles_nonExistingOutputDirectory() {
        new TranspilationStep(getFrontendToolsManager(temporaryFolder.getRoot()))
                .transpileFiles(temporaryFolder.getRoot(), new File("nope"), skipEs5, bundle, Collections.emptyMap());
    }

    @Test(expected = UncheckedIOException.class)
    public void transpileFiles_outputDirectoryAsFile() throws IOException {
        new TranspilationStep(getFrontendToolsManager(temporaryFolder.getRoot()))
                .transpileFiles(temporaryFolder.getRoot(), temporaryFolder.newFile("nope"), skipEs5, bundle, Collections.emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void transpileFiles_nonExistingEs6Directory() {
        new TranspilationStep(getFrontendToolsManager(temporaryFolder.getRoot()))
                .transpileFiles(new File("desNotExist"), temporaryFolder.getRoot(), skipEs5, bundle, Collections.emptyMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void transpileFiles_es6DirectoryAsFile() throws IOException {
        new TranspilationStep(getFrontendToolsManager(temporaryFolder.getRoot()))
                .transpileFiles(temporaryFolder.newFile("nope"), temporaryFolder.getRoot(), skipEs5, bundle, Collections.emptyMap());
    }

    @Test
    public void transpileFiles_emptyResults() throws IOException {
        File outputDirectory = new File("output");
        File es6Directory = new File(outputDirectory, "frontend");
        FrontendToolsManager toolsManagerMock = mock(FrontendToolsManager.class);
        when(toolsManagerMock.transpileFiles(es6Directory, outputDirectory, skipEs5, bundle, Collections.emptyMap())).thenReturn(Collections.emptyMap());

        try {
            new TranspilationStep(toolsManagerMock).transpileFiles(es6Directory, outputDirectory, skipEs5, bundle, Collections.emptyMap());
            fail("Frontend manager had returned empty transpilation results, but the step does not fail");
        } catch (IllegalStateException expected) {
            //expected
        }
        verify(toolsManagerMock, only()).transpileFiles(es6Directory, outputDirectory, skipEs5, bundle, Collections.emptyMap());
        verify(toolsManagerMock, times(1)).transpileFiles(es6Directory, outputDirectory, skipEs5, bundle, Collections.emptyMap());
    }

    @Test
    public void transpileFiles_noTranspilationDirectory() throws IOException {
        File outputDirectory = temporaryFolder.newFolder("output");
        File es6Directory = temporaryFolder.newFolder("output", "frontend");
        File nonExistingFile = new File("doesNotExist");
        FrontendToolsManager toolsManagerMock = mock(FrontendToolsManager.class);
        when(toolsManagerMock.transpileFiles(es6Directory, outputDirectory, skipEs5, bundle, Collections.emptyMap())).thenReturn(Collections.singletonMap(es6Directory.getName(), nonExistingFile));

        try {
            new TranspilationStep(toolsManagerMock).transpileFiles(es6Directory, outputDirectory, skipEs5, bundle, Collections.emptyMap());
            fail(String.format("Directory '%s' does not contain transpilation results, but the step does not fail", nonExistingFile));
        } catch (IllegalStateException expected) {
            //expected
        }
        verify(toolsManagerMock, only()).transpileFiles(es6Directory, outputDirectory, skipEs5, bundle, Collections.emptyMap());
        verify(toolsManagerMock, times(1)).transpileFiles(es6Directory, outputDirectory, skipEs5, bundle, Collections.emptyMap());
    }

    @Test
    public void transpileFiles() throws IOException {
        File outputDirectory = temporaryFolder.newFolder("target");
        File es6SourceDirectory = temporaryFolder.newFolder("target", "frontend");
        temporaryFolder.newFile("target/frontend/index-es6-original.html");

        List<String> sourceFiles = TestUtils.listFilesRecursively(es6SourceDirectory);
        File es5TranspiledDirectory = temporaryFolder.newFolder("target", "build", "frontend-es5");
        File es5TranspiledFile = temporaryFolder.newFile("target/build/frontend-es5/index-es5.html");

        File es6TranspiledDirectory = temporaryFolder.newFolder("target", "build", "frontend-es6");
        File es6transpiledFile = temporaryFolder.newFile("target/build/frontend-es6/index-es6.html");

        FrontendToolsManager toolsManagerMock = mock(FrontendToolsManager.class);
        when(toolsManagerMock.transpileFiles(es6SourceDirectory, outputDirectory, skipEs5, bundle, Collections.emptyMap()))
                .thenReturn(ImmutableMap.of("frontend-es5", es5TranspiledDirectory, "frontend-es6", es6TranspiledDirectory));

        new TranspilationStep(toolsManagerMock).transpileFiles(es6SourceDirectory, outputDirectory, skipEs5, bundle, Collections.emptyMap());

        assertEquals("Es6 source files should be left untouched", sourceFiles, TestUtils.listFilesRecursively(es6SourceDirectory));

        List<String> pathsAfterTranspilation = TestUtils.listFilesRecursively(outputDirectory);
        assertTrue("ES5 transpilation result should be present", pathsAfterTranspilation.stream().anyMatch(path1 -> path1.endsWith(es5TranspiledFile.getName())));
        assertTrue("ES6 transpilation result should be present", pathsAfterTranspilation.stream().anyMatch(path1 -> path1.endsWith(es6transpiledFile.getName())));

        verify(toolsManagerMock, only()).transpileFiles(es6SourceDirectory, outputDirectory, skipEs5, bundle, Collections.emptyMap());
        verify(toolsManagerMock, times(1)).transpileFiles(es6SourceDirectory, outputDirectory, skipEs5, bundle, Collections.emptyMap());
    }
}
