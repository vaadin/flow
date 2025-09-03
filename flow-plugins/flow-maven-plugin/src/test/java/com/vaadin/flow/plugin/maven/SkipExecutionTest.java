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
package com.vaadin.flow.plugin.maven;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.setProject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SkipExecutionTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PrepareFrontendMojo mojo;
    private Log mockLog;
    private File projectBase;

    @Before
    public void setup() throws Exception {
        mojo = new PrepareFrontendMojo();
        mockLog = Mockito.mock(Log.class);
        projectBase = temporaryFolder.getRoot();

        // Set up the mojo with basic configuration
        ReflectionUtils.setVariableValueInObject(mojo, "projectBasedir", projectBase);
        ReflectionUtils.setVariableValueInObject(mojo, "frontendDirectory",
                new File(projectBase, "src/main/frontend"));
        
        setProject(mojo, projectBase);
        
        // Use reflection to set the mock logger
        ReflectionUtils.setVariableValueInObject(mojo, "log", mockLog);
    }

    @Test
    public void testSkipExecutionWhenVaadinSkipIsTrue() throws Exception {
        // Set the skip parameter to true
        ReflectionUtils.setVariableValueInObject(mojo, "skip", true);

        // Execute the mojo
        mojo.execute();

        // Verify that the skip message was logged
        verify(mockLog).info("Skipping execution because vaadin.skip=true");
    }

    @Test
    public void testNormalExecutionWhenVaadinSkipIsFalse() throws Exception {
        // Set the skip parameter to false (default)
        ReflectionUtils.setVariableValueInObject(mojo, "skip", false);

        try {
            // Execute the mojo - this might fail due to missing dependencies in test env
            mojo.execute();
        } catch (Exception e) {
            // Expected - we're just testing that skip message is not logged
        }

        // Verify that the skip message was NOT logged
        verify(mockLog, never()).info("Skipping execution because vaadin.skip=true");
    }
}