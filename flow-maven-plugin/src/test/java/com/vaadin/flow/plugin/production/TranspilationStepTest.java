package com.vaadin.flow.plugin.production;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.plugin.common.FrontendDataProvider;
import com.vaadin.flow.plugin.common.FrontendToolsManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vaadin Ltd.
 */
public class TranspilationStepTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final boolean skipEs5 = true;
    private final String nodeVersion = "1";
    private final String yarnVersion = "1";
    private final ProxyConfig proxyConfig = new ProxyConfig(Collections.emptyList());

    private FrontendToolsManager getFrontendToolsManager(File outputDirectory) {
        FrontendDataProvider frontendDataProviderMock = mock(FrontendDataProvider.class);
        when(frontendDataProviderMock.createShellFile(outputDirectory)).thenReturn("shell");
        when(frontendDataProviderMock.createFragmentFiles(outputDirectory)).thenReturn(Collections.singleton("fragment"));

        FrontendToolsManager frontendToolsManager = spy(new FrontendToolsManager(outputDirectory, "frontend-es5", "frontend-es6", frontendDataProviderMock));
        doNothing().when(frontendToolsManager).installFrontendTools(proxyConfig, nodeVersion, yarnVersion);
        return frontendToolsManager;
    }

    @Test
    public void transpileFiles_outputDirectoryAsFile() throws IOException {
        File fileNotDirectory = temporaryFolder.newFile("nope");
        exception.expect(UncheckedIOException.class);
        exception.expectMessage(fileNotDirectory.toString());

        new TranspilationStep(getFrontendToolsManager(temporaryFolder.getRoot()), proxyConfig, nodeVersion, yarnVersion)
                .transpileFiles(temporaryFolder.getRoot(), fileNotDirectory, skipEs5);
    }

    @Test
    public void transpileFiles_nonExistingEs6Directory() {
        File nonExistingFile = new File("desNotExist");
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(nonExistingFile.toString());

        new TranspilationStep(getFrontendToolsManager(temporaryFolder.getRoot()), proxyConfig, nodeVersion, yarnVersion)
                .transpileFiles(nonExistingFile, temporaryFolder.getRoot(), skipEs5);
    }

    @Test
    public void transpileFiles_es6DirectoryAsFile() throws IOException {
        File fileNotDirectory = temporaryFolder.newFile("nope");
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(fileNotDirectory.toString());

        new TranspilationStep(getFrontendToolsManager(temporaryFolder.getRoot()), proxyConfig, nodeVersion, yarnVersion)
                .transpileFiles(fileNotDirectory, temporaryFolder.getRoot(), skipEs5);
    }

    @Test
    public void transpileFiles_emptyResults() {
        File outputDirectory = new File("output");
        File es6Directory = new File(outputDirectory, "frontend");
        FrontendToolsManager toolsManagerMock = mock(FrontendToolsManager.class);
        when(toolsManagerMock.transpileFiles(es6Directory, outputDirectory, skipEs5)).thenReturn(Collections.emptyMap());

        try {
            new TranspilationStep(toolsManagerMock, proxyConfig, nodeVersion, yarnVersion).transpileFiles(es6Directory, outputDirectory, skipEs5);
            fail("Frontend manager had returned empty transpilation results, but the step does not fail");
        } catch (IllegalStateException expected) {
            //expected
        }
        verify(toolsManagerMock, times(1)).installFrontendTools(proxyConfig, nodeVersion, yarnVersion);
        verify(toolsManagerMock, times(1)).transpileFiles(es6Directory, outputDirectory, skipEs5);
    }

    @Test
    public void transpileFiles_noTranspilationDirectory() throws IOException {
        File outputDirectory = temporaryFolder.newFolder("output");
        File es6Directory = temporaryFolder.newFolder("output", "frontend");
        File nonExistingFile = new File("doesNotExist");
        FrontendToolsManager toolsManagerMock = mock(FrontendToolsManager.class);
        when(toolsManagerMock.transpileFiles(es6Directory, outputDirectory, skipEs5)).thenReturn(Collections.singletonMap(es6Directory.getName(), nonExistingFile));

        try {
            new TranspilationStep(toolsManagerMock, proxyConfig, nodeVersion, yarnVersion).transpileFiles(es6Directory, outputDirectory, skipEs5);
            fail(String.format("Directory '%s' does not contain transpilation results, but the step does not fail", nonExistingFile));
        } catch (IllegalStateException expected) {
            //expected
        }
        verify(toolsManagerMock, times(1)).installFrontendTools(proxyConfig, nodeVersion, yarnVersion);
        verify(toolsManagerMock, times(1)).transpileFiles(es6Directory, outputDirectory, skipEs5);
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
        when(toolsManagerMock.transpileFiles(es6SourceDirectory, outputDirectory, skipEs5))
                .thenReturn(ImmutableMap.of("frontend-es5", es5TranspiledDirectory, "frontend-es6", es6TranspiledDirectory));

        new TranspilationStep(toolsManagerMock, proxyConfig, nodeVersion, yarnVersion).transpileFiles(es6SourceDirectory, outputDirectory, skipEs5);

        assertEquals("Es6 source files should be left untouched", sourceFiles, TestUtils.listFilesRecursively(es6SourceDirectory));

        List<String> pathsAfterTranspilation = TestUtils.listFilesRecursively(outputDirectory);
        assertTrue("ES5 transpilation result should be present", pathsAfterTranspilation.stream().anyMatch(path1 -> path1.endsWith(es5TranspiledFile.getName())));
        assertTrue("ES6 transpilation result should be present", pathsAfterTranspilation.stream().anyMatch(path1 -> path1.endsWith(es6transpiledFile.getName())));

        verify(toolsManagerMock, times(1)).installFrontendTools(proxyConfig, nodeVersion, yarnVersion);
        verify(toolsManagerMock, times(1)).transpileFiles(es6SourceDirectory, outputDirectory, skipEs5);
    }
}
