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
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.commons.cli.ParseException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

public class MigrationToolTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static class TestMigrationTool extends MigrationTool {

        private MigrationConfiguration conf;

        @Override
        protected void doMigration(MigrationConfiguration configuration) {
            conf = configuration;
        }
    }

    private TestMigrationTool tool = new TestMigrationTool();

    @Test
    public void passRequiredArguments_configurationIsPassed()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });
    }

    @Test
    public void noBaseDir_throw()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        exception.expect(CommandArgumentException.class);
        exception.expectCause(Matchers.any(ParseException.class));
        tool.runMigration(new String[] { "-src", "barSrcs", "-c",
                makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });
    }

    @Test
    public void noSourceRoot_throw()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        exception.expect(CommandArgumentException.class);
        exception.expectCause(Matchers.any(ParseException.class));
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-c",
                makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });
    }

    @Test
    public void noClassesDir_throw()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        exception.expect(CommandArgumentException.class);
        exception.expectCause(Matchers.any(ParseException.class));
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-d", makeSomeFolder().toURI().toURL().toExternalForm() });
    }

    @Test
    public void noDependeciesUrls_throw()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        exception.expect(CommandArgumentException.class);
        exception.expectCause(Matchers.any(ParseException.class));
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), });
    }

    @Test
    public void passBadDependencyUrl_throw()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        exception.expect(CommandArgumentException.class);
        exception.expectCause(Matchers.any(MalformedURLException.class));
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-d", "foo" });
    }

    @Test
    public void passBaseDir_baseDirIsConfigured()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals("fooBaseDir",
                tool.conf.getBaseDirectory().getPath());

        tool.conf = null;

        tool.runMigration(new String[] { "--baseDir", "barBaseDir", "-src",
                "barSrcs", "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals("barBaseDir",
                tool.conf.getBaseDirectory().getPath());
    }

    @Test
    public void passCompiledClassesDir_compiledClassesDirIsConfigured()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        File compileClasessDir = makeSomeFolder();
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", compileClasessDir.getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals(compileClasessDir,
                tool.conf.getCompiledClassDirectory());

        tool.conf = null;

        compileClasessDir = makeSomeFolder();

        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "--classesDir", compileClasessDir.getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals(compileClasessDir,
                tool.conf.getCompiledClassDirectory());
    }

    @Test
    public void passIgnoreModulizerError_theValueIsConfigured()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-stopOnError", "-b", "fooBaseDir",
                "-src", "barSrcs", "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertFalse(tool.conf.isIgnoreModulizerErrors());

        tool.conf = null;

        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertTrue(tool.conf.isIgnoreModulizerErrors());

        tool.conf = null;

        tool.runMigration(new String[] { "-se", "-b", "fooBaseDir", "-src",
                "barSrcs", "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertFalse(tool.conf.isIgnoreModulizerErrors());
    }

    @Test
    public void passKeepOriginal_theValueIsConfigured()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-keepOriginal", "-b", "fooBaseDir",
                "-src", "barSrcs", "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertTrue(tool.conf.isKeepOriginalFiles());

        tool.conf = null;

        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertFalse(tool.conf.isKeepOriginalFiles());

        tool.conf = null;

        tool.runMigration(new String[] { "-ko", "-b", "fooBaseDir", "-src",
                "barSrcs", "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertTrue(tool.conf.isKeepOriginalFiles());
    }

    @Test
    public void passResourcesDirs_resourcesDirsAreConfigured()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(
                new String[] { "-res", "fooBarRes", "-b", "fooBaseDir", "-src",
                        "barSrcs", "-c", makeSomeFolder().getPath(), "-d",
                        makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals(1, tool.conf.getResourceDirectories().length);
        Assert.assertEquals("fooBarRes",
                tool.conf.getResourceDirectories()[0].getPath());

        tool.conf = null;

        tool.runMigration(new String[] { "--resourcesDirs", "barFooRes", "-res",
                "baz", "-b", "fooBaseDir", "-src", "barSrcs", "-c",
                makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals(2, tool.conf.getResourceDirectories().length);
        Assert.assertEquals("barFooRes",
                tool.conf.getResourceDirectories()[0].getPath());
        Assert.assertEquals("baz",
                tool.conf.getResourceDirectories()[1].getPath());
    }

    @Test
    public void dontPassResourcesDir_resourcesDirIsNotConfigured_noException()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertNull(tool.conf.getResourceDirectories());
    }

    @Test
    public void passMigrationDir_migrationDirIsConfigured()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-md", "migrationFoo", "-b",
                "fooBaseDir", "-src", "barSrcs", "-c",
                makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals("migrationFoo",
                tool.conf.getTempMigrationFolder().getPath());

        tool.conf = null;

        tool.runMigration(new String[] { "--migrationDir", "fooMigration", "-b",
                "fooBaseDir", "-src", "barSrcs", "-c",
                makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals("fooMigration",
                tool.conf.getTempMigrationFolder().getPath());

    }

    @Test
    public void dontPassMigrationDir_MigrationDirIsNotConfigured_noException()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertNull(tool.conf.getTempMigrationFolder());
    }

    @Test
    public void passTargetDir_migrationTargetIsConfigured()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-t", "targetFoo", "-b", "fooBaseDir",
                "-src", "barSrcs", "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals("targetFoo",
                tool.conf.getTargetDirectory().getPath());

        tool.conf = null;

        tool.runMigration(new String[] { "--targetDir", "fooTarget", "-b",
                "fooBaseDir", "-src", "barSrcs", "-c",
                makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals("fooTarget",
                tool.conf.getTargetDirectory().getPath());

    }

    @Test
    public void dontPassTargetnDir_MigrationDirIsNotConfigured_noException()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertNull(tool.conf.getTargetDirectory());
    }

    @Test
    public void passJavaSourceDirs_sourceDirsAreConfigured()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals(1, tool.conf.getJavaSourceDirectories().length);
        Assert.assertEquals("barSrcs",
                tool.conf.getJavaSourceDirectories()[0].getPath());

        tool.conf = null;

        tool.runMigration(new String[] { "--sourceDirs", "fooSrc", "-b",
                "fooBaseDir", "-src", "barSrcs", "-c",
                makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals(2, tool.conf.getJavaSourceDirectories().length);
        Assert.assertEquals("fooSrc",
                tool.conf.getJavaSourceDirectories()[0].getPath());
        Assert.assertEquals("barSrcs",
                tool.conf.getJavaSourceDirectories()[1].getPath());
    }

    @Test
    public void passDpeUrls_classFinderIsConfigured()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException,
            URISyntaxException {
        File depDir = makeSomeFolder();
        File file = new File(depDir, "foo");
        file.createNewFile();

        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-d",
                depDir.toURI().toURL().toExternalForm() });

        ClassFinder classFinder = tool.conf.getClassFinder();
        Assert.assertNotNull(classFinder);

        Assert.assertEquals(file,
                new File(classFinder.getResource("foo").toURI()));

        tool.conf = null;

        tool.runMigration(new String[] { "-b", "fooBaseDir", "-src", "barSrcs",
                "-c", makeSomeFolder().getPath(), "-depUrls",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertNotNull(tool.conf.getClassFinder());

    }

    @Test
    public void passAnnotationRewriteStrategy_strategyIsConfigured()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-ars", "SKIP", "-t", "targetFoo",
                "-b", "fooBaseDir", "-src", "barSrcs", "-c",
                makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals(AnnotationsRewriteStrategy.SKIP,
                tool.conf.getAnnotationRewriteStrategy());

        tool.conf = null;

        tool.runMigration(new String[] { "--annRewrite", "SKIP_ON_ERROR", "-t",
                "targetFoo", "-b", "fooBaseDir", "-src", "barSrcs", "-c",
                makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals(AnnotationsRewriteStrategy.SKIP_ON_ERROR,
                tool.conf.getAnnotationRewriteStrategy());

    }

    @Test
    public void passBadAnnotationRewriteStrategy_throw()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        exception.expect(CommandArgumentException.class);
        exception.expectCause(Matchers.any(IllegalArgumentException.class));
        tool.runMigration(new String[] { "-ars", "bar", "-t", "targetFoo", "-b",
                "fooBaseDir", "-src", "barSrcs", "-c",
                makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });
    }

    @Test
    public void dontPassAnnotationRewriteStrategy_defaultStrategyIsInConfiguration()
            throws CommandArgumentException, MigrationToolsException,
            MigrationFailureException, IOException {
        tool.runMigration(new String[] { "-t", "targetFoo", "-b", "fooBaseDir",
                "-src", "barSrcs", "-c", makeSomeFolder().getPath(), "-d",
                makeSomeFolder().toURI().toURL().toExternalForm() });

        Assert.assertEquals(AnnotationsRewriteStrategy.ALWAYS,
                tool.conf.getAnnotationRewriteStrategy());
    }

    private File makeSomeFolder() throws IOException {
        File folder = temporaryFolder.newFolder();
        folder.mkdirs();
        return folder;
    }
}
