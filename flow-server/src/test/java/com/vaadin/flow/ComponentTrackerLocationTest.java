/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow;

import java.io.File;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.server.AbstractConfiguration;

public class ComponentTrackerLocationTest {

    @Test
    public void findJavaFile_simpleClass() {
        File fakeSrcDir = new File("src");
        AbstractConfiguration configuration = Mockito
                .mock(AbstractConfiguration.class);
        Mockito.when(configuration.getJavaSourceFolder())
                .thenReturn(fakeSrcDir);

        ComponentTracker.Location location = new ComponentTracker.Location(
                "com.example.app.MyClass", "MyClass.java", "whoCares", 99);
        File expectedFile = fakeSrcDir.toPath()
                .resolve(Path.of("com", "example", "app", "MyClass.java"))
                .toFile();

        File javaFile = location.findJavaFile(configuration);
        Assert.assertEquals(expectedFile, javaFile);
    }

    @Test
    public void findJavaFile_simpleClass_dollarInPackage() {
        File fakeSrcDir = new File("src");
        AbstractConfiguration configuration = Mockito
                .mock(AbstractConfiguration.class);
        Mockito.when(configuration.getJavaSourceFolder())
                .thenReturn(fakeSrcDir);

        ComponentTracker.Location location = new ComponentTracker.Location(
                "com.exa$mple.app.MyClass", "MyClass.java", "whoCares", 99);
        File expectedFile = fakeSrcDir.toPath()
                .resolve(Path.of("com", "exa$mple", "app", "MyClass.java"))
                .toFile();

        File javaFile = location.findJavaFile(configuration);
        Assert.assertEquals(expectedFile, javaFile);
    }

    @Test
    public void findJavaFile_simpleClass_dollarInName() {
        File fakeSrcDir = new File("src");
        AbstractConfiguration configuration = Mockito
                .mock(AbstractConfiguration.class);
        Mockito.when(configuration.getJavaSourceFolder())
                .thenReturn(fakeSrcDir);

        ComponentTracker.Location location = new ComponentTracker.Location(
                "com.example.app.MyClass$NotInner", "MyClass$NotInner.java",
                "whoCares", 99);
        File expectedFile = fakeSrcDir.toPath().resolve(
                Path.of("com", "example", "app", "MyClass$NotInner.java"))
                .toFile();

        File javaFile = location.findJavaFile(configuration);
        Assert.assertEquals(expectedFile, javaFile);
    }

    @Test
    public void findJavaFile_innerClass() {
        File fakeSrcDir = new File("src");
        AbstractConfiguration configuration = Mockito
                .mock(AbstractConfiguration.class);
        Mockito.when(configuration.getJavaSourceFolder())
                .thenReturn(fakeSrcDir);

        ComponentTracker.Location location = new ComponentTracker.Location(
                "com.example.app.MyClass$Inner", "MyClass.java", "whoCares",
                99);
        File expectedFile = fakeSrcDir.toPath()
                .resolve(Path.of("com", "example", "app", "MyClass.java"))
                .toFile();

        File javaFile = location.findJavaFile(configuration);
        Assert.assertEquals(expectedFile, javaFile);
    }

    @Test
    public void findJavaFile_nestedInnerClass() {
        File fakeSrcDir = new File("src");
        AbstractConfiguration configuration = Mockito
                .mock(AbstractConfiguration.class);
        Mockito.when(configuration.getJavaSourceFolder())
                .thenReturn(fakeSrcDir);

        ComponentTracker.Location location = new ComponentTracker.Location(
                "com.example.app.MyClass$Deep$Nested$Inner", "MyClass.java",
                "whoCares", 99);
        File expectedFile = fakeSrcDir.toPath()
                .resolve(Path.of("com", "example", "app", "MyClass.java"))
                .toFile();

        File javaFile = location.findJavaFile(configuration);
        Assert.assertEquals(expectedFile, javaFile);
    }

}
