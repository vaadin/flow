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
import java.util.HashSet;
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

import com.vaadin.flow.migration.CopyMigratedResourcesStep;

public class CopyMigratedResourcesStepTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CopyMigratedResourcesStep step;

    private File target;

    private File source;

    private Set<String> allowedDirs = new HashSet<>();

    @Before
    public void setUp() throws IOException {
        target = temporaryFolder.newFolder();
        source = temporaryFolder.newFolder();

        step = new CopyMigratedResourcesStep(target, source, allowedDirs);
    }

    @Test
    public void copyResources_filesAreCopiedAndContentIsModified()
            throws IOException {
        allowedDirs.add("dir");
        File file1 = new File(source, "dir/foo.js");
        File file2 = new File(source, "bar.js");

        file1.getParentFile().mkdirs();

        Files.write(file1.toPath(), Arrays.asList(
                "import { PolymerElement } from '/node_modules/@polymer/polymer/polymer-element.js';",
                "import '@vaadin/vaadin-button/vaadin-button.js';",
                "import { html } from \"/node_modules/@polymer/polymer/lib/utils/html-tag.js\";"));

        Files.write(file2.toPath(), Collections.singletonList(
                "import '@polymer/polymer/lib/elements/dom-repeat.js';"));

        Map<String, List<String>> copiedResources = step.copyResources();

        Assert.assertEquals(1, copiedResources.size());

        List<String> paths = copiedResources.values().iterator().next();

        Assert.assertEquals(2, paths.size());

        File copiedFile1 = new File(target, "dir/foo.js");

        // The first file is copied
        Assert.assertTrue(copiedFile1.exists());
        String content = readFile(copiedFile1);

        Assert.assertThat(content, CoreMatchers.containsString(
                "import { PolymerElement } from '@polymer/polymer/polymer-element.js';"));
        Assert.assertThat(content, CoreMatchers.containsString(
                "import '@vaadin/vaadin-button/vaadin-button.js';"));
        Assert.assertThat(content, CoreMatchers.containsString(
                "import { html } from \"@polymer/polymer/lib/utils/html-tag.js\";"));

        File copiedFile2 = new File(target, file2.getName());

        // The second file is copied
        Assert.assertTrue(copiedFile2.exists());
        content = readFile(copiedFile2);

        Assert.assertThat(content, CoreMatchers.containsString(
                "import '@polymer/polymer/lib/elements/dom-repeat.js';"));
    }

    @Test
    public void copyResources_filterOutUnnecessaryFiles() throws IOException {
        File file = new File(source, "bar.js");

        makeFile(new File(source, "bower_components/a.js"));
        makeFile(new File(source, "node_modules/b.js"));
        makeFile(new File(source, "bower.json"));
        makeFile(new File(source, "package.json"));
        makeFile(new File(source, "package-lock.json"));
        makeFile(new File(source, "coverage/c.cov"));
        makeFile(new File(source, "docs/d.doc"));
        makeFile(new File(source, ".gitignore"));

        Files.write(file.toPath(), Collections.singletonList(
                "import { PolymerElement } from '/node_modules/@polymer/polymer/polymer-element.js';"));

        Map<String, List<String>> copiedResources = step.copyResources();
        Assert.assertEquals(1, copiedResources.size());
        Assert.assertEquals(1, copiedResources.values().size());
        Assert.assertEquals("bar.js",
                copiedResources.values().iterator().next().get(0));

        Assert.assertFalse(new File(target, "bower_components/a.js").exists());
        Assert.assertFalse(new File(target, "bower_components").exists());
        Assert.assertFalse(new File(target, "node_modules").exists());
        Assert.assertFalse(new File(target, "node_modules/b.js").exists());
        Assert.assertFalse(new File(target, "package.json").exists());
        Assert.assertFalse(new File(target, "package-lock.json").exists());
        Assert.assertFalse(new File(target, "coverage/c.cov").exists());
        Assert.assertFalse(new File(target, "coverage").exists());
        Assert.assertFalse(new File(target, "docs").exists());
        Assert.assertFalse(new File(target, "docs/d.doc").exists());
        Assert.assertFalse(new File(target, ".gitignore").exists());
    }

    private void makeFile(File file) throws IOException {
        file.getParentFile().mkdirs();
        file.createNewFile();
    }

    private String readFile(File file) throws IOException {
        return Files.readAllLines(file.toPath()).stream()
                .collect(Collectors.joining("\n"));
    }

}
