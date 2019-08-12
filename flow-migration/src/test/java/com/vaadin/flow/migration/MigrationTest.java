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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

public class MigrationTest {

    private MigrationConfiguration configuration = Mockito.mock(MigrationConfiguration.class);

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

    @Test
    public void migratePassesHappyPath()
            throws MigrationFailureException, MigrationToolsException,
            IOException {
        File sourcesFolder = makeTempDirectoryStructure();
        File targetFolder = temporaryFolder.newFolder();
        targetFolder.mkdirs();

        Mockito.when(configuration.getBaseDirectory())
                .thenReturn(Paths.get(sourcesFolder.getPath(), "foo").toFile());
        Mockito.when(configuration.getTempMigrationFolder()).
                thenReturn(targetFolder);
        Mockito.when(configuration.getAnnotationRewriteStrategy())
                .thenReturn(AnnotationsRewriteStrategy.SKIP);
        Mockito.when(configuration.isKeepOriginalFiles()).thenReturn(true);
        Mockito.when(configuration.getClassFinder())
                .thenReturn(Mockito.mock(ClassFinder.class));
        Mockito.when(configuration.getJavaSourceDirectories()).thenReturn(
                new File[] {
                        Paths.get(sourcesFolder.getPath(), "bar").toFile() });
        Mockito.when(configuration.getCompiledClassDirectory()).thenReturn(
                Paths.get(sourcesFolder.getPath(), "foobar").toFile());

        Migration migration = new Migration(configuration) {
            @Override
            protected boolean executeProcess(List<String> command,
                    String errorMsg, String successMsg, String exceptionMsg) {
                // Do actually install migration tools as else we will fail the build
                // Skip actual execution of other commands.
                if (errorMsg.equals("Couldn't install migration tools")) {
                    return super.executeProcess(command, errorMsg, successMsg,
                            exceptionMsg);
                }
                return true;
            }
        };
        migration.migrate();
    }

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File makeTempDirectoryStructure() throws IOException {
        File folder = temporaryFolder.newFolder();
        folder.mkdirs();
        Files.createDirectories(
                Paths.get(folder.getAbsolutePath(), "foo", "src", "main",
                        "webapp"));
        Files.createDirectories(
                Paths.get(folder.getAbsolutePath(), "bar", "src", "main",
                        "java"));
        Files.createDirectories(Paths.get(folder.getAbsolutePath(), "foobar"));
        return folder;
    }
}
