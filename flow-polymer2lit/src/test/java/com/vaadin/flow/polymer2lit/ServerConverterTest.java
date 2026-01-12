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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ServerConverterTest {
    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private ServerConverter serverConverter;

    @Before
    public void init() throws IOException {
        serverConverter = new ServerConverter();
    }

    @Test
    public void noModel() throws IOException {
        convertFile_outputFileMatchesExpectedOne("NoModel.java");
    }

    @Test
    public void basicGettersSetters() throws IOException {
        convertFile_outputFileMatchesExpectedOne("BasicGettersSetters.java");
    }

    private void convertFile_outputFileMatchesExpectedOne(String fileName)
            throws IOException {
        InputStream inputFileStream = getClass().getClassLoader()
                .getResourceAsStream("server/in/" + fileName);
        InputStream expectedFileStream = getClass().getClassLoader()
                .getResourceAsStream("server/expected/" + fileName);

        Path tmpInputFilePath = tmpDir.newFile().toPath();
        Files.copy(inputFileStream, tmpInputFilePath,
                StandardCopyOption.REPLACE_EXISTING);

        serverConverter.convertFile(tmpInputFilePath);

        String expectedContent = new String(expectedFileStream.readAllBytes(),
                StandardCharsets.UTF_8);
        String actualContent = Files.readString(tmpInputFilePath,
                StandardCharsets.UTF_8);

        // TIP: Uncomment if you would like to update snapshots.
        // @formatter:off
        // Files.writeString(
        //         Path.of("src/test/resources/server/expected/" + fileName),
        //         actualContent, StandardCharsets.UTF_8);
        // @formatter:on

        Assert.assertEquals(
                "The output " + fileName
                        + " file does not match the expected one.",
                expectedContent, actualContent);
    }
}
