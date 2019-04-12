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

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.server.frontend.ClassPathIntrospector.ClassFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NodeExecutorTest extends NodeUpdateTestBase
        implements ClassFinder {

    private File importsFile;
    private File nodeModulesPath;

    private NodeExecutor node;

    private Map<Class<? extends Annotation>, Integer> annotationScanCount = new HashMap<>();

    @Before
    public void setup() throws Exception {

        File tmpRoot = temporaryFolder.getRoot();
        importsFile = new File(tmpRoot, "flow-imports.js");
        nodeModulesPath = new File(tmpRoot, "node_modules");

        node = new NodeExecutor.Builder(
                createProxyClassFinder(getClassFinder(), this), importsFile,
                tmpRoot, nodeModulesPath, true).build();

        createExpectedImports(importsFile.getParentFile(), nodeModulesPath);
    }

    @Test
    public void should_ScanAnnotations_Once() {
        node.execute();

        Assert.assertEquals("NpmPackage scanned more than once", 1,
                annotationScanCount.get(NpmPackage.class).intValue());
        Assert.assertEquals("HtmlImport scanned more than once", 1,
                annotationScanCount.get(HtmlImport.class).intValue());
        Assert.assertEquals("JsModule scanned more than once", 1,
                annotationScanCount.get(JsModule.class).intValue());
        Assert.assertEquals("JavaScript scanned more than once", 1,
                annotationScanCount.get(JavaScript.class).intValue());
    }

    @Override
    public Set<Class<?>> getAnnotatedClasses(
            Class<? extends Annotation> clazz) {

        annotationScanCount.compute(clazz, (k, v) -> v == null ? 1 : v + 1);

        return null;
    }

    @Override
    public URL getResource(String name) {
        return null;
    }

    @Override
    public <T> Class<T> loadClass(String name) throws ClassNotFoundException {
        return null;
    }

    @Override
    public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
        return null;
    }
}