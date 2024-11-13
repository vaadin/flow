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

import javax.inject.Inject;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.utils.FlowFileUtils;

import static com.vaadin.flow.plugin.maven.BuildFrontendMojoTest.getClassPath;

public class ReflectorTest {

    Reflector reflector;

    @Before
    public void setUp() {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        URLClassLoader urlClassLoader = new URLClassLoader(
                getClassPath(Path.of(".")).stream().distinct().map(File::new)
                        .map(FlowFileUtils::convertToUrl).toArray(URL[]::new),
                ClassLoader.getPlatformClassLoader()) {
            @Override
            protected Class<?> findClass(String name)
                    throws ClassNotFoundException {
                // For test purposes, make maven API are loaded from shared
                // class loader
                if (!name.startsWith("com.vaadin.flow.plugin.maven.")) {
                    return systemClassLoader.loadClass(name);
                }
                return super.findClass(name);
            }
        };
        reflector = new Reflector(urlClassLoader);
    }

    @Test
    public void createMojo_createInstanceAndCopyFields() throws Exception {
        MyMojo source = new MyMojo();
        source.fillFields();
        Mojo target = reflector.createMojo(source);
        MatcherAssert.assertThat("foo field", target,
                Matchers.hasProperty("foo", Matchers.equalTo(source.foo)));
        MatcherAssert.assertThat("bar field", target,
                Matchers.hasProperty("bar", Matchers.equalTo(source.bar)));
        MatcherAssert.assertThat("notAnnotated field", target,
                Matchers.hasProperty("notAnnotated",
                        Matchers.equalTo(source.notAnnotated)));
        MatcherAssert.assertThat("mojoExecution field", target,
                Matchers.hasProperty("mojoExecution",
                        Matchers.equalTo(source.mojoExecution)));
        MatcherAssert.assertThat("maven project field", target, Matchers
                .hasProperty("project", Matchers.equalTo(source.project)));
        MatcherAssert.assertThat("classFinder field", target,
                Matchers.hasProperty("classFinder", Matchers.notNullValue()));
    }

    @Test
    public void createMojo_subclass_createInstanceAndCopyFields()
            throws Exception {
        SubClassMojo source = new SubClassMojo();
        source.fillFields();
        Mojo target = reflector.createMojo(source);
        MatcherAssert.assertThat("foo field", target,
                Matchers.hasProperty("foo", Matchers.equalTo(source.foo)));
        MatcherAssert.assertThat("bar field", target,
                Matchers.hasProperty("bar", Matchers.equalTo(source.bar)));
        MatcherAssert.assertThat("childProperty field", target,
                Matchers.hasProperty("childProperty",
                        Matchers.equalTo(source.childProperty)));
        MatcherAssert.assertThat("notAnnotated field", target,
                Matchers.hasProperty("notAnnotated",
                        Matchers.equalTo(source.notAnnotated)));
        MatcherAssert.assertThat("mojoExecution field", target,
                Matchers.hasProperty("mojoExecution",
                        Matchers.equalTo(source.mojoExecution)));
        MatcherAssert.assertThat("maven project field", target, Matchers
                .hasProperty("project", Matchers.equalTo(source.project)));
        MatcherAssert.assertThat("classFinder field", target,
                Matchers.hasProperty("classFinder", Matchers.notNullValue()));
    }

    @Test
    public void createMojo_incompatibleFields_fails() {
        IncompatibleFieldsMojo source = new IncompatibleFieldsMojo();
        source.fillFields();
        NoSuchFieldException exception = Assert.assertThrows(
                NoSuchFieldException.class, () -> reflector.createMojo(source));
        Assert.assertTrue(
                "Expected exception to be thrown because of class loader mismatch",
                exception.getMessage()
                        .contains("loaded from different class loaders"));
    }

    @Test
    public void reflector_fromProject_getsIsolatedClassLoader()
            throws Exception {
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
                .addURL(Path
                        .of("src", "test", "resources",
                                "jar-without-frontend-resources.jar")
                        .toUri().toURL());
        // .addURL(new URL("file:///some/flat/maven-repo/maven-api.jar"));
        pluginDescriptor.setClassRealm(classWorld.newRealm("maven-plugin"));

        Reflector execReflector = Reflector.of(project, mojoExecution);

        URLClassLoader isolatedClassLoader = execReflector
                .getIsolatedClassLoader();

        Set<String> urlSet = Arrays.stream(isolatedClassLoader.getURLs())
                .map(URL::getFile).collect(Collectors.toSet());
        Assert.assertEquals(4, urlSet.size());
        Assert.assertTrue(urlSet.contains(outputDirectory));
        Assert.assertTrue(urlSet.contains(
                "/some/flat/maven-repo/com.vaadin.test-compile-1.0.jar"));
        Assert.assertTrue(urlSet.contains(
                "/some/flat/maven-repo/com.vaadin.test-system-1.0.jar"));
        Assert.assertTrue(urlSet.contains(
                "/some/flat/maven-repo/com.vaadin.test-plugin-1.0.jar"));

        // from platform class loader
        Assert.assertNotNull(
                isolatedClassLoader.loadClass("java.net.http.HttpClient"));
        // from maven.api class loader
        Assert.assertNotNull(
                isolatedClassLoader.getResource("org/json/CookieList.class"));
        Assert.assertNotNull(
                isolatedClassLoader.loadClass("org.json.CookieList"));
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

    public static class MyMojo extends FlowModeAbstractMojo {

        @Parameter
        String foo;

        @Parameter
        Boolean bar;

        String notAnnotated = "NOT ANNOTATED";

        public MyMojo() {
            project = new MavenProject();
            project.setGroupId("com.vaadin.test");
            project.setArtifactId("reflector-tests");
        }

        void fillFields() {
            mojoExecution = new MojoExecution(new MojoDescriptor());
            project = new MavenProject();
            foo = "foo";
            bar = true;
        }

        protected void executeInternal() {

        }

        public String getFoo() {
            return foo;
        }

        public Boolean getBar() {
            return bar;
        }

        public String getNotAnnotated() {
            return notAnnotated;
        }

        public MojoExecution getMojoExecution() {
            return mojoExecution;
        }

        public MavenProject getProject() {
            return project;
        }

    }

    public static class SubClassMojo extends MyMojo {

        @Parameter
        private String childProperty;

        @Override
        void fillFields() {
            super.fillFields();
            childProperty = "CHILD";
        }

        public String getChildProperty() {
            return childProperty;
        }
    }

    public static class FakeMavenComponent {
    }

    public static class IncompatibleFieldsMojo extends MyMojo {

        @Inject
        private FakeMavenComponent buildContext;

        @Override
        void fillFields() {
            super.fillFields();
            buildContext = new FakeMavenComponent();
        }

        public FakeMavenComponent getBuildContext() {
            return buildContext;
        }
    }

}