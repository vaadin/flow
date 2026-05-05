/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.polymer2lit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.internal.FrontendUtils.CommandExecutionException;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("com.vaadin.flow.testcategory.SlowTests")
class FrontendConverterTest {
    @TempDir
    File tmpDir;

    private FrontendConverter frontendConverter;

    @BeforeEach
    void init() throws IOException {
        String baseDir = Files.createTempDirectory(tmpDir.toPath(), "tmp")
                .toFile().getAbsolutePath();
        String vaadinHomeDir = Files.createTempDirectory(tmpDir.toPath(), "tmp")
                .toFile().getAbsolutePath();
        FrontendToolsSettings settings = new FrontendToolsSettings(baseDir,
                () -> vaadinHomeDir);
        FrontendTools tools = new FrontendTools(settings);
        frontendConverter = new FrontendConverter(tools);
    }

    @AfterEach
    void teardown() throws IOException {
        frontendConverter.close();
    }

    @Test
    void basicBinding() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("basic-bindings.js");
    }

    @Test
    void basicBinding_lit1() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("basic-bindings.js",
                "basic-bindings-with-lit1.js", true, false);
    }

    @Test
    void computedProperty() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("computed-property.js");
    }

    @Test
    void disabledUsingMethod() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("disabled-using-method.js");
    }

    @Test
    void domIf() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("dom-if.js");
    }

    @Test
    void domRepeat() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("dom-repeat.js");
    }

    @Test
    void domRepeat_disabledOptionalChaining() throws IOException,
            InterruptedException, CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("dom-repeat.js",
                "dom-repeat-disabled-optional-chaining.js", false, true);
    }

    @Test
    void eventHandlers() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("event-handlers.js");
    }

    @Test
    void gridColumns() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("grid-columns.js");
    }

    @Test
    void inlineStyles() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("inline-styles.js");
    }

    @Test
    void lightDom() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("light-dom.js");
    }

    @Test
    void multipleBindings() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("multiple-bindings.js");
    }

    @Test
    void nestedDomRepeat() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("nested-dom-repeat.js");
    }

    @Test
    void nestedDomRepeatDisabledOptionalChaining() throws IOException,
            InterruptedException, CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("nested-dom-repeat.js",
                "nested-dom-repeat-disabled-optional-chaining.js", false, true);
    }

    @Test
    void orderCardFromBakery() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("order-card-from-bakery.js");
    }

    @Test
    void readyCallback() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("ready-callback.js");
    }

    @Test
    void simpleObserver() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("simple-observer.js");
    }

    @Test
    void subProperties() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("sub-properties.js");
    }

    @Test
    void subProperties_disabledOptionalChaining() throws IOException,
            InterruptedException, CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("sub-properties.js",
                "sub-properties-with-disabled-optional-chaining.js", false,
                true);
    }

    @Test
    void thisDollarMappedElementIds() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne(
                "this-dollar-mapped-element-ids.js");
    }

    @Test
    void twoWayBinding() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("two-way-binding.js");
    }

    private void convertFile_outputFileMatchesExpectedOne(String fileName)
            throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne(fileName, fileName, false,
                false);
    }

    private void convertFile_outputFileMatchesExpectedOne(String inFileName,
            String expectedFileName, boolean useLit1,
            boolean disableOptionalChaining) throws IOException,
            InterruptedException, CommandExecutionException {
        InputStream inFileStream = getClass().getClassLoader()
                .getResourceAsStream("frontend/in/" + inFileName);
        InputStream expectedFileStream = getClass().getClassLoader()
                .getResourceAsStream("frontend/expected/" + expectedFileName);

        Path tmpInputFilePath = Files.createTempFile(tmpDir.toPath(), "tmp",
                null);
        Files.copy(inFileStream, tmpInputFilePath,
                StandardCopyOption.REPLACE_EXISTING);

        frontendConverter.convertFile(tmpInputFilePath, useLit1,
                disableOptionalChaining);

        String expectedContent = new String(expectedFileStream.readAllBytes(),
                StandardCharsets.UTF_8);
        String actualContent = Files.readString(tmpInputFilePath,
                StandardCharsets.UTF_8);

        // TIP: Uncomment if you would like to update snapshots.
        // @formatter:off
        // Files.writeString(
        //         Path.of("src/test/resources/frontend/expected/" + expectedFileName),
        //         actualContent, StandardCharsets.UTF_8);
        // @formatter:on

        assertEquals(expectedContent, actualContent, "The output " + inFileName
                + " file does not match the expected one.");
    }
}
