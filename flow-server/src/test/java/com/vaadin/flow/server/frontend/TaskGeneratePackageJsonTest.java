/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import elemental.json.JsonObject;

public class TaskGeneratePackageJsonTest {
    private TaskGeneratePackageJson task;
    private File flowResourcesFolder;
    private File npmFolder;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        flowResourcesFolder = temporaryFolder.newFolder();

        task = Mockito.spy(new TaskGeneratePackageJson(npmFolder, null, flowResourcesFolder));
        
        Mockito.doReturn(null).when(task).getPackageJson();
        Mockito.doReturn(false).when(task).updateDefaultDependencies(Mockito.any());
        Mockito.doReturn(null).when(task).writePackageFile(Mockito.any());
        Mockito.doReturn(null).when(task).writeFormResourcesPackageFile(Mockito.any());
        Mockito.doReturn(null).when(task).writeResourcesPackageFile(Mockito.any());
    }

    @After
    public void tearDown() throws IOException {
        npmFolder.delete();
        flowResourcesFolder.delete();
    }

    @Test
    public void should_call_riteFormResourcesPackageFile() throws IOException {
        JsonObject resourcePackageJson = Mockito.mock(JsonObject.class);
        JsonObject formReSourcePackageJson = Mockito.mock(JsonObject.class);
        Mockito.doReturn(resourcePackageJson).when(task).getResourcesPackageJson();
        Mockito.doReturn(formReSourcePackageJson).when(task).getFormResourcesPackageJson();

        task.execute();
        

        verify(task).writeResourcesPackageFile(resourcePackageJson);
        verify(task).writeFormResourcesPackageFile(formReSourcePackageJson);
    }
}
