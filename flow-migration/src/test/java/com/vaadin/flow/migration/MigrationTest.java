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
package com.vaadin.flow.migration;

import java.io.File;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

public class MigrationTest {

    private Configuration configuration = Mockito.mock(Configuration.class);

    @Test(expected = IllegalArgumentException.class)
    public void createMigration_noBaseDir_throw() {
        new Migration(configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createMigration_baseDirIsSet_noClassFinder_throw() {
        Mockito.when(configuration.getClassFinder())
                .thenReturn(Mockito.mock(ClassFinder.class));
        Mockito.when(configuration.getJavaSourceDirectories())
                .thenReturn(new File[] { new File("bar") });
        Mockito.when(configuration.getCompiledClassDirectory())
                .thenReturn(new File("foobar"));
        new Migration(configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createMigration_noJavaSourceDirs_throw() {
        Mockito.when(configuration.getBaseDirectory())
                .thenReturn(new File("foo"));
        Mockito.when(configuration.getClassFinder())
                .thenReturn(Mockito.mock(ClassFinder.class));
        Mockito.when(configuration.getCompiledClassDirectory())
                .thenReturn(new File("foobar"));
        new Migration(configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createMigration_emptyJavaSourceDirs_throw() {
        Mockito.when(configuration.getBaseDirectory())
                .thenReturn(new File("foo"));
        Mockito.when(configuration.getClassFinder())
                .thenReturn(Mockito.mock(ClassFinder.class));
        Mockito.when(configuration.getCompiledClassDirectory())
                .thenReturn(new File("foobar"));
        Mockito.when(configuration.getJavaSourceDirectories())
                .thenReturn(new File[] {});
        new Migration(configuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createMigration_noCompiledClassDir_throw() {
        Mockito.when(configuration.getBaseDirectory())
                .thenReturn(new File("foo"));
        Mockito.when(configuration.getClassFinder())
                .thenReturn(Mockito.mock(ClassFinder.class));
        Mockito.when(configuration.getJavaSourceDirectories())
                .thenReturn(new File[] { new File("bar") });
        new Migration(configuration);
    }

    @Test
    public void createMigration_allRequiredConfigParamsAreSet_doesnThrow() {
        Mockito.when(configuration.getBaseDirectory())
                .thenReturn(new File("foo"));
        Mockito.when(configuration.getClassFinder())
                .thenReturn(Mockito.mock(ClassFinder.class));
        Mockito.when(configuration.getJavaSourceDirectories())
                .thenReturn(new File[] { new File("bar") });
        Mockito.when(configuration.getCompiledClassDirectory())
                .thenReturn(new File("foobar"));

        new Migration(configuration);
    }
}
