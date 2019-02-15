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
package com.vaadin.flow.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class DevModeServerTest {
    
    private DeploymentConfiguration configuration = Mockito
            .mock(DeploymentConfiguration.class);
    
    private static final String TEST_FILE = "webpack-out.test";
    private static final boolean isUnix = new File(DevModeServer.UNIX_SH).canExecute();
    
    @Before
    public void setup() throws IOException {
        Mockito.when(configuration.isProductionMode()).thenReturn(false);
        Mockito.when(configuration.isProductionMode()).thenReturn(false);

        new File(DevModeServer.WEBAPP_FOLDER).mkdirs();
        
        File serverFile = new File(DevModeServer.WEBPACK_SERVER);
        serverFile.getParentFile().mkdirs();
        serverFile.createNewFile();
        serverFile.setExecutable(true);
        if (isUnix) {
            Files.write(Paths.get(serverFile.toURI()), (
                "#!" + DevModeServer.UNIX_SH + "\n" + 
                "echo running $0 $* | tee -a " + TEST_FILE)
                .getBytes());
        }
        new File(DevModeServer.WEBPACK_CONFIG).createNewFile();
    }
    
    @After
    public void teardown() throws IOException {
        FileUtils.deleteDirectory(new File("node_modules"));
        FileUtils.deleteDirectory(new File(DevModeServer.WEBAPP_FOLDER));
        FileUtils.deleteQuietly(new File(DevModeServer.WEBPACK_CONFIG));
    }
    
    @Test
    public void should_CreateInstanceAndRunWebPack_When_DevModeAndNpmInstalled() {
        assertNotNull(DevModeServer.createInstance(configuration));
        if (isUnix) {
            DevModeServer.createInstance(configuration);
            assertTrue(new File(DevModeServer.WEBAPP_FOLDER + TEST_FILE).canRead());
        }
    }

    @Test
    public void should_NotCreateInstance_When_ProductionMode() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        assertNull(DevModeServer.createInstance(configuration));
    }

    @Test
    public void should_NotCreateInstance_When_BowerMode() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        assertNull(DevModeServer.createInstance(configuration));
    }

    @Test
    public void should_NotCreateInstance_When_WebpackNotInstalled() {
        new File(DevModeServer.WEBPACK_SERVER).delete();
        assertNull(DevModeServer.createInstance(configuration));
    }

    @Test
    public void should_NotCreateInstance_When_WebpackIsNotExecutable() {
        new File(DevModeServer.WEBPACK_SERVER).setExecutable(false);
        assertNull(DevModeServer.createInstance(configuration));
    }

    @Test
    public void should_NotCreateInstance_When_WebpackNotConfigured() {
        new File(DevModeServer.WEBPACK_CONFIG).delete();
        assertNull(DevModeServer.createInstance(configuration));
    }
    
    @Test
    public void should_HandleJavaScriptRequests() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "foo.js").when(request).getPathInfo();
        assertTrue(new DevModeServer().isDevModeRequest(request));
    }
    
    @Test
    public void should_NotHandleOtherRequests() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "foo.bar").when(request).getPathInfo();
        assertFalse(new DevModeServer().isDevModeRequest(request));
    }
    
    @Test(expected = ConnectException.class)
    public void should_ThrowAnException_When_WebpackNotRunning() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "/foo.js").when(request).getPathInfo();
        Mockito.doAnswer(invocation -> "/app").when(request).getServletPath();
        Mockito.doAnswer(invocation -> "GET").when(request).getMethod();
        Mockito.doAnswer(invocation -> Collections.enumeration(Arrays.asList("foo"))).when(request).getHeaderNames();
        Mockito.doAnswer(invocation -> "bar").when(request).getHeader("foo");
        assertFalse(new DevModeServer().serveDevModeRequest(request, null));
    }
}
