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
package com.vaadin.flow.server;

import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.ReflectTools;

public class DevToolsTokenTest {

    @Rule
    public TemporaryFolder projectFolder = new TemporaryFolder();

    private VaadinService service;
    private DeploymentConfiguration configuration;
    private String initialToken;
    private String systemTempDir;

    @Before
    public void setUp() throws Exception {
        configuration = Mockito.mock(DeploymentConfiguration.class);
        Mockito.when(configuration.getProjectFolder())
                .thenReturn(projectFolder.getRoot());
        service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        initialToken = DevToolsToken.getToken();
        systemTempDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir",
                projectFolder.getRoot().getAbsolutePath());
    }

    @After
    public void tearDown() {
        overwriteToken(initialToken);
        System.setProperty("java.io.tmpdir", systemTempDir);
    }

    @Test
    public void init_tokenFileNotExising_createTokenFile() {
        DevToolsToken.init(service);
        Assert.assertEquals(initialToken, DevToolsToken.getToken());

        overwriteToken("EMPTY");

        // Token restored from file
        DevToolsToken.init(service);
        Assert.assertEquals(initialToken, DevToolsToken.getToken());
    }

    @Test
    public void init_nullProjectFolder_useInMemoryToken() {
        Mockito.when(configuration.getProjectFolder()).thenReturn(null);

        String testToken = UUID.randomUUID().toString();
        overwriteToken(testToken);
        DevToolsToken.init(service);
        Assert.assertEquals(testToken, DevToolsToken.getToken());
    }

    private void overwriteToken(String token) {
        try {
            ReflectTools.setJavaFieldValue(null,
                    DevToolsToken.class.getDeclaredField("randomDevToolsToken"),
                    token);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

}
