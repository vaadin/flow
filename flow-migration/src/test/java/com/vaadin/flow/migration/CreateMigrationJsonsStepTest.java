/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
