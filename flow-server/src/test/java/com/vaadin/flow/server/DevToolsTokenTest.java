/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
