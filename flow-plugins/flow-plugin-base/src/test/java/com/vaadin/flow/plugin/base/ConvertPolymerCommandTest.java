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
package com.vaadin.flow.plugin.base;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.polymer2lit.FrontendConverter;
import com.vaadin.flow.polymer2lit.ServerConverter;
import com.vaadin.flow.server.frontend.FrontendUtils.CommandExecutionException;

public class ConvertPolymerCommandTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Mock
    private MockedConstruction<FrontendConverter> frontendConverterMock;

    @Mock
    private MockedConstruction<ServerConverter> serverConverterMock;

    @Mock
    private PluginAdapterBase adapter;

    @Mock
    private ConvertPolymerCommand command;

    private AutoCloseable closeable;

    @Before
    public void init()
            throws IOException, URISyntaxException, IllegalAccessException {
        closeable = MockitoAnnotations.openMocks(this);
        TestUtil.stubPluginAdapterBase(adapter, tmpDir.getRoot());

        tmpDir.newFile("component.js");
        tmpDir.newFolder("nested");
        tmpDir.newFile("nested/component.js");
        tmpDir.newFolder("node_modules");
        tmpDir.newFile("node_modules/component.js");
        tmpDir.newFile("Component.java");
        tmpDir.newFile("nested/Component.java");
    }

    @After
    public void teardown() throws Exception {
        closeable.close();
    }

    @Test
    public void execute() throws URISyntaxException, IOException,
            InterruptedException, CommandExecutionException {
        try (ConvertPolymerCommand command = new ConvertPolymerCommand(adapter,
                null, false, false)) {
            command.execute();

            FrontendConverter frontendConverter = frontendConverterMock
                    .constructed().get(0);
            Mockito.verify(frontendConverter)
                    .convertFile(getTmpFilePath("component.js"), false, false);
            Mockito.verify(frontendConverter).convertFile(
                    getTmpFilePath("nested/component.js"), false, false);
            Mockito.verify(frontendConverter, Mockito.never()).convertFile(
                    getTmpFilePath("node_modules/component.js"), false, false);
            Mockito.verify(frontendConverter, Mockito.never()).convertFile(
                    getTmpFilePath("Component.java"), false, false);
            Mockito.verify(frontendConverter, Mockito.never()).convertFile(
                    getTmpFilePath("nested/Component.java"), false, false);

            ServerConverter serverConverter = serverConverterMock.constructed()
                    .get(0);
            Mockito.verify(serverConverter)
                    .convertFile(getTmpFilePath("Component.java"));
            Mockito.verify(serverConverter)
                    .convertFile(getTmpFilePath("nested/Component.java"));
            Mockito.verify(serverConverter, Mockito.never())
                    .convertFile(getTmpFilePath("component.js"));
            Mockito.verify(serverConverter, Mockito.never())
                    .convertFile(getTmpFilePath("nested/component.js"));
        }
    }

    @Test
    public void setSpecificFrontendFile_execute() throws URISyntaxException,
            IOException, InterruptedException, CommandExecutionException {
        try (ConvertPolymerCommand command = new ConvertPolymerCommand(adapter,
                "/nested/component.js", false, false)) {
            command.execute();

            FrontendConverter frontendConverter = frontendConverterMock
                    .constructed().get(0);
            Mockito.verify(frontendConverter, Mockito.never())
                    .convertFile(getTmpFilePath("component.js"), false, false);
            Mockito.verify(frontendConverter).convertFile(
                    getTmpFilePath("nested/component.js"), false, false);

            ServerConverter serverConverter = serverConverterMock.constructed()
                    .get(0);
            Mockito.verify(serverConverter, Mockito.never())
                    .convertFile(getTmpFilePath("Component.java"));
            Mockito.verify(serverConverter, Mockito.never())
                    .convertFile(getTmpFilePath("nested/Component.java"));
        }
    }

    @Test
    public void setSpecificServerFile_execute() throws URISyntaxException,
            IOException, InterruptedException, CommandExecutionException {
        try (ConvertPolymerCommand command = new ConvertPolymerCommand(adapter,
                "/nested/Component.java", false, false)) {
            command.execute();

            FrontendConverter frontendConverter = frontendConverterMock
                    .constructed().get(0);
            Mockito.verify(frontendConverter, Mockito.never())
                    .convertFile(getTmpFilePath("component.js"), true, false);
            Mockito.verify(frontendConverter, Mockito.never()).convertFile(
                    getTmpFilePath("nested/component.js"), true, false);

            ServerConverter serverConverter = serverConverterMock.constructed()
                    .get(0);
            Mockito.verify(serverConverter, Mockito.never())
                    .convertFile(getTmpFilePath("Component.java"));
            Mockito.verify(serverConverter)
                    .convertFile(getTmpFilePath("nested/Component.java"));
        }
    }

    @Test
    public void useLit1_execute() throws URISyntaxException, IOException,
            InterruptedException, CommandExecutionException {
        try (ConvertPolymerCommand command = new ConvertPolymerCommand(adapter,
                null, true, false)) {
            command.execute();

            FrontendConverter frontendConverter = frontendConverterMock
                    .constructed().get(0);
            Mockito.verify(frontendConverter)
                    .convertFile(getTmpFilePath("component.js"), true, false);
            Mockito.verify(frontendConverter).convertFile(
                    getTmpFilePath("nested/component.js"), true, false);
        }
    }

    @Test
    public void disableOptionalChaining_execute() throws URISyntaxException,
            IOException, InterruptedException, CommandExecutionException {
        try (ConvertPolymerCommand command = new ConvertPolymerCommand(adapter,
                null, false, true)) {
            command.execute();

            FrontendConverter frontendConverter = frontendConverterMock
                    .constructed().get(0);
            Mockito.verify(frontendConverter)
                    .convertFile(getTmpFilePath("component.js"), false, true);
            Mockito.verify(frontendConverter).convertFile(
                    getTmpFilePath("nested/component.js"), false, true);
        }
    }

    private Path getTmpFilePath(String path) {
        return new File(tmpDir.getRoot(), path).toPath();
    }
}
