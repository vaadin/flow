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

package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.Assert;
import org.junit.Test;
import org.reflections.ReflectionUtils;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

public class ReflectorTest {

    Reflector reflector = new Reflector(new URLClassLoader(new URL[0],
            Thread.currentThread().getContextClassLoader()));

    @Test
    public void createTask_missingCompanionTask_throws() {
        IllegalArgumentException exception = Assert
                .assertThrows(IllegalArgumentException.class, () -> reflector
                        .createTask(new MyLonelyMojo(), List.of(), List.of()));
        Assert.assertTrue(exception.getMessage()
                .contains("Cannot find companion task class"));
    }

    @Test
    public void createTask_companionTaskNotExtendingBaseTask_throws() {
        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> reflector.createTask(new WrongCompanionMojo(), List.of(),
                        List.of()));
        Assert.assertTrue(exception.getMessage().contains(
                "does not extend " + FlowModeAbstractTask.class.getName()));
    }

    @Test
    public void createTask_invalidParameterTypes_throws() {
        NoSuchMethodException exception = Assert
                .assertThrows(NoSuchMethodException.class,
                        () -> reflector.createTask(new MyMojo(),
                                List.of(String.class, Number.class),
                                List.of("", 0)));
        Assert.assertTrue(exception.getMessage()
                .contains(MyTask.class.getName() + ".<init>"));
    }

    @Test
    public void createTask_invalidParameterValues_throws() {
        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> reflector.createTask(new MyMojo(), List.of(String.class),
                        List.of(0)));
        Assert.assertTrue(
                exception.getMessage().contains("argument type mismatch"));
    }

    @Test
    public void createTask_companionTask_createInstanceAndCopyFields()
            throws Exception {
        MyMojo mojo = new MyMojo();
        Mojo task = reflector.createTask(mojo, List.of(String.class),
                List.of("PARAM"));
        if (task instanceof MyTask myTask) {
            Assert.assertEquals("foo field", mojo.foo, myTask.foo);
            Assert.assertEquals("bar field", mojo.bar, myTask.bar);
            Assert.assertEquals("additional constructor param",
                    myTask.additionalParam, "PARAM");
            Assert.assertEquals("not annotated field", mojo.notAnnotated,
                    myTask.notAnnotated);
        } else {
            Assert.fail("Expected task to be of type " + MyTask.class.getName()
                    + " but was " + task.getClass().getName());
        }
    }

    @Test
    public void createTask_companionTaskSubclass_createInstanceAndCopyFields()
            throws Exception {
        SubClassMojo mojo = new SubClassMojo();
        Mojo task = reflector.createTask(mojo, List.of(), List.of());
        if (task instanceof SubClassTask myTask) {
            Assert.assertEquals("foo field", mojo.foo, myTask.foo);
            Assert.assertEquals("bar field", mojo.bar, myTask.bar);
            Assert.assertEquals("child field", mojo.childProperty,
                    myTask.childProperty);
            Assert.assertEquals("not annotated field", mojo.notAnnotated,
                    myTask.notAnnotated);
        } else {
            Assert.fail("Expected task to be of type " + MyTask.class.getName()
                    + " but was " + task.getClass().getName());
        }
    }

    @Test
    public void reflectorFromProject() throws Exception {
        String outputDirectory = "/my/project/target";

        MavenProject project = new MavenProject();
        project.setGroupId("com.vaadin.test");
        project.setArtifactId("reflector-tests");
        project.setBuild(new Build());
        project.getBuild().setOutputDirectory(outputDirectory);
        project.setArtifacts(Set.of(
                createArtifact("com.vaadin.test", "compile", "1.0", "compile",
                        true),
                createArtifact("com.vaadin.test", "provided", "1.0", "provided",
                        true),
                createArtifact("com.vaadin.test", "test", "1.0", "test", true),
                createArtifact("com.vaadin.test", "system", "1.0", "system",
                        true),
                createArtifact("com.vaadin.test", "not-classpath", "1.0",
                        "compile", false)));

        MojoExecution mojoExecution = new MojoExecution(new MojoDescriptor());
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        mojoExecution.getMojoDescriptor().setPluginDescriptor(pluginDescriptor);
        pluginDescriptor.setGroupId("com.vaadin.test");
        pluginDescriptor.setArtifactId("test-plugin");
        pluginDescriptor.setArtifacts(List.of(
                createArtifact("com.vaadin.test", "plugin", "1.0", "compile",
                        true),
                createArtifact("com.vaadin.test", "compile", "2.0", "compile",
                        true)));
        ClassWorld classWorld = new ClassWorld("maven.api", null);
        classWorld.getRealm("maven.api")
                .addURL(new URL("file:///some/flat/maven-repo/maven-api.jar"));
        pluginDescriptor.setClassRealm(classWorld.newRealm("maven-plugin"));

        Reflector execReflector = Reflector.of(project, mojoExecution);

        URLClassLoader taskClassLoader = execReflector.getTaskClassLoader();

        Set<String> urlSet = Arrays.stream(taskClassLoader.getURLs())
                .map(URL::getFile).collect(Collectors.toSet());
        Assert.assertEquals(4, urlSet.size());
        Assert.assertTrue(urlSet.contains(outputDirectory));
        Assert.assertTrue(urlSet.contains(
                "/some/flat/maven-repo/com.vaadin.test-compile-1.0.jar"));
        Assert.assertTrue(urlSet.contains(
                "/some/flat/maven-repo/com.vaadin.test-system-1.0.jar"));
        Assert.assertTrue(urlSet.contains(
                "/some/flat/maven-repo/com.vaadin.test-plugin-1.0.jar"));

        ClassLoader parentClassloader = taskClassLoader.getParent();
        Assert.assertTrue(parentClassloader instanceof ClassRealm);
        ClassRealm mavenApi = (ClassRealm) parentClassloader;
        Assert.assertEquals("maven.api", mavenApi.getId());
        Assert.assertEquals(1, mavenApi.getURLs().length);
        Assert.assertEquals("/some/flat/maven-repo/maven-api.jar",
                mavenApi.getURLs()[0].getFile());

    }

    private Artifact createArtifact(String groupId, String artifactId,
            String version, String scope, boolean addedToClasspath) {
        DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler();
        artifactHandler.setAddedToClasspath(addedToClasspath);
        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId,
                version, scope, "jar", null, artifactHandler);
        artifact.setFile(
                new File(String.format("/some/flat/maven-repo/%s-%s-%s.jar",
                        groupId, artifactId, version)));
        return artifact;
    }

    private static class MyLonelyMojo extends FlowModeAbstractMojo {
    }

    private static class WrongCompanionMojo extends FlowModeAbstractMojo {
    }

    private static class WrongCompanionTask {
    }

    private static class MyMojo extends FlowModeAbstractMojo {

        @Parameter
        String foo = "FOO";

        @Parameter
        Boolean bar = Boolean.TRUE;

        String notAnnotated = "NOT ANNOTATED";

        public MyMojo() {
            project = new MavenProject();
            project.setGroupId("com.vaadin.test");
            project.setArtifactId("reflector-tests");
        }
    }

    private static class MyTask extends FlowModeAbstractTask {

        private String foo;
        private String notAnnotated;
        private Boolean bar;
        final String additionalParam;

        public MyTask(MavenProject project, ClassFinder classFinder, Log logger,
                String param) {
            super(project, classFinder, logger);
            additionalParam = param;
        }

        @Override
        public void execute()
                throws MojoFailureException, MojoExecutionException {

        }
    }

    private static class SubClassMojo extends MyMojo {

        @Parameter
        private String childProperty = "CHILD";

    }

    private static class SubClassTask extends FlowModeAbstractTask {

        private String foo;
        private Boolean bar;
        private String childProperty;
        private String notAnnotated;

        protected SubClassTask(MavenProject project, ClassFinder classFinder,
                Log logger) {
            super(project, classFinder, logger);
        }

        @Override
        public void execute()
                throws MojoFailureException, MojoExecutionException {

        }
    }

    private static class MissingFieldsMojo extends MyMojo {

        @Parameter
        private String childPropery = "CHILD";

    }

    private static class MissingFieldsTask extends MyTask {

        public MissingFieldsTask(MavenProject project, ClassFinder classFinder,
                Log logger, String param) {
            super(project, classFinder, logger, param);
        }
    }

}