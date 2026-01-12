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
package com.vaadin.flow.polymer2lit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.internal.FrontendUtils.CommandExecutionException;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;
import com.vaadin.flow.testcategory.SlowTests;

@Category(SlowTests.class)
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
    public void teardown() throws IOException {
        frontendConverter.close();
    }

    @Test
    public void basicBinding() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("basic-bindings.js");
    }

    @Test
    public void basicBinding_lit1() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("basic-bindings.js",
                "basic-bindings-with-lit1.js", true, false);
    }

    @Test
    public void computedProperty() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("computed-property.js");
    }

    @Test
    public void disabledUsingMethod() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("disabled-using-method.js");
    }

    @Test
    public void domIf() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("dom-if.js");
    }

    @Test
    public void domRepeat() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("dom-repeat.js");
    }

    @Test
    public void domRepeat_disabledOptionalChaining() throws IOException,
            InterruptedException, CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("dom-repeat.js",
                "dom-repeat-disabled-optional-chaining.js", false, true);
    }

    @Test
    public void eventHandlers() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("event-handlers.js");
    }

    @Test
    public void gridColumns() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("grid-columns.js");
    }

    @Test
    public void inlineStyles() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("inline-styles.js");
    }

    @Test
    public void lightDom() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("light-dom.js");
    }

    @Test
    public void multipleBindings() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("multiple-bindings.js");
    }

    @Test
    public void nestedDomRepeat() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("nested-dom-repeat.js");
    }

    @Test
    public void nestedDomRepeatDisabledOptionalChaining() throws IOException,
            InterruptedException, CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("nested-dom-repeat.js",
                "nested-dom-repeat-disabled-optional-chaining.js", false, true);
    }

    @Test
    public void orderCardFromBakery() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("order-card-from-bakery.js");
    }

    @Test
    public void readyCallback() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("ready-callback.js");
    }

    @Test
    public void simpleObserver() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("simple-observer.js");
    }

    @Test
    public void subProperties() throws IOException, InterruptedException,
            CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("sub-properties.js");
    }

    @Test
    public void subProperties_disabledOptionalChaining() throws IOException,
            InterruptedException, CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne("sub-properties.js",
                "sub-properties-with-disabled-optional-chaining.js", false,
                true);
    }

    @Test
    public void thisDollarMappedElementIds() throws IOException,
            InterruptedException, CommandExecutionException {
        convertFile_outputFileMatchesExpectedOne(
                "this-dollar-mapped-element-ids.js");
    }

    @Test
    public void twoWayBinding() throws IOException, InterruptedException,
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

        Path tmpInputFilePath = tmpDir.newFile().toPath();
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

        Assert.assertEquals(
                "The output " + inFileName
                        + " file does not match the expected one.",
                expectedContent, actualContent);
    }
}
