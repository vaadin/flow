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
import java.net.URI;
import java.net.URISyntaxException;

import org.mockito.Mockito;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.utils.LookupImpl;

import static com.vaadin.flow.server.frontend.installer.Platform.DEFAULT_NODEJS_DOWNLOAD_ROOT;

public class TestUtil {
    public static void stubPluginAdapterBase(PluginAdapterBase adapter,
            File baseDir) throws URISyntaxException {
        ClassFinder classFinder = Mockito.mock(ClassFinder.class);
        LookupImpl lookup = Mockito.spy(new LookupImpl(classFinder));
        Mockito.when(adapter.createLookup(Mockito.any())).thenReturn(lookup);
        Mockito.doReturn(classFinder).when(lookup).lookup(ClassFinder.class);

        Mockito.when(adapter.projectBaseDirectory())
                .thenReturn(baseDir.toPath());
        Mockito.when(adapter.npmFolder()).thenReturn(baseDir);
        Mockito.when(adapter.nodeVersion())
                .thenReturn(FrontendTools.DEFAULT_NODE_VERSION);
        Mockito.when(adapter.nodeDownloadRoot())
                .thenReturn(URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));
        Mockito.when(adapter.frontendDirectory()).thenReturn(
                new File(baseDir, FrontendUtils.DEFAULT_FRONTEND_DIR));
        Mockito.when(adapter.buildFolder()).thenReturn(Constants.TARGET);
        Mockito.when(adapter.javaSourceFolder())
                .thenReturn(new File(baseDir, "src/main/java"));
        Mockito.when(adapter.javaResourceFolder())
                .thenReturn(new File(baseDir, "src/main/resources"));
        Mockito.when(adapter.openApiJsonFile())
                .thenReturn(new File(new File(baseDir, Constants.TARGET),
                        "classes/com/vaadin/hilla/openapi.json"));
        Mockito.when(adapter.getClassFinder())
                .thenReturn(new ClassFinder.DefaultClassFinder(
                        TestUtil.class.getClassLoader()));
    }
}
