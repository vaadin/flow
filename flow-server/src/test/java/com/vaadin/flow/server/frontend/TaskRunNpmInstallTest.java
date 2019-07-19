/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;

import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;

import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

public class TaskRunNpmInstallTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private NodeUpdater nodeUpdater;

    private TaskRunNpmInstall task;

    private File npmFolder;

    private Logger logger = Mockito.mock(Logger.class);

    @Before
    public void setUp() throws IOException {
        npmFolder = temporaryFolder.newFolder();
        nodeUpdater = new NodeUpdater(Mockito.mock(ClassFinder.class),
                Mockito.mock(FrontendDependencies.class), npmFolder,
                new File("")) {

            @Override
            public void execute() {
            }

            @Override
            Logger log() {
                return logger;
            }

        };
        task = new TaskRunNpmInstall(nodeUpdater);

    }

    @Test
    public void runNpmInstall_emptyDir_npmInstallIsExecuted()
            throws ExecutionFailedException {
        File nodeModules = new File(npmFolder, NODE_MODULES);
        nodeModules.mkdir();
        nodeUpdater.modified = false;
        task.execute();

        Mockito.verify(logger).info(TaskRunNpmInstall.RUNNING_NPM_INSTALL);
    }

    @Test
    public void runNpmInstall_nonEmptyDir_npmInstallIsNotExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = new File(npmFolder, NODE_MODULES);
        nodeModules.mkdir();
        new File(nodeModules, "foo").createNewFile();
        nodeUpdater.modified = false;
        task.execute();

        Mockito.verify(logger).info(TaskRunNpmInstall.SKIPPING_NPM_INSTALL);
    }

    @Test
    public void runNpmInstall_dirContainsOnlyFlowNpmPackage_npmInstallIsNotExecuted()
            throws IOException, ExecutionFailedException {
        File nodeModules = new File(npmFolder, NODE_MODULES);
        nodeModules.mkdir();
        new File(nodeModules, "@vaadin/flow-frontend/").mkdirs();
        nodeUpdater.modified = false;
        task.execute();

        Mockito.verify(logger).info(TaskRunNpmInstall.RUNNING_NPM_INSTALL);
    }

    @Test
    public void runNpmInstall_modified_npmInstallIsExecuted()
            throws ExecutionFailedException {
        nodeUpdater.modified = true;
        task.execute();

        Mockito.verify(logger).info(TaskRunNpmInstall.RUNNING_NPM_INSTALL);
    }
}
