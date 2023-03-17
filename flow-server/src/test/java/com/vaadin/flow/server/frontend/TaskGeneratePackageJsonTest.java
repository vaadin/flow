/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import static com.vaadin.flow.server.Constants.TARGET;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;

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

        task = Mockito.spy(new TaskGeneratePackageJson(npmFolder, null,
                flowResourcesFolder, TARGET, Mockito.mock(FeatureFlags.class)));

        Mockito.doReturn(null).when(task).getPackageJson();
        Mockito.doReturn(false).when(task)
                .updateDefaultDependencies(Mockito.any());
        Mockito.doReturn(null).when(task).writePackageFile(Mockito.any());
        Mockito.doReturn(null).when(task)
                .writeResourcesPackageFile(Mockito.any());
    }

    @After
    public void tearDown() throws IOException {
        npmFolder.delete();
        flowResourcesFolder.delete();
    }

    @Test
    public void should_witeFlowAndFormResourcesPackageFiles()
            throws IOException {
        JsonObject resourcePackageJson = Mockito.mock(JsonObject.class);
        Mockito.doReturn(resourcePackageJson).when(task)
                .getResourcesPackageJson();

        task.execute();

        verify(task).writeResourcesPackageFile(resourcePackageJson);
    }
}
