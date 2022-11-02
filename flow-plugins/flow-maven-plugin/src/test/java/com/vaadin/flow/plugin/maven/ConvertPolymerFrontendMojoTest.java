/*
 * Copyright 2000-2022 Vaadin Ltd.
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
 *
 */
package com.vaadin.flow.plugin.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.plugin.TestUtils;

public class ConvertPolymerFrontendMojoTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private final ConvertPolymerFrontendMojo mojo = Mockito
            .spy(new ConvertPolymerFrontendMojo());

    @Before
    public void init() throws DependencyResolutionRequiredException,
            IllegalAccessException {
        TestUtils.mockPluginAdapter(mojo, tmpDir.getRoot());
    }

    @Test
    public void executeGoal() throws MojoFailureException {
        mojo.execute();
    }

    @Test
    public void setCustomGlobParameter_executeGoal()
            throws IllegalAccessException, MojoFailureException {
        ReflectionUtils.setVariableValueInObject(mojo, "glob", "");

        mojo.execute();
    }

    @Test
    public void setUseLit1Parameter_executeGoal()
            throws IllegalAccessException, MojoFailureException {
        ReflectionUtils.setVariableValueInObject(mojo, "useLit1", true);

        mojo.execute();
    }

    @Test
    public void setDisableOptionalChainingParameter_executeGoal()
            throws IllegalAccessException, MojoFailureException {
        ReflectionUtils.setVariableValueInObject(mojo,
                "disableOptionalChaining", true);

        mojo.execute();
    }
}
