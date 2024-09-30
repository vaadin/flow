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

package com.vaadin.flow.server.scanner;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

public class ReflectionsClassFinderTest {

    private static final String CLASS_TEMPLATE = "package %s;\n" + "\n"
            + "import com.vaadin.flow.component.dependency.NpmPackage;\n" + "\n"
            + "import com.vaadin.flow.component.Component;\n" + "\n"
            + "@NpmPackage(value = \"@vaadin/something\", version = \"%s\")\n"
            + "public class %s extends Component {\n" + "}\n";

    // Class used to test the class finder
    public static final Class<NpmPackage> NPM_PACKAGE_CLASS = NpmPackage.class;

    @Rule
    public TemporaryFolder externalModules = new TemporaryFolder();

    URL[] urls;
    private ClassFinder.DefaultClassFinder defaultClassFinder;
    private List<String> componentClassNames;

    @Before
    public void setUp() throws Exception {
        urls = new URL[] {
                createTestModule("module-1", "com.vaadin.flow.test.last",
                        "ComponentN", "3.0.0"),
                createTestModule("module-2", "com.vaadin.flow.test.first",
                        "ComponentX", "1.0.0"),
                createTestModule("module-3", "com.vaadin.flow.test.middle",
                        "ComponentA", "2.0.0"),
                NPM_PACKAGE_CLASS.getProtectionDomain().getCodeSource()
                        .getLocation(),
                Component.class.getProtectionDomain().getCodeSource()
                        .getLocation() };

        try (URLClassLoader classLoader = new URLClassLoader(urls,
                Thread.currentThread().getContextClassLoader())) {
            List<Class<?>> componentClasses = List.of(
                    classLoader
                            .loadClass("com.vaadin.flow.test.first.ComponentX"),
                    classLoader
                            .loadClass("com.vaadin.flow.test.last.ComponentN"),
                    classLoader.loadClass(
                            "com.vaadin.flow.test.middle.ComponentA"));
            componentClassNames = componentClasses.stream().map(Class::getName)
                    .collect(Collectors.toList());
            defaultClassFinder = new ClassFinder.DefaultClassFinder(
                    Set.copyOf(componentClasses));
        }
    }

    @Test
    public void getSubTypesOf_orderIsDeterministic() {
        List<String> a1 = componentClassNames;
        List<String> a2 = new ReflectionsClassFinder(urls[2], urls[0], urls[1],
                urls[4]).getSubTypesOf(Component.class).stream()
                .map(Class::getName)
                .filter(name -> name.startsWith("com.vaadin.flow.test."))
                .collect(Collectors.toList());
        List<String> a3 = new ReflectionsClassFinder(urls[1], urls[4], urls[2],
                urls[0]).getSubTypesOf(Component.class).stream()
                .map(Class::getName)
                .filter(name -> name.startsWith("com.vaadin.flow.test."))
                .collect(Collectors.toList());

        Assert.assertEquals(a1, a2);
        Assert.assertEquals(a2, a3);
    }

    @Test
    public void getAnnotatedClasses_orderIsDeterministic() {
        List<String> a1 = toList(new ReflectionsClassFinder(urls)
                .getAnnotatedClasses(NPM_PACKAGE_CLASS));
        List<String> a2 = toList(
                new ReflectionsClassFinder(urls[3], urls[2], urls[0], urls[1])
                        .getAnnotatedClasses(NPM_PACKAGE_CLASS));
        List<String> a3 = toList(
                new ReflectionsClassFinder(urls[1], urls[2], urls[3], urls[0])
                        .getAnnotatedClasses(NPM_PACKAGE_CLASS));

        Assert.assertEquals(a1, a2);
        Assert.assertEquals(a2, a3);
    }

    @Test
    public void getSubTypesOf_order_sameAsDefaultClassFinder() {
        Assert.assertEquals(
                toList(defaultClassFinder.getSubTypesOf(Component.class)),
                componentClassNames);
    }

    @Test
    public void getAnnotatedClasses_order_sameAsDefaultClassFinder() {
        Assert.assertEquals(
                toList(defaultClassFinder
                        .getAnnotatedClasses(NPM_PACKAGE_CLASS)),
                toList(new ReflectionsClassFinder(urls)
                        .getAnnotatedClasses(NPM_PACKAGE_CLASS)));
    }

    @Test
    public void reflections_notExistingDirectory_warningMessageNotLogged()
            throws Exception {
        Path notExistingDir = Files.createTempDirectory("test")
                .resolve(Path.of("target", "classes"));
        Logger logger = LoggerFactory.getLogger("mockLogger");
        Logger spy = Mockito.spy(logger);
        Logger mocked = Mockito.mock(Logger.class);
        try (MockedStatic<LoggerFactory> mockStatic = Mockito
                .mockStatic(LoggerFactory.class)) {
            mockStatic
                    .when(() -> LoggerFactory
                            .getLogger(ArgumentMatchers.any(Class.class)))
                    .thenReturn(mocked);
            mockStatic.when(() -> LoggerFactory.getLogger(Reflections.class))
                    .thenReturn(spy);
            Logger x = LoggerFactory.getLogger(ReflectionsClassFinder.class);
            new ReflectionsClassFinder(notExistingDir.toUri().toURL());
            Mockito.verify(spy, Mockito.never()).warn(ArgumentMatchers.contains(
                    "could not create Vfs.Dir from url. ignoring the exception and continuing"),
                    ArgumentMatchers.any(Exception.class));
        }
    }

    private <X extends Class<?>> List<String> toList(Set<X> classes) {
        return classes.stream().map(Class::getName)
                .collect(Collectors.toList());
    }

    private URL createTestModule(String moduleName, String pkg,
            String className, String npmPackageVersion) throws IOException {
        File sources = externalModules.newFolder(moduleName + "/src");
        File sourcePkg = externalModules
                .newFolder(moduleName + "/src/" + pkg.replace('.', '/'));
        File buildDir = externalModules.newFolder(moduleName + "/target");

        Path sourceFile = sourcePkg.toPath().resolve(className + ".java");
        Files.writeString(sourceFile, String.format(CLASS_TEMPLATE, pkg,
                npmPackageVersion, className), StandardCharsets.UTF_8);
        compile(sourceFile.toFile(), sources, buildDir);
        return buildDir.toURI().toURL();
    }

    private void compile(File sourceFile, File sourcePath, File outputPath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, "-d", outputPath.getPath(),
                "-sourcepath", sourcePath.getPath(), sourceFile.getPath());
        Assert.assertEquals("Failed to compile " + sourceFile, 0, result);
    }

}
