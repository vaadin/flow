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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.migration.samplecode.ClassUnitWithNonPublicClass;
import com.vaadin.flow.migration.samplecode.Component1;
import com.vaadin.flow.migration.samplecode.Component2;
import com.vaadin.flow.migration.samplecode.Component3;
import com.vaadin.flow.migration.samplecode.EnclosingClassWithNestedClass;
import com.vaadin.flow.migration.samplecode.EnclosingClassWithNestedClass.NestedComponent;
import com.vaadin.flow.migration.samplecode.NonVaadinComponent;
import com.vaadin.flow.migration.samplecode.ShouldNotBeRewritten;
import com.vaadin.flow.migration.samplecode.StyledComponent;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

public class RewriteLegacyAnnotationsStepTest {

    private RewriteLegacyAnnotationsStep step;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File compiledClassesDir;

    private ClassFinder finder = Mockito.mock(ClassFinder.class);

    private File sourceRoot1;
    private File sourceRoot2;

    @Before
    public void setUp()
            throws IOException, ClassNotFoundException, URISyntaxException {
        compiledClassesDir = new File(RewriteLegacyAnnotationsStepTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());

        sourceRoot1 = temporaryFolder.newFolder();
        sourceRoot2 = temporaryFolder.newFolder();
        step = new RewriteLegacyAnnotationsStep(compiledClassesDir, finder,
                Arrays.asList(sourceRoot1, sourceRoot2));

        Mockito.doAnswer(invocation -> Class
                .forName(invocation.getArgumentAt(0, String.class)))
                .when(finder).loadClass(Mockito.any(String.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fileIsInOneSourceRoot_classIsInCompiledClassesDir_htmlImportsAreRewritten()
            throws IOException {
        Mockito.when(finder.getAnnotatedClasses(HtmlImport.class))
                .thenReturn(Collections.singleton(Component1.class));
        File sourceFile = makeSourceJavaFile(sourceRoot1, Component1.class);
        step.rewrite();

        String content = FileUtils.readFileToString(sourceFile,
                StandardCharsets.UTF_8);
        Assert.assertThat(content, CoreMatchers.allOf(
                CoreMatchers.containsString(
                        "import com.vaadin.flow.component.dependency.JsModule;"),
                CoreMatchers.containsString("@JsModule(\"./foo.js\")"),
                CoreMatchers.containsString("@JsModule(\"./foo1.js\")"),
                CoreMatchers.containsString("@JsModule(\"./foo2.js\")"),
                CoreMatchers.containsString("@JsModule(\"./bar.js\")"),
                CoreMatchers.containsString("@JsModule(\"./bar1.js\")"),
                CoreMatchers.containsString("@JsModule(\"./src/baz.js\")"),
                CoreMatchers.containsString(
                        "@JsModule(\"@vaadin/vaadin-button/src/vaadin-button.js\")"),
                CoreMatchers.containsString(
                        "@JsModule(\"@vaadin/vaadin-text-field/src/vaadin-text-field.js\")")));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fileIsInOneSourceRoot_classIsInCompiledClassesDir_stylesheetsAreRewritten()
            throws IOException {
        Mockito.when(finder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Collections.singleton(StyledComponent.class));
        File sourceFile = makeSourceJavaFile(sourceRoot1,
                StyledComponent.class);
        step.rewrite();

        String content = FileUtils.readFileToString(sourceFile,
                StandardCharsets.UTF_8);
        Assert.assertThat(content, CoreMatchers.allOf(
                CoreMatchers.containsString(
                        "import com.vaadin.flow.component.dependency.JsModule;"),
                CoreMatchers.containsString("@JsModule(\"./styles/foo.js\")"),
                CoreMatchers.containsString("@JsModule(\"./styles/foo1.js\")"),
                CoreMatchers.containsString("@JsModule(\"./styles/foo2.js\")"),
                CoreMatchers.containsString("@JsModule(\"./styles/bar.js\")"),
                CoreMatchers.containsString("@JsModule(\"./styles/bar1.js\")"),
                CoreMatchers
                        .containsString("@JsModule(\"./styles/src/baz.js\")")));
    }

    @Test
    public void fileContainsHtmlImportAndStyleSheetWords_wordsAreNotReplaced()
            throws IOException {
        Mockito.when(finder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Collections.singleton(ShouldNotBeRewritten.class));
        Mockito.when(finder.getAnnotatedClasses(HtmlImport.class))
                .thenReturn(Collections.singleton(ShouldNotBeRewritten.class));
        File sourceFile = makeSourceJavaFile(sourceRoot1,
                ShouldNotBeRewritten.class);

        String originalContent = FileUtils.readFileToString(sourceFile,
                StandardCharsets.UTF_8);

        step.rewrite();

        String content = FileUtils.readFileToString(sourceFile,
                StandardCharsets.UTF_8);

        // nothing has changed
        Assert.assertEquals(originalContent, content);
    }

    @Test
    public void fileIsInOneSourceRoot_classIsNotInCompiledClassesDir_nothingIsDone()
            throws IOException, URISyntaxException {
        Mockito.when(finder.getAnnotatedClasses(HtmlImport.class))
                .thenReturn(Collections.singleton(Component1.class));

        compiledClassesDir = temporaryFolder.newFolder();

        step = new RewriteLegacyAnnotationsStep(compiledClassesDir, finder,
                Arrays.asList(sourceRoot1, sourceRoot2));

        File sourceFile = makeSourceJavaFile(sourceRoot1, Component1.class);
        step.rewrite();

        String content = FileUtils.readFileToString(sourceFile,
                StandardCharsets.UTF_8);
        Assert.assertEquals(
                FileUtils.readFileToString(getSourceFile(Component1.class),
                        StandardCharsets.UTF_8),
                content);
    }

    @Test
    public void filesAreInBothSourceRoots_classedAreInCompiledClassesDir_htmlImportsAreRewritten()
            throws IOException {
        Mockito.when(finder.getAnnotatedClasses(HtmlImport.class))
                .thenReturn(new HashSet<>(
                        Arrays.asList(Component2.class, Component3.class)));
        File sourceFile1 = makeSourceJavaFile(sourceRoot1, Component2.class);
        File sourceFile2 = makeSourceJavaFile(sourceRoot2, Component3.class);
        step.rewrite();

        Assert.assertThat(
                FileUtils.readFileToString(sourceFile1, StandardCharsets.UTF_8),
                CoreMatchers.containsString("@JsModule(\"./foo.js\")"));

        Assert.assertThat(
                FileUtils.readFileToString(sourceFile2, StandardCharsets.UTF_8),
                CoreMatchers.containsString("@JsModule(\"./bar.js\")"));
    }

    @Test
    public void nonPublicClassDeclaredInForeignJavaFile_filesAreInBothSourceRoots_classedAreInCompiledClassesDir_htmlImportsAreRewritten()
            throws IOException, ClassNotFoundException {
        // Fake a case when the same non public class is declared in foreign
        // Java file but it exists in both source roots (improbable situation
        // since it has to be only in one file, but this allows to check that
        // every possible file which contains class declaration (in source) is
        // rewritten

        Class<?> nonPublicClass = Class
                .forName(ClassUnitWithNonPublicClass.NON_PUBLIC_CLASS_NAME);
        Mockito.when(finder.getAnnotatedClasses(HtmlImport.class)).thenReturn(
                new HashSet<>(Arrays.asList(nonPublicClass, Component3.class)));
        File sourceFile1 = makeSourceJavaFile(sourceRoot1,
                ClassUnitWithNonPublicClass.class);
        File sourceFile2 = makeSourceJavaFile(sourceRoot2, Component3.class);
        // append source code from sourceFile1 to sourceFile2

        Files.write(sourceFile2.toPath(),
                Collections.singletonList(FileUtils
                        .readFileToString(sourceFile1, StandardCharsets.UTF_8)),
                StandardOpenOption.APPEND);
        step.rewrite();

        // Correct class source code: non public class is defined in foreign
        // Java file, the import is rewritten
        Assert.assertThat(
                FileUtils.readFileToString(sourceFile1, StandardCharsets.UTF_8),
                CoreMatchers.containsString("@JsModule(\"./src/foo/bar.js\")"));

        // Incorrect class source code: we have appended to the correct Java
        // class source code the content of another class. But it still should
        // be rewritten correctly
        Assert.assertThat(
                FileUtils.readFileToString(sourceFile2, StandardCharsets.UTF_8),
                CoreMatchers.containsString("@JsModule(\"./src/foo/bar.js\")"));
    }

    @Test
    public void nestedClass_fileIsInOneSourceRoot_classIsInCompiledClassesDir_htmlImportsAreRewritten()
            throws IOException {
        Mockito.when(finder.getAnnotatedClasses(HtmlImport.class))
                .thenReturn(Collections.singleton(NestedComponent.class));
        File sourceFile = makeSourceJavaFile(sourceRoot1,
                EnclosingClassWithNestedClass.class);
        step.rewrite();

        Assert.assertThat(
                FileUtils.readFileToString(sourceFile, StandardCharsets.UTF_8),
                CoreMatchers.containsString("@JsModule(\"./foo.js\")"));
    }

    @Test
    public void nestedClassInsideNonPublicClassDeclaredInForeignJavaFile_fileIsInOneSourceRoot_classIsInCompiledClassesDir_htmlImportsAreRewritten()
            throws IOException, ClassNotFoundException {
        Class<?> nestedClas = Class.forName(
                ClassUnitWithNonPublicClass.NESTEDCLASS_INSIDE_NON_PUBLIC_CLASS_NAME);
        Mockito.when(finder.getAnnotatedClasses(HtmlImport.class))
                .thenReturn(Collections.singleton(nestedClas));
        File sourceFile = makeSourceJavaFile(sourceRoot1,
                ClassUnitWithNonPublicClass.class);
        step.rewrite();

        Assert.assertThat(
                FileUtils.readFileToString(sourceFile, StandardCharsets.UTF_8),
                CoreMatchers.containsString("@JsModule(\"./baz.js\")"));
    }

    @Test
    public void nonVaadinComponent_htmlImportsAreRewrittenUsing_NPM_VENDOR_Prefix()
            throws IOException {
        Mockito.when(finder.getAnnotatedClasses(HtmlImport.class))
                .thenReturn(Collections.singleton(NonVaadinComponent.class));
        File sourceFile = makeSourceJavaFile(sourceRoot1,
                NonVaadinComponent.class);
        step.rewrite();

        String content = FileUtils.readFileToString(sourceFile,
                StandardCharsets.UTF_8);
        Assert.assertThat(content, CoreMatchers.allOf(
                CoreMatchers.containsString(
                        "import com.vaadin.flow.component.dependency.JsModule;"),
                CoreMatchers.containsString(
                        "@JsModule(\"NPM_VENDOR/non-vaadin/non-vaadin.js\")")));
    }

    private File makeSourceJavaFile(File root, Class<?> clazz)
            throws IOException {
        String folder = clazz.getPackage().getName().replace(".", "/");
        File sourceFolder = new File(root, folder);
        sourceFolder.mkdirs();
        String name = clazz.getSimpleName() + ".java";

        File sourceFile = new File(sourceFolder, name);

        InputStream inputStream = clazz.getResourceAsStream(name);
        Files.copy(inputStream, sourceFile.toPath());
        return sourceFile;
    }

    private File getSourceFile(Class<?> clazz) throws URISyntaxException {
        String name = clazz.getSimpleName() + ".java";

        return new File(clazz.getResource(name).toURI());
    }

}
