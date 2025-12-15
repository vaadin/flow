/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

public class TaskProcessStylesheetCssTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File metaInfResources;
    private Options options;
    private ClassFinder classFinder;

    @Before
    public void setup() throws IOException {
        File buildOutput = temporaryFolder.newFolder("target", "classes");
        File metaInf = new File(buildOutput, "META-INF");
        metaInfResources = new File(metaInf, "resources");
        metaInfResources.mkdirs();

        classFinder = Mockito.mock(ClassFinder.class);

        options = Mockito.mock(Options.class);
        Mockito.when(options.isProductionMode()).thenReturn(true);
        Mockito.when(options.getMetaInfResourcesDirectory())
                .thenReturn(metaInfResources);
        Mockito.when(options.getClassFinder()).thenReturn(classFinder);
    }

    // Test class with @StyleSheet annotation for testing
    @StyleSheet("./styles.css")
    private static class TestClassWithStyleSheet {
    }

    @StyleSheet("./sub/nested.css")
    private static class TestClassWithNestedStyleSheet {
    }

    @StyleSheet("https://example.com/external.css")
    private static class TestClassWithExternalStyleSheet {
    }

    @StyleSheet("./styles.css")
    @StyleSheet("./other.css")
    private static class TestClassWithMultipleStyleSheets {
    }

    @Test
    public void execute_processesAndMinifiesCssFiles()
            throws ExecutionFailedException, IOException {
        // Setup mock to return our test class
        Mockito.when(classFinder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Set.of(TestClassWithStyleSheet.class));

        String originalCss = """
                /* A comment */
                .class {
                    color: red;
                }
                """;
        File cssFile = new File(metaInfResources, "styles.css");
        FileUtils.writeStringToFile(cssFile, originalCss,
                StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute();

        String processedCss = Files.readString(cssFile.toPath());
        Assert.assertEquals(".class{color:red}", processedCss);
    }

    @Test
    public void execute_inlinesImports()
            throws ExecutionFailedException, IOException {
        Mockito.when(classFinder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Set.of(TestClassWithStyleSheet.class));

        String mainCss = "@import 'other.css';\n.main { color: red; }";
        String otherCss = ".other { color: blue; }";

        File mainFile = new File(metaInfResources, "styles.css");
        File otherFile = new File(metaInfResources, "other.css");
        FileUtils.writeStringToFile(mainFile, mainCss, StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(otherFile, otherCss,
                StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute();

        String processedCss = Files.readString(mainFile.toPath());
        // Should contain both rules, minified
        Assert.assertTrue(processedCss.contains(".other{color:blue}"));
        Assert.assertTrue(processedCss.contains(".main{color:red}"));
    }

    @Test
    public void execute_processesNestedDirectories()
            throws ExecutionFailedException, IOException {
        Mockito.when(classFinder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Set.of(TestClassWithNestedStyleSheet.class));

        File subDir = new File(metaInfResources, "sub");
        subDir.mkdirs();

        String css = ".nested { color: green; }";
        File cssFile = new File(subDir, "nested.css");
        FileUtils.writeStringToFile(cssFile, css, StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute();

        String processedCss = Files.readString(cssFile.toPath());
        Assert.assertEquals(".nested{color:green}", processedCss);
    }

    @Test
    public void execute_skipsNonProductionMode()
            throws ExecutionFailedException, IOException {
        Mockito.when(options.isProductionMode()).thenReturn(false);

        String originalCss = "/* comment */ .class { color: red; }";
        File cssFile = new File(metaInfResources, "styles.css");
        FileUtils.writeStringToFile(cssFile, originalCss,
                StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute();

        // File should remain unchanged
        String processedCss = Files.readString(cssFile.toPath());
        Assert.assertEquals(originalCss, processedCss);
    }

    @Test
    public void execute_skipsExternalStylesheets()
            throws ExecutionFailedException, IOException {
        Mockito.when(classFinder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Set.of(TestClassWithExternalStyleSheet.class));

        // Create a local CSS file that should NOT be processed
        String originalCss = "/* comment */ .class { color: red; }";
        File cssFile = new File(metaInfResources, "local.css");
        FileUtils.writeStringToFile(cssFile, originalCss,
                StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute();

        // File should remain unchanged since only external URL is referenced
        String processedCss = Files.readString(cssFile.toPath());
        Assert.assertEquals(originalCss, processedCss);
    }

    @Test
    public void execute_skipsUnreferencedCssFiles()
            throws ExecutionFailedException, IOException {
        // Only styles.css is referenced
        Mockito.when(classFinder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Set.of(TestClassWithStyleSheet.class));

        // Create referenced CSS file
        File referencedCss = new File(metaInfResources, "styles.css");
        FileUtils.writeStringToFile(referencedCss, "/* comment */ .ref { }",
                StandardCharsets.UTF_8);

        // Create unreferenced CSS file
        String unreferencedOriginal = "/* comment */ .unref { color: red; }";
        File unreferencedCss = new File(metaInfResources, "unreferenced.css");
        FileUtils.writeStringToFile(unreferencedCss, unreferencedOriginal,
                StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute();

        // Unreferenced file should remain unchanged
        String unreferencedProcessed = Files
                .readString(unreferencedCss.toPath());
        Assert.assertEquals(unreferencedOriginal, unreferencedProcessed);

        // Referenced file should be processed
        String referencedProcessed = Files.readString(referencedCss.toPath());
        Assert.assertEquals(".ref{}", referencedProcessed);
    }

    @Test
    public void execute_handlesNoAnnotations()
            throws ExecutionFailedException, IOException {
        Mockito.when(classFinder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Collections.emptySet());

        String originalCss = "/* comment */ .class { color: red; }";
        File cssFile = new File(metaInfResources, "styles.css");
        FileUtils.writeStringToFile(cssFile, originalCss,
                StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute(); // Should not throw

        // File should remain unchanged since no annotations found
        String processedCss = Files.readString(cssFile.toPath());
        Assert.assertEquals(originalCss, processedCss);
    }

    @Test
    public void execute_handlesNullClassFinder()
            throws ExecutionFailedException, IOException {
        Mockito.when(options.getClassFinder()).thenReturn(null);

        String originalCss = "/* comment */ .class { color: red; }";
        File cssFile = new File(metaInfResources, "styles.css");
        FileUtils.writeStringToFile(cssFile, originalCss,
                StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute(); // Should not throw

        // File should remain unchanged
        String processedCss = Files.readString(cssFile.toPath());
        Assert.assertEquals(originalCss, processedCss);
    }

    @Test
    public void execute_handlesMissingDirectory()
            throws ExecutionFailedException, IOException {
        FileUtils.deleteDirectory(metaInfResources);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute(); // Should not throw
    }

    @Test
    public void execute_handlesNullResourceOutputDirectory()
            throws ExecutionFailedException {
        Mockito.when(options.getResourceOutputDirectory()).thenReturn(null);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute(); // Should not throw
    }

    @Test
    public void execute_inlinesNodeModulesImports()
            throws ExecutionFailedException, IOException {
        Mockito.when(classFinder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Set.of(TestClassWithStyleSheet.class));

        // Create node_modules structure
        File nodeModules = temporaryFolder.newFolder("node_modules");
        File packageDir = new File(nodeModules, "some-package");
        packageDir.mkdirs();

        File nodeModulesCss = new File(packageDir, "styles.css");
        FileUtils.writeStringToFile(nodeModulesCss,
                ".from-node-modules { color: blue; }", StandardCharsets.UTF_8);

        Mockito.when(options.getNodeModulesFolder()).thenReturn(nodeModules);

        // Create main CSS that imports from node_modules
        String mainCss = "@import 'some-package/styles.css';\n.main { color: red; }";
        File mainFile = new File(metaInfResources, "styles.css");
        FileUtils.writeStringToFile(mainFile, mainCss, StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute();

        String processedCss = Files.readString(mainFile.toPath());
        // Should contain minified node_modules CSS
        Assert.assertTrue("Should contain inlined node_modules CSS",
                processedCss.contains(".from-node-modules{color:blue}"));
        Assert.assertTrue("Should contain main CSS",
                processedCss.contains(".main{color:red}"));
    }

    @Test
    public void execute_processesMultipleAnnotatedClasses()
            throws ExecutionFailedException, IOException {
        Mockito.when(classFinder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Set.of(TestClassWithStyleSheet.class,
                        TestClassWithNestedStyleSheet.class));

        // Create both CSS files
        File stylesFile = new File(metaInfResources, "styles.css");
        FileUtils.writeStringToFile(stylesFile, "/* comment */ .styles { }",
                StandardCharsets.UTF_8);

        File subDir = new File(metaInfResources, "sub");
        subDir.mkdirs();
        File nestedFile = new File(subDir, "nested.css");
        FileUtils.writeStringToFile(nestedFile, "/* comment */ .nested { }",
                StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute();

        // Both files should be processed
        Assert.assertEquals(".styles{}", Files.readString(stylesFile.toPath()));
        Assert.assertEquals(".nested{}", Files.readString(nestedFile.toPath()));
    }

    @Test
    public void execute_handlesRepeatedAnnotations()
            throws ExecutionFailedException, IOException {
        Mockito.when(classFinder.getAnnotatedClasses(StyleSheet.class))
                .thenReturn(Set.of(TestClassWithMultipleStyleSheets.class));

        // Create both CSS files
        File stylesFile = new File(metaInfResources, "styles.css");
        FileUtils.writeStringToFile(stylesFile, "/* comment */ .styles { }",
                StandardCharsets.UTF_8);

        File otherFile = new File(metaInfResources, "other.css");
        FileUtils.writeStringToFile(otherFile, "/* comment */ .other { }",
                StandardCharsets.UTF_8);

        TaskProcessStylesheetCss task = new TaskProcessStylesheetCss(options);
        task.execute();

        // Both files should be processed
        Assert.assertEquals(".styles{}", Files.readString(stylesFile.toPath()));
        Assert.assertEquals(".other{}", Files.readString(otherFile.toPath()));
    }
}
