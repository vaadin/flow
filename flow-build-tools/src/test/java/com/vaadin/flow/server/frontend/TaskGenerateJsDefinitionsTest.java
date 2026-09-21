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
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.js.JsCall;
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

    private static final String GREETING_EXPRESSION = "window.alert({ text: $0, kind: 'greeting' })";

    @JsDefinition
    public interface GreeterJs extends Serializable {
        @JsExpression(GREETING_EXPRESSION)
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
                        + JsCall.functionId(GREETING_EXPRESSION, 1)
                        + "\"] = async function ($0) {"),
                "a function should be registered under the identifier of the JavaScript it runs: "
                        + content);
        assertTrue(content.contains(GREETING_EXPRESSION),
                "the declared expression should be the body of the function, as it was written: "
                        + content);
        assertTrue(
                content.contains("window.Vaadin.Flow.jsDefinitions[\""
                        + JsCall.functionId("window.alert('Hello')", 0)
                        + "\"] = async function () {"),
                "the overload that takes no arguments is another function: "
                        + content);
    }

    @Test
    void generatedFile_namesNothingOfTheJava() throws ExecutionFailedException {
        task.execute();
        String content = task.getFileContent();

        assertFalse(content.contains(GreeterJs.class.getName()),
                "a browser that loads the file should not be told what declared the JavaScript: "
                        + content);
        assertFalse(content.contains("showGreeting"),
                "and not what the methods are called either: " + content);
    }

    @Test
    void definitionWithoutDeclaredJavaScript_isNotRegistered()
            throws ExecutionFailedException {
        task.execute();
        String content = task.getFileContent();

        assertEquals(2,
                content.split("window.Vaadin.Flow.jsDefinitions\\[\"",
                        -1).length - 1,
                "only the two methods that declare JavaScript should be registered: "
                        + content);
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
        assertTrue(
                written.contains(
                        JsCall.functionId("this.count = ($0 || 0) + 1", 1)),
                "the interface that was asked for should be in the file: "
                        + written);
        assertTrue(written.contains(JsCall.functionId(GREETING_EXPRESSION, 1)),
                "what the file held should still be in it: " + written);
        assertTrue(
                written.indexOf("import.meta.hot") > written.indexOf(
                        JsCall.functionId("this.count = ($0 || 0) + 1", 1)),
                "and what was added should be part of the module: " + written);
    }

    @Test
    void updateJsDefinitions_fileWrittenByAnotherVersion_keepsWhatItHolds()
            throws IOException {
        // What a file written by another version of this class looks like:
        // functions a browser has, and nothing this one recognizes to write
        // around
        File generated = new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_DEFINITIONS_FILE_NAME);
        generated.getParentFile().mkdirs();
        Files.writeString(generated.toPath(),
                "window.Vaadin.Flow.jsDefinitions = {\n  \"fromsomewhereelse\": async function () {}\n};\n");

        List<Class<?>> missing = TaskGenerateJsDefinitions
                .updateJsDefinitions(options, List.of(CounterJs.class));

        assertTrue(missing.isEmpty());
        String written = Files.readString(generated.toPath());
        assertTrue(written.contains("fromsomewhereelse"),
                "a function a browser has should not be taken out of the file: "
                        + written);
        assertTrue(
                written.contains(
                        JsCall.functionId("this.count = ($0 || 0) + 1", 1)),
                "and the one that was asked for should be in it: " + written);
    }

    @Test
    void updateJsDefinitions_oneMethodEdited_writesOnlyThatFunction()
            throws ExecutionFailedException, IOException {
        // An interface of two methods, of which one declares something else
        // than what the file was written with
        task.execute();
        File generated = new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_DEFINITIONS_FILE_NAME);
        String unchanged = JsCall.functionId("window.alert('Hello')", 0);
        Files.writeString(generated.toPath(),
                Files.readString(generated.toPath()).replace(
                        GREETING_EXPRESSION, "window.alert('what it was')"));

        TaskGenerateJsDefinitions.updateJsDefinitions(options,
                List.of(GreeterJs.class));

        String written = Files.readString(generated.toPath());
        assertEquals(1, countOf(written, unchanged),
                "the method that was not edited should not be written again: "
                        + written);
        assertTrue(written.contains(GREETING_EXPRESSION),
                "and the edited one should be in the file: " + written);
    }

    private static int countOf(String content, String value) {
        return content.split(Pattern.quote(value), -1).length - 1;
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
