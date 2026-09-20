/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.io.Serializable;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;

import static com.vaadin.flow.internal.FrontendUtils.FRONTEND;
import static com.vaadin.flow.internal.FrontendUtils.GENERATED;
import static com.vaadin.flow.internal.FrontendUtils.JS_DEFINITIONS_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TaskGenerateJsDefinitionsTest {

    @JsDefinition
    public interface GreeterJs extends Serializable {
        @JsExpression("window.alert({ text: $0, kind: 'greeting' })")
        void showGreeting(String greeting);

        @JsExpression("window.alert('Hello')")
        void showGreeting();
    }

    @JsDefinition
    public interface CounterJs extends Serializable {
        @JsExpression("this.count = ($0 || 0) + 1")
        void count(Integer from);
    }

    @JsDefinition
    public interface NothingJs extends Serializable {
        void notDeclared();
    }

    @TempDir
    File temporaryFolder;

    private TaskGenerateJsDefinitions task;
    private Options options;
    private File frontendFolder;

    @BeforeEach
    void setUp() {
        frontendFolder = new File(temporaryFolder, FRONTEND);
        frontendFolder.mkdirs();
        options = new Options(Mockito.mock(Lookup.class),
                new DefaultClassFinder(
                        Set.of(GreeterJs.class, NothingJs.class)),
                null).withFrontendDirectory(frontendFolder);
        task = new TaskGenerateJsDefinitions(options);
    }

    @Test
    void generatesAFunctionPerDeclaredExpression()
            throws ExecutionFailedException {
        task.execute();
        String content = task.getFileContent();

        assertTrue(
                content.contains("window.Vaadin.Flow.jsDefinitions[\""
                        + GreeterJs.class.getName() + "\"]"),
                "the definition should be registered under its class name: "
                        + content);
        assertTrue(
                content.contains("\"showGreeting/1\": async function ($0) {"),
                "an overload should be keyed by name and argument count: "
                        + content);
        assertTrue(
                content.contains(
                        "window.alert({ text: $0, kind: 'greeting' })"),
                "the declared expression should be the body of the function, as it was written: "
                        + content);
        assertTrue(content.contains("\"showGreeting/0\": async function () {"),
                "the no-argument overload should be generated too: " + content);
    }

    @Test
    void definitionWithoutDeclaredJavaScript_isNotRegistered()
            throws ExecutionFailedException {
        task.execute();
        String content = task.getFileContent();

        assertFalse(content.contains(NothingJs.class.getName()),
                "an interface that declares no JavaScript has nothing to register: "
                        + content);
    }

    @Test
    void updateJsDefinitions_dropsADefinitionTheFileNamesAndNothingHas()
            throws ExecutionFailedException, IOException {
        task.execute();
        // What a file written by an older state of the application looks like:
        // it names an interface that is no longer there to render
        File generated = new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_DEFINITIONS_FILE_NAME);
        Files.writeString(generated.toPath(),
                Files.readString(generated.toPath()).replace(
                        NothingJs.class.getName(), "com.example.GoneJs"));

        List<Class<?>> missing = TaskGenerateJsDefinitions
                .updateJsDefinitions(options, List.of(GreeterJs.class));

        assertTrue(missing.isEmpty(),
                "the interface that was asked for should be in the file");
        assertFalse(
                Files.readString(generated.toPath())
                        .contains("com.example.GoneJs"),
                "a name the file holds that nothing answers to should be dropped");
    }

    @Test
    void updateJsDefinitions_fileNotWritable_answersWithWhatItDoesNotCarry()
            throws ExecutionFailedException {
        // A file that carries one of the two interfaces, and a folder nothing
        // can be written into
        task.execute();
        File generatedFolder = FrontendUtils
                .getFrontendGeneratedFolder(frontendFolder);
        assumeTrue(generatedFolder.setWritable(false),
                "the folder has to be made read only for this");

        try {
            List<Class<?>> missing = TaskGenerateJsDefinitions
                    .updateJsDefinitions(options,
                            List.of(GreeterJs.class, CounterJs.class));

            assertEquals(List.of(CounterJs.class), missing,
                    "the interface the file carries is not missing because the write failed");
        } finally {
            generatedFolder.setWritable(true);
        }
    }

    @Test
    void findMissingFromGeneratedFile_answersForWhatTheFileCarries()
            throws ExecutionFailedException, IOException {
        task.execute();
        File generated = new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_DEFINITIONS_FILE_NAME);
        String carried = Files.readString(generated.toPath());

        assertTrue(
                TaskGenerateJsDefinitions.findMissingFromGeneratedFile(options,
                        List.of(GreeterJs.class)).isEmpty(),
                "the file was written from this interface");

        // The JavaScript it declared before it was shortened: the browser
        // would keep running the extra statement
        Files.writeString(generated.toPath(),
                carried.replace("window.alert({ text: $0, kind: 'greeting' })",
                        "window.alert($0)"));
        assertEquals(List.of(GreeterJs.class),
                TaskGenerateJsDefinitions.findMissingFromGeneratedFile(options,
                        List.of(GreeterJs.class)),
                "another version of the declarations is not the declarations");

        // What renaming or moving the interface leaves behind: the methods and
        // the JavaScript are there, under the name of before
        Files.writeString(generated.toPath(), carried
                .replace(GreeterJs.class.getName(), "com.example.RenamedJs"));
        assertEquals(List.of(GreeterJs.class),
                TaskGenerateJsDefinitions.findMissingFromGeneratedFile(options,
                        List.of(GreeterJs.class)),
                "a call looks the interface up by name, so the name is part of carrying it");

        Files.delete(generated.toPath());
        assertEquals(List.of(GreeterJs.class),
                TaskGenerateJsDefinitions.findMissingFromGeneratedFile(options,
                        List.of(GreeterJs.class)),
                "no file carries nothing");
    }

    @Test
    void updateJsDefinitions_writesWhatIsAskedForBesideWhatTheFileHolds()
            throws ExecutionFailedException, IOException {
        // A file written before the other interface was annotated: nothing has
        // scanned for it, and what the file holds has to stay in it
        task.execute();
        File generated = new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_DEFINITIONS_FILE_NAME);

        List<Class<?>> missing = TaskGenerateJsDefinitions
                .updateJsDefinitions(options, List.of(CounterJs.class));

        assertTrue(missing.isEmpty());
        String written = Files.readString(generated.toPath());
        assertTrue(written.contains(CounterJs.class.getName()),
                "the interface that was asked for should be in the file: "
                        + written);
        assertTrue(written.contains(GreeterJs.class.getName()),
                "the interface the file held should still be in it: "
                        + written);
    }

    @Test
    void acceptsItsOwnUpdate() throws ExecutionFailedException {
        task.execute();
        String content = task.getFileContent();

        assertTrue(content.contains("import.meta.hot.accept()"),
                "the file should accept its own update, or writing it again while the application runs is ignored by the browser instead of replacing the module: "
                        + content);
    }

    @Test
    void writesTheFileTheBootstrapImports() throws ExecutionFailedException {
        task.execute();

        assertTrue(
                new File(new File(frontendFolder, GENERATED),
                        JS_DEFINITIONS_FILE_NAME).exists(),
                "the generated file should be where the bootstrap imports it from");
    }
}
