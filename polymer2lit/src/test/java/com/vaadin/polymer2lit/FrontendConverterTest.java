package com.vaadin.polymer2lit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;

public class FrontendConverterTest {
    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private FrontendConverter frontendConverter;

    @Before
    public void init() throws IOException {
        String baseDir = tmpDir.newFolder().getAbsolutePath();
        String vaadinHomeDir = tmpDir.newFolder().getAbsolutePath();
        FrontendToolsSettings settings = new FrontendToolsSettings(baseDir,
                () -> vaadinHomeDir);
        FrontendTools tools = new FrontendTools(settings);
        frontendConverter = new FrontendConverter(tools);
    }

    @After
    public void close() throws IOException {
        frontendConverter.close();
    }

    @Test
    public void convertDifferentFiles_outputFilesMatchExpectedOnes()
            throws IOException, InterruptedException {
        convertFile_outputFileMatchesExpectedOne("basic-bindings.js");
        convertFile_outputFileMatchesExpectedOne("computed-property.js");
        convertFile_outputFileMatchesExpectedOne("disabled-using-method.js");
        convertFile_outputFileMatchesExpectedOne("dom-if.js");
        convertFile_outputFileMatchesExpectedOne("dom-repeat.js");
        convertFile_outputFileMatchesExpectedOne("event-handlers.js");
        convertFile_outputFileMatchesExpectedOne("grid-columns.js");
        convertFile_outputFileMatchesExpectedOne("inline-styles.js");
        convertFile_outputFileMatchesExpectedOne("light-dom.js");
        convertFile_outputFileMatchesExpectedOne("multiple-bindings.js");
        convertFile_outputFileMatchesExpectedOne("nested-dom-repeat.js");
        convertFile_outputFileMatchesExpectedOne("order-card-from-bakery.js");
        convertFile_outputFileMatchesExpectedOne("ready-callback.js");
        convertFile_outputFileMatchesExpectedOne("simple-observer.js");
        convertFile_outputFileMatchesExpectedOne("sub-properties.js");
        convertFile_outputFileMatchesExpectedOne(
                "this-dollar-mapped-element-ids.js");
        convertFile_outputFileMatchesExpectedOne("two-way-binding.js");
    }

    private void convertFile_outputFileMatchesExpectedOne(String fileName)
            throws IOException, InterruptedException {
        InputStream inputFileStream = getClass().getClassLoader()
                .getResourceAsStream("frontend/in/" + fileName);
        InputStream expectedFileStream = getClass().getClassLoader()
                .getResourceAsStream("frontend/expected/" + fileName);

        Path tmpInputFilePath = tmpDir.newFile().toPath();
        Files.copy(inputFileStream, tmpInputFilePath,
                StandardCopyOption.REPLACE_EXISTING);

        frontendConverter.convertFile(tmpInputFilePath);

        String expectedContent = new String(expectedFileStream.readAllBytes(),
                StandardCharsets.UTF_8);
        String actualContent = Files.readString(tmpInputFilePath,
                StandardCharsets.UTF_8);
        Assert.assertEquals(
                "The output " + fileName
                        + " file does not match the expected one.",
                expectedContent, actualContent);
    }
}
