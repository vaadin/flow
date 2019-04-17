/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.router.Route;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class NodeExecutorTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File importsFile;
    private File nodeModulesPath;

    private NodeExecutor node;

    private Map<Class<? extends Annotation>, Integer> annotationScanCount = new HashMap<>();

    @Before
    public void setup() throws Exception {

        File tmpRoot = temporaryFolder.getRoot();
        importsFile = new File(tmpRoot, "flow-imports.js");
        nodeModulesPath = new File(tmpRoot, "node_modules");

        ClassFinder classFinder = getClassFinder();
        ClassFinder classFinderSpy = Mockito.spy(getClassFinder());

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                Class<? extends Annotation> clazz = (Class<? extends Annotation>) invocation
                        .getArguments()[0];

                annotationScanCount.compute(clazz,
                        (k, v) -> v == null ? 1 : v + 1);
                
                return classFinder.getAnnotatedClasses(clazz);
            }
        }).when(classFinderSpy).getAnnotatedClasses(Mockito.any());

        node = new NodeExecutor.Builder(classFinderSpy, importsFile, tmpRoot,
                nodeModulesPath, true).build();

        createExpectedImports(importsFile.getParentFile(), nodeModulesPath);
    }

    @Test
    public void should_ScanAnnotations_Once() {
        node.execute();

        Assert.assertEquals("Route scanned more than once", 1,
                annotationScanCount.get(Route.class).intValue());
    }

}
