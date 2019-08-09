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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CopyResourcesStepTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CopyResourcesStep step;

    private File target;

    private File source1, source2;

    @Before
    public void setUp() throws IOException {
        target = temporaryFolder.newFolder();
        source1 = temporaryFolder.newFolder();
        source2 = temporaryFolder.newFolder();
        step = new CopyResourcesStep(target, new File[] { source1, source2 });
    }

    @Test
    public void copyResources_copyPlainFilesAndModifyContent()
            throws IOException {
        File file1 = new File(source1, "foo.html");
        File file2 = new File(source2, "bar.css");
        File file3 = new File(source1, "baz.txt");
        Files.write(file1.toPath(), Arrays.asList(
                "<link rel=\"import\" href=\"/frontend/bower_components/polymer/polymer-element.html\">",
                "<link rel=\"import\" href=\"./bower_components/polymer/lib/elements/dom-repeat.html\">",
                "extra text"));

        String cssContent = ".h1 {}";
        Files.write(file2.toPath(), Collections.singletonList(cssContent));

        Files.write(file3.toPath(), new byte[0]);
        Map<String, List<String>> copiedResources = step.copyResources();

        Assert.assertEquals(2, copiedResources.keySet().size());
        assertCopiedResources(copiedResources, source1, file1.getName());
        assertCopiedResources(copiedResources, source2, file2.getName());

        File copiedFile1 = new File(target, file1.getName());

        // The target file exists
        Assert.assertTrue(copiedFile1.exists());
        String content = readFile(copiedFile1);

        // The content is modified
        Assert.assertThat(content, CoreMatchers.containsString(
                "href=\"bower_components/polymer/polymer-element.html\""));
        Assert.assertThat(content, CoreMatchers.containsString(
                "href=\"bower_components/polymer/lib/elements/dom-repeat.html\""));
        Assert.assertThat(content, CoreMatchers.containsString("extra text"));

        File copiedFile2 = new File(target, file2.getName());

        // The target css file exists
        Assert.assertTrue(copiedFile2.exists());
        content = readFile(copiedFile2);

        Assert.assertEquals(cssContent, content.trim());

        // No extra file copied
        Assert.assertEquals(2, target.listFiles().length);
    }

    @Test
    public void copyResources_copyHierarchicalFilesAndModifyContent()
            throws IOException {
        File file1 = new File(source1, "baz/foo.html");
        File file2 = new File(source2, "dir/subdir/bar.html");

        file1.getParentFile().mkdirs();
        file2.getParentFile().mkdirs();
        Files.write(file1.toPath(), Arrays.asList(
                "<link rel=\"import\" href=\"/frontend/bower_components/polymer/polymer-element.html\">",
                "<link rel=\"import\" href=\"../bower_components/polymer/lib/elements/dom-repeat.html\">",
                "file1"));

        Files.write(file2.toPath(), Arrays.asList(
                "<link rel=\"import\" href=\"/frontend/bower_components/polymer/polymer-element.html\">",
                "file2"));

        Map<String, List<String>> copiedResources = step.copyResources();

        Assert.assertEquals(2, copiedResources.keySet().size());
        assertCopiedResources(copiedResources, source1, "baz/foo.html");
        assertCopiedResources(copiedResources, source2, "dir/subdir/bar.html");

        File copiedFile1 = new File(target, "baz/foo.html");

        // The target file exists
        Assert.assertTrue(copiedFile1.exists());
        String content = readFile(copiedFile1);

        // The content is modified
        Assert.assertThat(content, CoreMatchers.containsString(
                "href=\"../bower_components/polymer/polymer-element.html\""));
        Assert.assertThat(content, CoreMatchers.containsString(
                "href=\"../bower_components/polymer/lib/elements/dom-repeat.html\""));
        Assert.assertThat(content, CoreMatchers.containsString("file1"));

        File copiedFile2 = new File(target, "dir/subdir/bar.html");
        // The second target file exists
        Assert.assertTrue(copiedFile2.exists());
        content = readFile(copiedFile2);

        // The content is modified
        Assert.assertThat(content, CoreMatchers.containsString(
                "href=\"../../bower_components/polymer/polymer-element.html\""));
        Assert.assertThat(content, CoreMatchers.containsString("file2"));
    }

    @Test
    public void copyResources_copyFileAndReturnBowerComponents()
            throws IOException {
        File file = new File(source1, "foo.html");

        Files.write(file.toPath(), Arrays.asList(
                "<link rel=\"import\" href=\"bower_components/vaadin-button/vaadin-button.html\">",
                "<link rel=\"import\" href=\"./bower_components/vaadin-text-field/vaadin-text-area.html\">"));

        step.copyResources();

        Set<String> bowerComponents = step.getBowerComponents();
        Assert.assertEquals(2, bowerComponents.size());
        Assert.assertTrue(bowerComponents.contains("vaadin-button"));
        Assert.assertTrue(bowerComponents.contains("vaadin-text-field"));
    }

    @Test
    public void copyResources_moveFrontendToRoot_copyFileAndReturnBowerComponents()
            throws IOException {
        File file1 = new File(source1, "foo.html");
        File file2 = new File(source1, "frontend/bar.html");
        file2.getParentFile().mkdirs();

        Files.write(file1.toPath(), Collections.singletonList(
                "<link rel=\"import\" href=\"bower_components/vaadin-button/vaadin-button.html\">"));
        Files.write(file2.toPath(), Collections.singletonList(
                "<link rel=\"import\" href=\"./bower_components/vaadin-text-field/vaadin-text-area.html\">"));

        Map<String, List<String>> resources = step.copyResources();

        assertCopiedResources(resources, source1, "bar.html", "foo.html");

        Set<String> bowerComponents = step.getBowerComponents();
        Assert.assertEquals(2, bowerComponents.size());
        Assert.assertTrue(bowerComponents.contains("vaadin-button"));
        Assert.assertTrue(bowerComponents.contains("vaadin-text-field"));

        Assert.assertTrue(new File(target, "foo.html").exists());
        Assert.assertTrue(new File(target, "bar.html").exists());
    }

    @Test
    public void copyResources_copyFileWithContentWithoutHtmlComments()
            throws IOException {
        File file = new File(source1, "foo.html");

        Files.write(file.toPath(), Arrays.asList("<!-- HTML comment 1 -->",
                "<link rel='import' href='/frontend/bower_components/polymer/polymer-element.html'>",
                "<!-- HTML comment 2 -->", "<dom-module id='foo-bar'>",
                "<template><div>xx</div></template>", "<!-- HTML comment 3-->",
                "<script>class A extends Polymer.Element {}</script>"));

        step.copyResources();

        File copiedFile1 = new File(target, file.getName());

        step.getBowerComponents();

        String content = readFile(copiedFile1);
        Assert.assertThat(content,
                CoreMatchers.not(CoreMatchers.containsString("HTML comment")));
    }

    private String readFile(File file) throws IOException {
        return Files.readAllLines(file.toPath()).stream()
                .collect(Collectors.joining("\n"));
    }

    private void assertCopiedResources(Map<String, List<String>> copied,
            File source, String... path) {
        List<String> list = copied.get(source.getPath());
        Assert.assertEquals(path.length, list.size());
        for (int i = 0; i < path.length; i++) {
            Assert.assertTrue("Copied resources didn't contain: " + path[i], list.contains(path[i]));
        }
    }
}
