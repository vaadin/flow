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
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.migration.CreateMigrationJsonsStep;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class CreateMigrationJsonsStepTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File target;

    private CreateMigrationJsonsStep step;

    @Before
    public void setUp() throws IOException {
        target = temporaryFolder.newFolder();
        step = new CreateMigrationJsonsStep(target);
    }

    @Test
    public void createJsons_createBowerAndPackageJsonFiles()
            throws IOException {
        step.createJsons(Arrays.asList("foo.html", "bar/baz.css"));

        File bower = new File(target, "bower.json");
        Assert.assertTrue("bower.json is not created", bower.exists());

        File pckg = new File(target, "package.json");
        Assert.assertTrue("package.json is not created", pckg.exists());

        String bowerContent = Files.readAllLines(bower.toPath()).stream()
                .collect(Collectors.joining("\n"));
        assertFiles(Json.parse(bowerContent));

        String packageContent = Files.readAllLines(pckg.toPath()).stream()
                .collect(Collectors.joining("\n"));
        assertFiles(Json.parse(packageContent));
    }

    private void assertFiles(JsonObject object) {
        JsonArray main = object.get("main");
        Assert.assertEquals(2, main.length());
        Assert.assertEquals("foo.html", main.get(0).asString());
        Assert.assertEquals("bar/baz.css", main.get(1).asString());
    }
}
